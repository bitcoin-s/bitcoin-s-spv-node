package org.bitcoins.spvnode.networking

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.io.Tcp
import org.bitcoins.core.util.BitcoinSLogger

/**
  * Created by chris on 6/7/16.
  */
trait PeerMessageHandler extends Actor with BitcoinSLogger {
  def receive = {
    case Tcp.Received(data) =>
      logger.debug("Received data: " + data)
      //sender ! Tcp.Write(data)
    case Tcp.PeerClosed     => context stop self
  }
}




object PeerMessageHandler {
  private case class PeerMessageHandlerImpl() extends PeerMessageHandler

  def apply(actorSystem : ActorSystem) : ActorRef = actorSystem.actorOf(Props(PeerMessageHandlerImpl()))
}