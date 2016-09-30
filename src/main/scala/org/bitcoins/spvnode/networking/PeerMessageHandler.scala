package org.bitcoins.spvnode.networking

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, ActorRefFactory, PoisonPill, Props}
import akka.event.LoggingReceive
import akka.io.Tcp
import akka.util.ByteString
import org.bitcoins.core.util.{BitcoinSLogger, BitcoinSUtil}
import org.bitcoins.spvnode.NetworkMessage
import org.bitcoins.spvnode.constant.Constants
import org.bitcoins.spvnode.messages.control.{PongMessage, VersionMessage}
import org.bitcoins.spvnode.messages.{GetAddrMessage, VerAckMessage, _}
import org.bitcoins.spvnode.util.BitcoinSpvNodeUtil

/**
  * Created by chris on 6/7/16.
  * This actor is the middle man between our [[Client]] and higher level actors such as
  * [[BlockActor]]. When it receives a message, it tells [[Client]] to create connection to a peer,
  * then it exchanges [[VersionMessage]], [[VerAckMessage]] and [[PingMessage]]/[[PongMessage]] message
  * with our peer on the network. When the Client finally responds to the [[NetworkMessage]] we originally
  * sent it sends that [[NetworkMessage]] back to the actor that requested it.
  */
sealed trait PeerMessageHandler extends Actor with BitcoinSLogger {

  lazy val peer: ActorRef = context.actorOf(Client.props,BitcoinSpvNodeUtil.createActorName(this.getClass))

  def receive = LoggingReceive {
    case connectedMsg: Tcp.Connected =>
      //for the case where the first message we receive is a Tcp.Connected message
      context.become(awaitConnected(Nil,Nil))
      self.forward(connectedMsg)
    case message : Tcp.Message =>
      val remainingBytes = handleTcpMessage(message, Nil, sender)
      context.become(awaitConnected(Nil,remainingBytes))
    case msg: NetworkMessage =>
      logger.info("Switching to awaitConnected from default receive")
      context.become(awaitConnected(Seq((sender,msg)), Nil))
    case networkPayload: NetworkPayload =>
      self.forward(NetworkMessage(Constants.networkParameters,networkPayload))
  }

  /** Waits for us to receive a [[Tcp.Connected]] message from our [[Client]] */
  def awaitConnected(requests: Seq[(ActorRef,NetworkMessage)], unalignedBytes: Seq[Byte]): Receive = LoggingReceive {
    case Tcp.Connected(remote,local) =>
      val versionMsg = VersionMessage(Constants.networkParameters,local.getAddress)
      peer ! versionMsg
      logger.info("Switching to awaitVersionMessage from awaitConnected")
      context.become(awaitVersionMessage(requests, unalignedBytes))
    case msg: Tcp.Message =>
      val newUnalignedBytes = handleTcpMessage(msg, unalignedBytes,sender)
      context.become(awaitConnected(requests, newUnalignedBytes))
    case msg: NetworkMessage =>
      logger.debug("Received another peer request while waiting for Tcp.Connected: " + msg)
      context.become(awaitConnected((sender,msg) +: requests, unalignedBytes))
    case payload: NetworkPayload =>
      self ! NetworkMessage(Constants.networkParameters,payload)
  }


  /** Waits for a peer on the network to send us a [[VersionMessage]] */
  private def awaitVersionMessage(requests: Seq[(ActorRef,NetworkMessage)], unalignedBytes: Seq[Byte]): Receive = LoggingReceive {
    case networkMessage : NetworkMessage => networkMessage.payload match {
      case _ : VersionMessage =>
        peer ! VerAckMessage
        //need to wait for the peer to send back a verack message
        logger.debug("Switching to awaitVerack from awaitVersionMessage")
        context.become(awaitVerack(requests, unalignedBytes))
      case msg : NetworkPayload =>
        context.become(awaitVersionMessage((sender,networkMessage) +: requests, unalignedBytes))
    }
    case msg: Tcp.Message =>
      val newUnalignedBytes = handleTcpMessage(msg, unalignedBytes,sender)
      context.become(awaitVersionMessage(requests,newUnalignedBytes))
    case payload: NetworkPayload =>
      self ! NetworkMessage(Constants.networkParameters,payload)
  }

