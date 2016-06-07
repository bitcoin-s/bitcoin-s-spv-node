package org.bitcoins.spvnode.networking

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import org.bitcoins.spvnode.messages.{NetworkRequest, NetworkResponse}
import org.bitcoins.spvnode.util.NetworkIpAddress

import scala.concurrent.Future
/**
  * Created by chris on 6/6/16.
  */
sealed trait Client extends Actor {

  import BitcoinSActorSystem._

  def listener : ActorRef

  def remote : InetSocketAddress

  /**
    * The manager is an actor that handles the underlying low level I/O resources (selectors, channels)
    * and instantiates workers for specific tasks, such as listening to incoming connections.
    */
  def manager = IO(Tcp)


  def receive  = {
    case Tcp.CommandFailed(_: Tcp.Connect) =>
      listener ! "connect failed"
      context stop self

    case c @ Tcp.Connected(remote, local) =>
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



object Client extends BitcoinSActorSystem {
  private case class ClientImpl(remote: InetSocketAddress, listener: ActorRef) extends Client {
    manager ! Tcp.Connect(remote)
  }

  def apply(remote: InetSocketAddress, listener : ActorRef) : ActorRef = {
    actorSystem.actorOf(Props(ClientImpl(remote,listener)))
  }
}
