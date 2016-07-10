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
}

object PeerRequest {
  private case class PeerRequestImpl(request: NetworkMessage, listener: ActorRef) extends PeerRequest

  def apply(request: NetworkMessage, listener: ActorRef): PeerRequest = PeerRequestImpl(request, listener)

}
