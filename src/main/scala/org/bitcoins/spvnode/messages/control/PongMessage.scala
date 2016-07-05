package org.bitcoins.spvnode.messages.control

import org.bitcoins.core.number.UInt64
import org.bitcoins.core.util.Factory
import org.bitcoins.spvnode.messages.{PongMessageRequest, PongMessageResponse}
import org.bitcoins.spvnode.serializers.control.RawPongMessageSerializer

/**
  * Created by chris on 7/5/16.
  */
object PongMessageRequest extends Factory[PongMessageRequest] {
  private case class PongMessageRequestImpl(nonce: UInt64) extends PongMessageRequest

  def fromBytes(bytes: Seq[Byte]): PongMessageRequest = {
    val pongMsg = RawPongMessageSerializer.read(bytes)
    PongMessageRequestImpl(pongMsg.nonce)
  }

  def apply(nonce: UInt64): PongMessageRequest = PongMessageRequestImpl(nonce)
}


object PongMessageResponse extends Factory[PongMessageResponse] {
  private case class PongMessageResponseImpl(nonce: UInt64) extends PongMessageResponse

  def fromBytes(bytes: Seq[Byte]): PongMessageResponse = {
    val pongMsg = RawPongMessageSerializer.read(bytes)
    PongMessageResponseImpl(pongMsg.nonce)
  }

  def apply(nonce: UInt64): PongMessageResponse = PongMessageResponseImpl(nonce)
}
