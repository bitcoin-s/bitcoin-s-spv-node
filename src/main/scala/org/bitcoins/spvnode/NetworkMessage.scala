package org.bitcoins.spvnode

import org.bitcoins.core.protocol.NetworkElement
import org.bitcoins.spvnode.headers.NetworkHeader
import org.bitcoins.spvnode.messages.NetworkPayload

/**
  * Created by chris on 6/10/16.
  * Represents an entire p2p network message in bitcoins
  */
sealed trait NetworkMessage extends NetworkElement {
  def header : NetworkHeader
  def payload : NetworkPayload
  override def hex = header.hex + payload.hex
}


object NetworkMessage {
  private case class NetworkMessageImpl(header : NetworkHeader, payload : NetworkPayload) extends NetworkMessage
  def apply(header : NetworkHeader, payload : NetworkPayload) : NetworkMessage = {
    NetworkMessageImpl(header,payload)
  }
}
