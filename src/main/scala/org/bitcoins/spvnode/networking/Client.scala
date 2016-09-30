package org.bitcoins.spvnode.networking

import akka.actor.{Actor, ActorContext, ActorRef, Props}
import akka.event.LoggingReceive
import akka.io.{IO, Tcp}
import org.bitcoins.core.config.NetworkParameters
import org.bitcoins.core.util.BitcoinSLogger
import org.bitcoins.spvnode.NetworkMessage
import org.bitcoins.spvnode.constant.Constants
import org.bitcoins.spvnode.messages._
import org.bitcoins.spvnode.util.BitcoinSpvNodeUtil
/**
  * Created by chris on 6/6/16.
  * This actor is responsible for creating a connection,
  * relaying messages and closing a connection to our peer on
  * the p2p network
  */
sealed trait Client extends Actor with BitcoinSLogger {

  /**
    * The manager is an actor that handles the underlying low level I/O resources (selectors, channels)
    * and instantiates workers for specific tasks, such as listening to incoming connections.
    */
  def manager : ActorRef = IO(Tcp)(context.system)

  /**
    * The parameters for the network we are connected to
    * i.e. [[org.bitcoins.core.config.MainNet]] or [[org.bitcoins.core.config.TestNet3]]
    * @return
    */
  def network : NetworkParameters = Constants.networkParameters

  /**
    * This actor signifies the node we are connected to on the p2p network
    * This is the context we are in after we received a [[Tcp.Connected]] message
    */
  private def awaitNetworkRequest(peer: ActorRef): Receive = LoggingReceive {
    case message: NetworkMessage => sendNetworkMessage(message,peer)
    case payload: NetworkPayload =>
      val networkMsg = NetworkMessage(network,payload)
      self.forward(networkMsg)
    case message: Tcp.Message =>
      handleTcpMessage(message,Some(peer))
  }

  /** This context is responpsible for initialiazing a tcp connection with a peer on the bitcoin p2p network */
  def receive = LoggingReceive {
    case message : Tcp.Message => handleTcpMessage(message,None)
  }

  /**
    * Handles boiler plate [[Tcp.Message]] types
    * @param message
    * @return
    */
  private def handleTcpMessage(message: Tcp.Message, peer: Option[ActorRef]) = message match {
    case event: Tcp.Event => handleEvent(event)
    case command: Tcp.Command => handleCommand(command,peer)
  }

  /**
    * This function is responsible for handling a [[Tcp.Event]] algebraic data type
    * @param event
    */
  private def handleEvent(event : Tcp.Event) = event match {
    case Tcp.Bound(localAddress) =>
      logger.debug("Actor is now bound to the local address: " + localAddress)
      context.parent ! Tcp.Bound(localAddress)
    case Tcp.CommandFailed(command) =>
      logger.debug("Client Command failed:" + command)
    case Tcp.Connected(remote, local) =>
      logger.debug("Tcp connection to: " + remote)
      logger.debug("Local: " + local)
      sender ! Tcp.Register(context.parent)
      context.parent ! Tcp.Connected(remote,local)
      context.become(awaitNetworkRequest(sender))
    case closeCmd @ (Tcp.ConfirmedClosed | Tcp.Closed | Tcp.Aborted | Tcp.PeerClosed) =>
      logger.debug("Closed command received: " + closeCmd)
      context.parent ! closeCmd
      context.stop(self)
  }


  /**
    * This function is responsible for handling a [[Tcp.Command]] algebraic data type
    * @param command
    */
  private def handleCommand(command : Tcp.Command, peer: Option[ActorRef]) = command match {
    case closeCmd @ (Tcp.ConfirmedClose | Tcp.Close | Tcp.Abort) =>
      peer.map(p => p ! closeCmd)
    case connectCmd : Tcp.Connect =>
      manager ! connectCmd
    case bind: Tcp.Bind =>
      manager ! bind
  }

  /**
    * Sends a network request to our peer on the network
    * @param message
    * @return
    */
  private def sendNetworkMessage(message : NetworkMessage, peer: ActorRef) = {
    val byteMessage = BitcoinSpvNodeUtil.buildByteString(message.bytes)
    logger.debug("Network message: " + message)
    peer ! Tcp.Write(byteMessage)
  }

}




object Client {
  private case class ClientImpl() extends Client

  def props: Props = Props(classOf[ClientImpl])

  def apply(context: ActorContext): ActorRef = context.actorOf(props,BitcoinSpvNodeUtil.createActorName(this.getClass))
}

