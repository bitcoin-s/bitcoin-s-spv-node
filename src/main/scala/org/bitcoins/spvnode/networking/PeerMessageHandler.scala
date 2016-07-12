package org.bitcoins.spvnode.networking

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, Props}
import akka.event.LoggingReceive
import akka.io.Tcp
import akka.util.ByteString
import org.bitcoins.core.util.{BitcoinSLogger, BitcoinSUtil}
import org.bitcoins.spvnode.NetworkMessage
import org.bitcoins.spvnode.constant.Constants
import org.bitcoins.spvnode.messages._
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

  lazy val peer: ActorRef = context.actorOf(Client.props(Constants.networkParameters,self))
  //var unalignedBytes: Seq[Byte] = Nil

  def receive = LoggingReceive {
    case message : Tcp.Message => handleTcpMessage(message)
    case msg: NetworkMessage =>
      context.become(awaitConnected(Seq(msg)))
    case msg =>
      logger.error("Unknown message inside of PeerMessageHandler: " + msg)
      throw new IllegalArgumentException("Unknown message inside of PeerMessageHandler: " + msg)
  }


  def awaitConnected(peerRequests: Seq[NetworkMessage]): Receive = LoggingReceive {
    case Tcp.Connected(remote,local) =>
      peer ! VersionMessage(Constants.networkParameters,local.getAddress)
      context.become(awaitVersionMessage(peerRequests))

    case msg: NetworkMessage =>
      logger.debug("Received another peer request while waiting for Tcp.Connected: " + msg)
      context.become(awaitConnected(msg +: peerRequests))
    case msg =>
      logger.error("Expected a Tcp.Connected message, got: " + msg)
      throw new IllegalArgumentException("Unknown message in awaitConnected: " + msg)
  }


  private def awaitVersionMessage(peerRequests: Seq[NetworkMessage]): Receive = LoggingReceive {
    case Tcp.Received(byteString: ByteString) =>
      logger.debug("Received byte string in awaitVersionMessage: " + BitcoinSUtil.encodeHex(byteString.toArray))
      //this means that we receive a bunch of messages bundled into one [[ByteString]]
      //need to parse out the individual message
      val bytes: Seq[Byte] = /*unalignedBytes ++ */byteString.toArray.toSeq
      val (messages,remainingBytes) = BitcoinSpvNodeUtil.parseIndividualMessages(bytes)
      //unalignedBytes = remainingBytes
      for {m <- messages} yield self ! m
    case networkMessage : NetworkMessage => networkMessage.payload match {
      case versionMesage: VersionMessage =>
        peer ! VerAckMessage
        //need to wait for the peer to send back a verack message
        logger.debug("Switching to awaitVerack")
        context.become(awaitVerack(peerRequests))
      case msg : NetworkPayload =>
        logger.error("Expected a version message, got: " + msg)
        context.become(awaitVersionMessage(networkMessage +: peerRequests))
    }
    case msg =>
      logger.error("Unknown message inside of awaitVersionMessage: " + msg)
      throw new IllegalArgumentException("Unknown message inside of awaitVersionMessage: " + msg)

  }

  private def awaitVerack(peerRequests: Seq[NetworkMessage]): Receive = LoggingReceive {
    case Tcp.Received(byteString: ByteString) =>
      logger.debug("Received byte string in awaitVerack: " + BitcoinSUtil.encodeHex(byteString.toArray))
      //this means that we receive a bunch of messages bundled into one [[ByteString]]
      //need to parse out the individual message
      val bytes: Seq[Byte] = /*unalignedBytes ++*/ byteString.toArray.toSeq
      val (messages,remainingBytes) = BitcoinSpvNodeUtil.parseIndividualMessages(bytes)
      //unalignedBytes = remainingBytes
      for {m <- messages} yield self ! m

    case networkMessage : NetworkMessage => networkMessage.payload match {
      case VerAckMessage =>
        sendPeerRequests(peerRequests,peer)
        context.become(peerMessageHandler)
      case _ : NetworkPayload =>
        context.become(awaitVerack(networkMessage +: peerRequests))
    }
  }

  /**
    * Sends all of the given [[NetworkMessage]] to our peer on the p2p network
    * Sends a message to the original sender of the peer request
    * confirming that the message was sent to the p2p network
    * @param peerRequests
    * @param peer
    * @return
    */
  private def sendPeerRequests(peerRequests: Seq[NetworkMessage], peer: ActorRef) = for {
    peerRequest <- peerRequests
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
  def peerMessageHandler : Receive = LoggingReceive {
    case Tcp.Received(byteString: ByteString) =>
      //logger.info("Unaligned bytes: "+ BitcoinSUtil.encodeHex(unalignedBytes))
      logger.info("Received byte string in awaitPeerResponse " + BitcoinSUtil.encodeHex(byteString.toArray))
      //this means that we receive a bunch of messages bundled into one [[ByteString]]
      //need to parse out the individual message
      val bytes: Seq[Byte] = /*unalignedBytes ++*/ byteString.toArray.toSeq
      val (messages,remainingBytes) = BitcoinSpvNodeUtil.parseIndividualMessages(bytes)
      //unalignedBytes = remainingBytes
      for {m <- messages} yield self ! m

    case networkMessage: NetworkMessage =>
      self ! networkMessage.payload

    case networkResponse: ControlPayload => networkResponse match {
      case pingMsg : PingMessage =>
        peer ! PongMessage(pingMsg.nonce)
      case SendHeadersMessage =>
      case addrMessage: AddrMessage =>
    }
    case payload: DataPayload => handleDataPayload(payload)
    case msg: Tcp.Message => handleTcpMessage(msg)
    case msg =>
      logger.error("Unknown message in peerMessageHandler: " + msg)
  }

  /**
    * This function is responsible for handling a [[Tcp.Event]] algebraic data type
    * @param event
    */
  private def handleEvent(event : Tcp.Event) = event match {
    case Tcp.CommandFailed(w: Tcp.Write) =>
      logger.debug("Peer message Handler command failed: " + Tcp.CommandFailed(w))
      logger.debug("O/S buffer was full")
      // O/S buffer was full
      //listener ! "write failed"
    case Tcp.CommandFailed(command) =>
      logger.debug("PeerMessageHandler command failed: " + command)
    case Tcp.Received(data) =>
      logger.debug("Received data from our peer on the network: " + Tcp.Received(data))
      //listener ! data
    case Tcp.Connected(remote, local) =>
      logger.debug("Tcp connection to: " + remote + " inside of peer message handler")
      logger.debug("Local: " + local)
    case Tcp.PeerClosed =>
      context.stop(self)
    case closed @ (Tcp.ConfirmedClosed | Tcp.Closed | Tcp.Aborted) =>
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


  private def handleDataPayload(payload: DataPayload) = {
    logger.debug("Forwarding data payload to parent: " + payload)
    logger.debug("COntext.parent: " + context.parent)
    context.parent ! payload
    //context.stop(self)
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
  //def apply(actorSystem : ActorSystem): ActorRef = actorSystem.actorOf(props)
}