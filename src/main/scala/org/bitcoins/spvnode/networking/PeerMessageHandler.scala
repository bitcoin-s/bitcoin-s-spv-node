package org.bitcoins.spvnode.networking

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorContext, ActorRef, Props}
import akka.event.LoggingReceive
import akka.io.Tcp
import akka.util.ByteString
import org.bitcoins.core.util.{BitcoinSLogger, BitcoinSUtil}
import org.bitcoins.spvnode.NetworkMessage
import org.bitcoins.spvnode.constant.Constants
import org.bitcoins.spvnode.messages.{GetAddrMessage, _}
import org.bitcoins.spvnode.messages.control.{PongMessage, VersionMessage}
import org.bitcoins.spvnode.util.BitcoinSpvNodeUtil

/**
  * Created by chris on 6/7/16.
  * This actor is the middle man between our [[Client]] and higher level actors such as
  * [[BlockActor]]. When it receives a message, it tells [[Client]] to create connectino to a peer,
  * then it exchanges [[VersionMessage]], [[VerAckMessage]] and [[PingMessage]]/[[PongMessage]] message
  * with our peer on the network. When the Client finally responds to the [[NetworkMessage]] we originally
  * sent it sends that [[NetworkMessage]] back to the actor that requested it.
  */
sealed trait PeerMessageHandler extends Actor with BitcoinSLogger {

  lazy val peer: ActorRef = Client(context)
  var unalignedBytes: Seq[Byte] = Nil

  def receive = LoggingReceive {
    case message : Tcp.Message => handleTcpMessage(message)
    case msg: NetworkMessage =>
      logger.info("Switching to awaitConnected from default receive")
      context.become(awaitConnected(Seq(msg)))
    case msg =>
      logger.error("Unknown message inside of PeerMessageHandler: " + msg)
      throw new IllegalArgumentException("Unknown message inside of PeerMessageHandler: " + msg)
  }


  def awaitConnected(requests: Seq[NetworkMessage]): Receive = LoggingReceive {
    case Tcp.Connected(remote,local) =>
      peer ! VersionMessage(Constants.networkParameters,local.getAddress)
      logger.info("Switching to awaitVersionMessage from awaitConnected")
      context.become(awaitVersionMessage(requests))

    case msg: NetworkMessage =>
      logger.debug("Received another peer request while waiting for Tcp.Connected: " + msg)
      context.become(awaitConnected(msg +: requests))
    case msg =>
      logger.error("Expected a Tcp.Connected message, got: " + msg)
      throw new IllegalArgumentException("Unknown message in awaitConnected: " + msg)
  }


  private def awaitVersionMessage(requests: Seq[NetworkMessage]): Receive = LoggingReceive {
    case networkMessage : NetworkMessage => networkMessage.payload match {
      case versionMesage: VersionMessage =>
        peer ! VerAckMessage
        //need to wait for the peer to send back a verack message
        logger.debug("Switching to awaitVerack from awaitVersionMessage")
        context.become(awaitVerack(requests))
      case msg : NetworkPayload =>
        logger.error("Expected a version message, got: " + msg)
        context.become(awaitVersionMessage(networkMessage +: requests))
    }
    case msg: Tcp.Message => handleTcpMessage(msg)
    case msg =>
      logger.error("Unknown message inside of awaitVersionMessage: " + msg)
      throw new IllegalArgumentException("Unknown message inside of awaitVersionMessage: " + msg)
  }

  private def awaitVerack(requests: Seq[NetworkMessage]): Receive = LoggingReceive {
    case networkMessage : NetworkMessage => networkMessage.payload match {
      case VerAckMessage =>
        logger.info("Received verack message, sending queued messages: " + requests)
        sendPeerRequests(requests,peer)
        logger.info("Switching to peerMessageHandler from awaitVerack")
        val controlPayloads = requests.filter(_.payload.isInstanceOf[ControlPayload]).map(_.payload.asInstanceOf[ControlPayload])
        context.become(peerMessageHandler(controlPayloads))
      case _ : NetworkPayload =>
        context.become(awaitVerack(networkMessage +: requests))
    }
    case msg: Tcp.Message => handleTcpMessage(msg)
    case msg =>
      logger.error("Unknown message inside of awaitVerack: " + msg)
      throw new IllegalArgumentException("Unknown message inside of awaitVerack: " + msg)
  }

  /**
    * Sends all of the given [[NetworkMessage]] to our peer on the p2p network
    * Sends a message to the original sender of the peer request
    * confirming that the message was sent to the p2p network
    * @param requests
    * @param peer
    * @return
    */
  private def sendPeerRequests(requests: Seq[NetworkMessage], peer: ActorRef) = for {
    peerRequest <- requests
  } yield sendPeerRequest(peerRequest,peer)

  /**
    * Sends the given [[NetworkMessage]] to our peer on the bitcoin p2p network
    * Sends a message to the original sender of the peer request
    * confirming that the message was sent to the p2p network
    * @param msg
    * @param peer
    * @return
    */
  private def sendPeerRequest(msg: NetworkMessage, peer: ActorRef) = peer ! msg


  /**
    * This is the main receive function inside of [[PeerMessageHandler]]
    * This will receive peer requests, then send the payload to the the corresponding
    * actor responsible for handling that specific message
    * @return
    */
  def peerMessageHandler(controlMessages: Seq[ControlPayload]) : Receive = LoggingReceive {
    case networkMessage: NetworkMessage =>
      self ! networkMessage.payload
    case controlPayload: ControlPayload =>
      val newControlMsgs = handleControlPayload(controlPayload,peer,sender,controlMessages)
      context.become(peerMessageHandler(newControlMsgs))
    case dataPayload: DataPayload => handleDataPayload(dataPayload,peer,sender)
    case msg: Tcp.Message => handleTcpMessage(msg)
    case msg =>
      logger.error("Unknown message in peerMessageHandler: " + msg)
  }

