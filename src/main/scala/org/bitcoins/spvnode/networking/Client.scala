package org.bitcoins.spvnode.networking

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import org.bitcoins.core.util.BitcoinSLogger
import org.bitcoins.spvnode.messages.{NetworkMessage, NetworkRequest, NetworkResponse, VersionMessage}
import org.bitcoins.spvnode.util.NetworkIpAddress

import scala.concurrent.Future
/**
  * Created by chris on 6/6/16.
  */
sealed trait Client extends Actor with BitcoinSLogger {

  def remote: InetSocketAddress

  def listener : ActorRef

  def actorSystem : ActorSystem
  /**
    * The manager is an actor that handles the underlying low level I/O resources (selectors, channels)
    * and instantiates workers for specific tasks, such as listening to incoming connections.
    */
  def manager : ActorRef = IO(Tcp)(actorSystem)

  /**
    * This actor signifies the node we are connected to on the p2p network
    * This is set when we received a [[Tcp.Connected]] message
    */
  private var connection : Option[ActorRef] = None

  def receive = {
    case message : Tcp.Message => message match {
      case event : Tcp.Event => handleEvent(event)
      case command : Tcp.Command => handleCommand(command)
    }
    case networkMessage : NetworkMessage =>
      logger.debug("Recieved network message inside of Client: " + networkMessage)
      handleNetworkMessage(networkMessage)
    case unknownMessage => throw new IllegalArgumentException("Unknown message for client: " + unknownMessage)

/*    case data: ByteString =>
      logger.debug("Sending this data to our peer on the network: " + data)
      connection.get ! Tcp.Write(data)*/

  }
  def sendMessage(msg : NetworkRequest, peer : NetworkIpAddress) : Future[NetworkResponse] = ???

  /**
    * This function is responsible for handling a [[Tcp.Event]] algebraic data type
    * @param event
    */
  private def handleEvent(event : Tcp.Event) = event match {
    case Tcp.CommandFailed(w: Tcp.Write) =>
      logger.debug("Client command failed: " + Tcp.CommandFailed(w))
      logger.debug("O/S buffer was full")
      // O/S buffer was full
      listener ! "write failed"
    case Tcp.Received(data) =>
      logger.debug("Received data from our peer on the network: " + Tcp.Received(data))
      listener ! data
    case Tcp.Connected(remote, local) =>
      logger.debug("Tcp connection to: " + remote)
      logger.debug("Local: " + local)
      listener ! Tcp.Connected(remote,local)
      connection = Some(sender)
      connection.get ! Tcp.Register(self)
    case Tcp.ConfirmedClosed =>
      logger.debug("Client received confirmed closed msg: " + Tcp.ConfirmedClosed)
      listener ! Tcp.ConfirmedClosed
      connection.get ! Tcp.ConfirmedClosed
      connection = None
      context stop self
    case event : NetworkMessage => handleNetworkMessage(event)

  }

  /**
    * Function to handle all of our [[NetworkMessage]] on the p2p network.
    * @param message
    * @return
    */
  private def handleNetworkMessage(message : NetworkMessage) = ???

  /**
    * This function is responsible for handling a [[Tcp.Command]] algebraic data type
    * @param command
    */
  private def handleCommand(command : Tcp.Command) = command match {
    case Tcp.ConfirmedClose =>
      logger.debug("Client received connection closed msg: " + Tcp.ConfirmedClose)
      listener ! Tcp.ConfirmedClose
      connection.get ! Tcp.ConfirmedClose

  }
}


object Client {
  private case class ClientImpl(remote: InetSocketAddress, listener: ActorRef, actorSystem : ActorSystem) extends Client {
    //this eagerly connects the client with our peer on the network as soon
    //as the case class is instantiated
    manager ! Tcp.Connect(remote)
  }

  def apply(remote : InetSocketAddress, listener : ActorRef, actorSystem : ActorSystem) : Props = {
    Props(classOf[ClientImpl], remote,  listener, actorSystem)
  }

  def actor(remote : InetSocketAddress, listener : ActorRef, actorSystem : ActorSystem) : Actor = {
    ClientImpl(remote, listener, actorSystem)
  }

}