  /** Waits for our peer on the network to send us a [[VerAckMessage]] */
  private def awaitVerack(requests: Seq[(ActorRef,NetworkMessage)], unalignedBytes: Seq[Byte]): Receive = LoggingReceive {
    case networkMessage : NetworkMessage => networkMessage.payload match {
      case VerAckMessage =>
        logger.info("Received verack message, sending queued messages: " + requests)
        sendPeerRequests(requests)
        logger.info("Switching to peerMessageHandler from awaitVerack")
        val controlPayloads = findControlPayloads(requests)
        context.become(peerMessageHandler(controlPayloads, unalignedBytes))
      case _ : NetworkPayload =>
        context.become(awaitVerack((sender,networkMessage) +: requests, unalignedBytes))
    }
    case msg: Tcp.Message =>
      val newUnalignedBytes = handleTcpMessage(msg,unalignedBytes,sender)
      context.become(awaitVerack(requests,newUnalignedBytes))
    case payload: NetworkPayload =>
      self ! NetworkMessage(Constants.networkParameters,payload)
  }

  /**
    * Sends all of the given [[NetworkMessage]] to our peer on the p2p network
    * @param requests
    * @return
    */
  private def sendPeerRequests(requests: Seq[(ActorRef,NetworkMessage)]) = for {
    (sender,peerRequest) <- requests
  } yield peer ! peerRequest

  /**
    * This is the main receive function inside of [[PeerMessageHandler]]
    * This will receive peer requests, then send the payload to the the corresponding
    * actor responsible for handling that specific message
    * @return
    */
  def peerMessageHandler(controlMessages: Seq[(ActorRef,ControlPayload)], unalignedBytes: Seq[Byte]) : Receive = LoggingReceive {
    case networkMessage: NetworkMessage =>
      //hack to get around using 'self' as the sender
      self.tell(networkMessage.payload,sender)
    case controlPayload: ControlPayload =>
      val newControlMsgs = handleControlPayload(controlPayload,sender,controlMessages)
      context.become(peerMessageHandler(newControlMsgs,unalignedBytes))
    case dataPayload: DataPayload => handleDataPayload(dataPayload,sender)
    case msg: Tcp.Message =>
      val newUnalignedBytes = handleTcpMessage(msg,unalignedBytes,sender)
      context.become(peerMessageHandler(controlMessages,newUnalignedBytes))
  }

  /**
    * This function is responsible for handling a [[Tcp.Event]] algebraic data type
    * @param event the event that needs to be handled
    * @param unalignedBytes the unaligned bytes from previous tcp frames
    *                       These can be used to construct a full message, since the last frame could
    *                       have transmitted the first half of the message, and this frame transmits the
    *                       rest of the message
    * @return the new unaligned bytes, if there are any
    */
  private def handleEvent(event : Tcp.Event, unalignedBytes: Seq[Byte], sender: ActorRef): Seq[Byte] = event match {
    case Tcp.Received(byteString: ByteString) =>
      //logger.debug("Received byte string in peerMessageHandler " + BitcoinSUtil.encodeHex(byteString.toArray))
      //logger.debug("Unaligned bytes: " + BitcoinSUtil.encodeHex(unalignedBytes))
      //this means that we receive a bunch of messages bundled into one [[ByteString]]
      //need to parse out the individual message
      val bytes: Seq[Byte] = unalignedBytes ++ byteString.toArray.toSeq
      //logger.debug("Bytes for message parsing: " + BitcoinSUtil.encodeHex(bytes))
      val (messages,remainingBytes) = BitcoinSpvNodeUtil.parseIndividualMessages(bytes)
      for {m <- messages} yield self.tell(m,sender)
      remainingBytes
    case x @ (_ : Tcp.CommandFailed | _ : Tcp.Received | _ : Tcp.Connected) =>
      logger.debug("Received TCP event the peer message handler actor should not have received: " + x)
      unalignedBytes
    case Tcp.PeerClosed =>
      self ! PoisonPill
      unalignedBytes
    case closed @ (Tcp.ConfirmedClosed | Tcp.Closed | Tcp.Aborted | Tcp.PeerClosed) =>
      context.parent ! closed
      self ! PoisonPill
      unalignedBytes
  }

  /**
    * This function is responsible for handling a [[Tcp.Command]] algebraic data type
    * @param command
    */
  private def handleCommand(command : Tcp.Command) = command match {
    case close @ (Tcp.ConfirmedClose | Tcp.Close | Tcp.Abort) =>
      peer ! close
  }

  private def handleTcpMessage(message: Tcp.Message, unalignedBytes: Seq[Byte], sender: ActorRef): Seq[Byte] = message match {
    case event: Tcp.Event => handleEvent(event, unalignedBytes,sender)
    case command: Tcp.Command =>
      handleCommand(command)
      Nil
  }

