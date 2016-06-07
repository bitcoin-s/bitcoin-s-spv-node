package org.bitcoins.spvnode.networking

import akka.actor.Actor
import akka.io.Tcp

/**
  * Created by chris on 6/7/16.
  */
trait PeerMessageHandler extends Actor {
  def receive = {
    case Tcp.Received(data) => sender ! Tcp.Write(data)
    case Tcp.PeerClosed     => context stop self
  }
}
