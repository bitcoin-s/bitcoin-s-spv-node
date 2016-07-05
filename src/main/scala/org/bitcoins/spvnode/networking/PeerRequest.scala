package org.bitcoins.spvnode.networking

import akka.actor.{ActorRef, ActorSystem}
import org.bitcoins.core.config.NetworkParameters
import org.bitcoins.spvnode.messages.NetworkRequest

/**
  * Created by chris on 7/1/16.
  */
sealed trait PeerRequest {
  def request: NetworkRequest
  def listener: ActorRef
  def networkParameters: NetworkParameters
}

object PeerRequest {
  private case class PeerRequestImpl(request: NetworkRequest, listener: ActorRef, networkParameters: NetworkParameters) extends PeerRequest
  def apply(request: NetworkRequest, listener: ActorRef, networkParameters: NetworkParameters): PeerRequest = {
    PeerRequestImpl(request, listener, networkParameters)
  }
}
