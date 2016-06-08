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
  *
  * @param listener
  * @param actorSystem
  * @param manager The manager is an actor that handles the underlying low level I/O resources (selectors, channels)
  *                and instantiates workers for specific tasks, such as listening to incoming connections.
  */
class Client(listener : ActorRef, actorSystem: ActorSystem, manager : ActorRef) extends Actor with BitcoinSLogger {


  def receive  = {

    case Tcp.Connect(remote,_,_,_,_) => manager ! Tcp.Connect(remote)
    case Tcp.CommandFailed(_: Tcp.Connect) =>
      logger.debug("Connection failed")
      listener ! "connect failed"
      context stop self

    case c @ Tcp.Connected(remote, local) =>
      logger.debug("Tcp connection to: " + remote)
      logger.debug("Local: " + local)
      listener ! c
      val connection = sender()
      connection ! Tcp.Register(self)
      context become {
        case data: ByteString =>
          connection ! Tcp.Write(data)
        case Tcp.CommandFailed(w: Tcp.Write) =>
          // O/S buffer was full
          listener ! "write failed"
        case Tcp.Received(data) =>
          listener ! data
        case "close" =>
          connection ! Tcp.Close
        case _: Tcp.ConnectionClosed =>
          listener ! "connection closed"
          context stop self
      }
  }
  def sendMessage(msg : NetworkRequest, peer : NetworkIpAddress) : Future[NetworkResponse] = ???

}


object Client {
  //private case class ClientImpl(remote: InetSocketAddress, listener: ActorRef, actorSystem : ActorSystem) extends Client

  def apply(listener : ActorRef, actorSystem : ActorSystem) : Props = {
    Props(classOf[Client], listener, actorSystem, IO(Tcp)(actorSystem))
  }

  def actor(listener : ActorRef, actorSystem : ActorSystem) : Actor = new Client(listener, actorSystem, IO(Tcp)(actorSystem))

}

