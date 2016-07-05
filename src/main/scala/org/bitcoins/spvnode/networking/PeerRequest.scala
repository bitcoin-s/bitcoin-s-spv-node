package org.bitcoins.spvnode.networking

import akka.actor.ActorRef
import org.bitcoins.core.config.NetworkParameters
import org.bitcoins.spvnode.NetworkMessage

/**
  * Created by chris on 7/1/16.
  */
sealed trait PeerRequest {
  def request: NetworkMessage
  def listener: ActorRef
  def networkParameters: NetworkParameters
}

object PeerRequest {
  private case class PeerRequestImpl(request: NetworkMessage, listener: ActorRef, networkParameters: NetworkParameters) extends PeerRequest
  def apply(request: NetworkMessage, listener: ActorRef, networkParameters: NetworkParameters): PeerRequest = {
    PeerRequestImpl(request, listener, networkParameters)
  }
}
