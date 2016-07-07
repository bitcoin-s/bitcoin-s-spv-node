package org.bitcoins.spvnode.networking

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
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
  */
trait PeerMessageHandler extends Actor with BitcoinSLogger {

  var unalignedBytes: Seq[Byte] = Nil

  def receive = LoggingReceive {
    case message : Tcp.Message => message match {
      case event : Tcp.Event => handleEvent(event)
      case command : Tcp.Command => handleCommand(command)
    }

    case peerRequest: PeerRequest =>
      val peer = Client(peerRequest.networkParameters, self)(context.system)
      context.become(awaitConnected(peerRequest,peer))
    case msg =>
      logger.error("Unknown message inside of PeerMessageHandler: " + msg)
      throw new IllegalArgumentException("Unknown message inside of PeerMessageHandler: " + msg)
  }


  def awaitConnected(peerRequest: PeerRequest, peer: ActorRef): Receive = LoggingReceive {
    case Tcp.Connected(remote,local) =>
      peer ! VersionMessage(Constants.networkParameters,local.getAddress)
      context.become(awaitVersionMessage(peerRequest,peer))

    case msg =>
      logger.error("Expected a Tcp.Connected message, got: " + msg)
      throw new IllegalArgumentException("Unknown message in awaitConnected: " + msg)
  }


  private def awaitVersionMessage(peerRequest: PeerRequest, peer: ActorRef): Receive = LoggingReceive {
    case Tcp.Received(byteString: ByteString) =>
      logger.debug("Received byte string in awaitVersionMessage: " + BitcoinSUtil.encodeHex(byteString.toArray))
      //this means that we receive a bunch of messages bundled into one [[ByteString]]
      //need to parse out the individual message
      val bytes: Seq[Byte] = unalignedBytes ++ byteString.toArray.toSeq
      val (messages,remainingBytes) = BitcoinSpvNodeUtil.parseIndividualMessages(bytes)
      unalignedBytes = remainingBytes
      for {m <- messages} yield self ! m
    case networkMessage : NetworkMessage => networkMessage.payload match {
      case versionMesage: VersionMessage =>
        peer ! VerAckMessage
        //need to wait for the peer to send back a verack message
        context.become(awaitVerack(peerRequest,peer))
      case msg : NetworkPayload =>
        logger.error("Expected a version message, got: " + msg)
        context.unbecome()
    }
    case msg =>
      logger.error("Unknown message inside of awaitVersionMessage: " + msg)
      throw new IllegalArgumentException("Unknown message inside of awaitVersionMessage: " + msg)

  }

  private def awaitVerack(peerRequest: PeerRequest, peer: ActorRef): Receive = LoggingReceive {
    case Tcp.Received(byteString: ByteString) =>
      logger.debug("Received byte string in awaitVerack: " + BitcoinSUtil.encodeHex(byteString.toArray))
      //this means that we receive a bunch of messages bundled into one [[ByteString]]
      //need to parse out the individual message
      val bytes: Seq[Byte] = unalignedBytes ++ byteString.toArray.toSeq
      val (messages,remainingBytes) = BitcoinSpvNodeUtil.parseIndividualMessages(bytes)
      unalignedBytes = remainingBytes
      for {m <- messages} yield self ! m
    case networkMessage : NetworkMessage => networkMessage.payload match {
      case VerAckMessage =>
        val networkMessage = peerRequest.request
        peer ! networkMessage
        context.become(awaitPeerResponse(peerRequest,peer))
      case msg : NetworkPayload =>
        logger.error("Expected a verack message, got: " + msg)
    }

  }

  def awaitPeerResponse(peerRequest: PeerRequest, peer: ActorRef): Receive = LoggingReceive {
    case Tcp.Received(byteString: ByteString) =>
      logger.debug("Unaligned bytes: "+ BitcoinSUtil.encodeHex(unalignedBytes))
      logger.debug("Received byte string in awaitPeerResponse " + BitcoinSUtil.encodeHex(byteString.toArray))
      //this means that we receive a bunch of messages bundled into one [[ByteString]]
      //need to parse out the individual message
      val bytes: Seq[Byte] = unalignedBytes ++ byteString.toArray.toSeq
      val (messages,remainingBytes) = BitcoinSpvNodeUtil.parseIndividualMessages(bytes)
      unalignedBytes = remainingBytes
      for {m <- messages} yield self ! m
    case networkMessage: NetworkMessage =>
      self ! networkMessage.payload

    case networkResponse: ControlPayload => networkResponse match {
      case pingMsg : PingMessage => peer ! PongMessage(pingMsg.nonce)
      case SendHeadersMessage =>
      case addrMessage: AddrMessage =>
    }

    case networkResponse: DataPayload =>
      peerRequest.listener ! networkResponse
      peer ! Tcp.Abort
    case msg =>
      logger.error("Unknown message in awaitPeerResponse: " + msg)
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
      logger.debug("Tcp connection to: " + remote)
      logger.debug("Local: " + local)
    case Tcp.PeerClosed =>
      context stop self
    case Tcp.ConfirmedClosed | Tcp.Closed | Tcp.Aborted =>
      context.stop(self)
  }

  /**
    * This function is responsible for handling a [[Tcp.Command]] algebraic data type
    * @param command
    */
  private def handleCommand(command : Tcp.Command) = command match {
    case Tcp.ConfirmedClose =>
      //listener ! Tcp.ConfirmedClose
      //peer.get ! Tcp.ConfirmedClose
  }

}




object PeerMessageHandler {
  private case class PeerMessageHandlerImpl(actorSystem: ActorSystem) extends PeerMessageHandler

  def apply(actorSystem : ActorSystem) : ActorRef = actorSystem.actorOf(Props(PeerMessageHandlerImpl(actorSystem)))
}