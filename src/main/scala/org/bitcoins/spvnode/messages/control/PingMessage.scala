package org.bitcoins.spvnode.messages.control

import org.bitcoins.core.number.UInt64
import org.bitcoins.core.util.Factory
import org.bitcoins.spvnode.messages.{PingMessageRequest, PingMessageResponse}
import org.bitcoins.spvnode.serializers.control.RawPingMessageSerializer



object PingMessageRequest extends Factory[PingMessageRequest] {
  private case class PingMessageRequestImpl(nonce : UInt64) extends PingMessageRequest
  override def fromBytes(bytes: Seq[Byte]): PingMessageRequest = {
    val pingMsg = RawPingMessageSerializer.read(bytes)
    PingMessageRequestImpl(pingMsg.nonce)
  }

  def apply(nonce : UInt64): PingMessageRequest = PingMessageRequestImpl(nonce)
}

object PingMessageResponse extends Factory[PingMessageResponse] {
  private case class PingMessageResponseImpl(nonce : UInt64) extends PingMessageResponse
  override def fromBytes(bytes: Seq[Byte]): PingMessageResponse = {
    val pingMsg = RawPingMessageSerializer.read(bytes)
    PingMessageResponseImpl(pingMsg.nonce)
  }

  def apply(nonce : UInt64): PingMessageResponse = PingMessageResponseImpl(nonce)
}