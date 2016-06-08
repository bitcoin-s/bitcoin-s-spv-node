package org.bitcoins.spvnode.networking

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import org.bitcoins.core.util.BitcoinSLogger
import org.bitcoins.spvnode.messages.{NetworkRequest, NetworkResponse}
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

  def receive = {
    case Tcp.CommandFailed(x) =>
      logger.debug("Client command failed: " + Tcp.CommandFailed(x))
      listener ! Tcp.CommandFailed(x)
      context stop self
    case c @ Tcp.Connected(remote, local) =>
      logger.debug("Tcp connection to: " + remote)
      logger.debug("Local: " + local)
      listener ! c
      val connection = sender()
      connection ! Tcp.Register(self)
      context become {
        case data: ByteString =>
          logger.debug("Sending this data to our peer on the network: " + data)
          connection ! Tcp.Write(data)
        case Tcp.CommandFailed(w: Tcp.Write) =>
          logger.debug("Client command failed: " + Tcp.CommandFailed(w))
          logger.debug("O/S buffer was full")
          // O/S buffer was full
          listener ! "write failed"
        case Tcp.Received(data) =>
          logger.debug("Received data from our peer on the network: " + Tcp.Received(data))
          listener ! data
        case "close" =>
          connection ! Tcp.Close
        case x : Tcp.ConnectionClosed =>
          listener ! x
          context stop self
      }
  }
  def sendMessage(msg : NetworkRequest, peer : NetworkIpAddress) : Future[NetworkResponse] = ???

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