  /**
    * Handles a [[DataPayload]] message. It checks if the sender is the parent
    * actor, it sends it to our peer on the network. If the sender was the
    * peer on the network, forward to the actor that spawned our actor
    * @param payload
    * @param sender
    */
  private def handleDataPayload(payload: DataPayload, sender: ActorRef): Unit = {
    val destination = deriveDestination(sender)
    destination ! payload
  }

  /**
    * Handles control payloads defined here https://bitcoin.org/en/developer-reference#control-messages
    * @param payload the payload we need to do something with
    * @param requests the @payload may be a response to a request inside this sequence
    * @return the requests with the request removed for which the @payload is responding too
    */
  private def handleControlPayload(payload: ControlPayload, sender: ActorRef,
                                   requests: Seq[(ActorRef,ControlPayload)]): Seq[(ActorRef,ControlPayload)] = {
    logger.debug("Control payload before derive: " + payload)
    val destination = deriveDestination(sender)
    payload match {
      case pingMsg: PingMessage =>
        if (destination == context.parent) {
          //means that our peer sent us a ping message, we respond with a pong
          peer ! PongMessage(pingMsg.nonce)
          //remove ping message from requests
          requests.filterNot { case (sender,msg) => msg.isInstanceOf[PingMessage] }
        } else {
          //means we initialized the ping message, send it to our peer
          logger.debug("Sending ping message to peer: " + pingMsg)
          peer ! pingMsg
          requests
        }
      case SendHeadersMessage =>
        requests
      case addrMessage: AddrMessage =>
        //figure out if this was a solicited AddrMessage or an Unsolicited AddrMessage
        //see https://bitcoin.org/en/developer-reference#addr
        val getAddrMessage: Option[(ActorRef,ControlPayload)] = requests.find{ case (sender,msg) => msg == GetAddrMessage}
        if (getAddrMessage.isDefined) {
          destination ! addrMessage
          //remove the GetAddrMessage request
          requests.filterNot{ case (sender,msg) => msg == GetAddrMessage }
        } else requests
      case filterMsg @ (_: FilterAddMessage | _: FilterLoadMessage | FilterClearMessage) =>
        logger.debug("Sending filter message: " + filterMsg + " to " + "destination")
        destination ! filterMsg
        requests
      case controlMsg @ (GetAddrMessage | VerAckMessage | _ : VersionMessage | _ : PongMessage) =>
        destination ! controlMsg
        requests
      case rejectMsg: RejectMessage =>
        logger.error("Received a reject message from the p2p network: " + rejectMsg)
        requests
    }
  }


  /**
    * Figures out the actor that is the destination for a message
    * For messages, if the sender was context.parent, we need to send the message to our peer on the network
    * if the sender was the peer, we need to relay the message to the context.parent
    * @param sender
    * @return
    */
  private def deriveDestination(sender: ActorRef): ActorRef = {
    if (sender == context.parent) {
      logger.debug("The sender was context.parent, therefore we are sending this message to our peer on the network")
      peer
    } else {
      logger.debug("The sender was the peer on the network, therefore we need to send to context.parent")
      context.parent
    }
  }

  /**
    * Finds all control payloads inside of a given sequence of requests
    * @param requests
    * @return
    */
  private def findControlPayloads(requests: Seq[(ActorRef,NetworkMessage)]): Seq[(ActorRef,ControlPayload)] = {
    val controlPayloads = requests.filter { case (sender,msg) => msg.payload.isInstanceOf[ControlPayload] }
    controlPayloads.map { case (sender, msg) => (sender, msg.payload.asInstanceOf[ControlPayload]) }
  }
}



object PeerMessageHandler {
  private case class PeerMessageHandlerImpl(seed: InetSocketAddress) extends PeerMessageHandler {
    peer ! Tcp.Connect(seed)
  }

  def props: Props = {
    val seed = new InetSocketAddress(Constants.networkParameters.dnsSeeds(2), Constants.networkParameters.port)
    props(seed)
  }

  def props(seed: InetSocketAddress): Props = Props(classOf[PeerMessageHandlerImpl],seed)

  def apply(context : ActorRefFactory): ActorRef = context.actorOf(props, BitcoinSpvNodeUtil.createActorName(this.getClass))

  def apply(context: ActorRefFactory, seed: InetSocketAddress): ActorRef = {
    context.actorOf(props(seed), BitcoinSpvNodeUtil.createActorName(this.getClass))
  }
}

