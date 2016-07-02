package org.bitcoins.spvnode.networking

import akka.actor.{ActorRef, ActorSystem}
import org.bitcoins.spvnode.messages.NetworkRequest

/**
  * Created by chris on 7/1/16.
  */
sealed trait PeerRequest {
  def request: NetworkRequest
  def listener: ActorRef
  def actorSystem: ActorSystem
}

object PeerRequest {
  private case class PeerRequestImpl(request: NetworkRequest, listener: ActorRef, actorSystem: ActorSystem) extends PeerRequest
  def apply(request: NetworkRequest, listener: ActorRef, actorSystem: ActorSystem): PeerRequest = {
    PeerRequestImpl(request, listener, actorSystem)
  }
}
