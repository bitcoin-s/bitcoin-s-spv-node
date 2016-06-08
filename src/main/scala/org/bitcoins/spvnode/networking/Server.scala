package org.bitcoins.spvnode.networking

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, Props}
import akka.io.{IO, Tcp}
import org.bitcoins.core.util.BitcoinSLogger

/**
  * Created by chris on 6/7/16.
  */
trait Server extends Actor with BitcoinSLogger {
  import context.system

  var boundAddress : Option[InetSocketAddress] = None

  lazy val manager : ActorRef = IO(Tcp)

  def receive = {
    case Tcp.Bound(localAddress) =>
      logger.debug("Server is bound to: " + localAddress.toString)

    case Tcp.CommandFailed(bind : Tcp.Bind) =>
      logger.debug("Failed to bind tcp server: " + bind)
      context stop self
    case Tcp.Connected(remote, local) =>
      logger.debug("Tcp connection between " + local + " and remote " + remote)
      //this is the actor that will receive the data eventually
      val handler = PeerMessageHandler(system)
      val connection = sender
      //Tcp.Register defines the actor that will receive all data
      //associated with the socket
      connection ! Tcp.Register(handler)
    case bindMsg : Tcp.Bind => bind(bindMsg)
    case Tcp.Unbind =>
      logger.debug("Unbinding " + boundAddress)
      manager ! Tcp.Unbind
      context stop self
    case msg => throw new RuntimeException("Unknown message: " + msg)
  }


  /**
    * Responsible for sending a bind message to the IO manager
    * @param bindMsg
    */
  def bind(bindMsg : Tcp.Bind) : Unit = {
    logger.debug("Binding to: " + bindMsg.localAddress)
    boundAddress = Some(bindMsg.localAddress)
    manager ! Tcp.Bind(self,bindMsg.localAddress)
  }

}

object Server extends BitcoinSActorSystem {

  private case class ServerImpl() extends Server
  def apply() : ActorRef = actorSystem.actorOf(Props(ServerImpl()))

}