  /**
    * This function is responsible for handling a [[Tcp.Event]] algebraic data type
    * @param event
    */
  private def handleEvent(event : Tcp.Event) = event match {
    case Tcp.Received(byteString: ByteString) =>
      logger.info("Received byte string in peerMessageHandler " + BitcoinSUtil.encodeHex(byteString.toArray))
      //this means that we receive a bunch of messages bundled into one [[ByteString]]
      //need to parse out the individual message
      val bytes: Seq[Byte] = unalignedBytes ++ byteString.toArray.toSeq
      val (messages,remainingBytes) = BitcoinSpvNodeUtil.parseIndividualMessages(bytes)
      unalignedBytes = remainingBytes
      for {m <- messages} yield self ! m
    case Tcp.CommandFailed(w: Tcp.Write) =>
      logger.debug("Peer message Handler command failed: " + Tcp.CommandFailed(w))
    case Tcp.CommandFailed(command) =>
      logger.debug("PeerMessageHandler command failed: " + command)
    case Tcp.Received(data) =>
      logger.debug("Received data from our peer on the network: " + Tcp.Received(data))
      //listener ! data
    case Tcp.Connected(remote, local) =>
    case Tcp.PeerClosed =>
      context.stop(self)
    case closed @ (Tcp.ConfirmedClosed | Tcp.Closed | Tcp.Aborted | Tcp.PeerClosed) =>
      context.parent ! closed
      context.stop(self)
  }

  /**
    * This function is responsible for handling a [[Tcp.Command]] algebraic data type
    * @param command
    */
  private def handleCommand(command : Tcp.Command) = command match {
    case close @ (Tcp.ConfirmedClose | Tcp.Close | Tcp.Abort) =>
      peer ! close
  }

  private def handleTcpMessage(message: Tcp.Message) = message match {
    case event: Tcp.Event => handleEvent(event)
    case command: Tcp.Command => handleCommand(command)
  }

  /**
    * Handles a [[DataPayload]] message. It checks if the sender is the parent
    * actor, it sends it to our peer on the network. If the sender was the
    * peer on the network, forward to the actor that spawned our actor
    * @param payload
    * @param peer
    * @param sender
    */
  private def handleDataPayload(payload: DataPayload, peer : ActorRef, sender: ActorRef): Unit = {
    val destination = deriveDestination(peer,sender)
    handleDataPayload(payload,destination)
  }

  /**
    * Sends a [[DataPayload]] to the destination actor
    * @param payload
    * @param destination
    */
  private def handleDataPayload(payload: DataPayload, destination: ActorRef): Unit = destination ! payload


  /**
    * Handles a control payload.
    * @param payload
    * @param peer
    * @param sender
    * @param requests
    * @return
    */
  private def handleControlPayload(payload: ControlPayload, peer: ActorRef, sender: ActorRef,
                                   requests: Seq[ControlPayload]): Seq[ControlPayload] = {
    val destination = deriveDestination(peer,sender)
    handleControlPayload(payload,destination,requests)
  }

  private def handleControlPayload(payload: ControlPayload, destination: ActorRef,
                                   requests: Seq[ControlPayload]): Seq[ControlPayload] = payload match {
    case pingMsg : PingMessage =>
      if (destination == context.parent) {
        //means that our peer sent us a ping message, we respond with a pong
        peer ! PongMessage(pingMsg.nonce)
        requests.filterNot(_.isInstanceOf[PingMessage])
      } else {
        //means we initialized the ping message, send it to our peer
        peer ! pingMsg
        requests
      }
    case SendHeadersMessage =>
      requests
    case GetAddrMessage =>
      destination ! GetAddrMessage
      requests
    case addrMessage: AddrMessage =>
      //figure out if this was a solicited AddrMessage or an Unsolicited AddrMessage
      //see https://bitcoin.org/en/developer-reference#addr
      val getAddrMessage: Option[ControlPayload] = requests.find(_ == GetAddrMessage)
      if (getAddrMessage.isDefined) {
        destination ! addrMessage
        logger.debug("We requested this addrMessage")
        logger.info("Sending to context.parent: " + (destination == context.parent))

        //remove the GetAddrMessage request
        requests.filterNot(_ == GetAddrMessage)
      } else requests
  }


  /**
    * Figures out the actor that is the destination for a message
    * For messages, if the sender was context.parent, we need to send the message to our peer on the network
    * if the sender was the peer, we need to relay the message to the context.parent
    * @param peer
    * @param sender
    * @return
    */
  private def deriveDestination(peer: ActorRef, sender: ActorRef): ActorRef = {
    if (sender == context.parent) peer
    else context.parent
  }
}



object PeerMessageHandler {
  private case class PeerMessageHandlerImpl(seed: InetSocketAddress) extends PeerMessageHandler {
    peer ! Tcp.Connect(seed)
  }

  def props: Props = {
    val seed = new InetSocketAddress(Constants.networkParameters.dnsSeeds(0), Constants.networkParameters.port)
    props(seed)
  }

  def props(seed: InetSocketAddress): Props = Props(classOf[PeerMessageHandlerImpl],seed)

  def apply(context : ActorContext): ActorRef = context.actorOf(props, BitcoinSpvNodeUtil.createActorName(this.getClass))

  def apply(context: ActorContext, seed: InetSocketAddress) = {
    context.actorOf(props(seed), BitcoinSpvNodeUtil.createActorName(this.getClass))
  }
}