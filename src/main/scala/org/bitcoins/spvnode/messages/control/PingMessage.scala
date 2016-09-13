package org.bitcoins.spvnode.messages.control

import org.bitcoins.core.number.UInt64
import org.bitcoins.core.util.Factory
import org.bitcoins.spvnode.messages.{PingMessage}
import org.bitcoins.spvnode.serializers.messages.control.RawPingMessageSerializer



object PingMessage extends Factory[PingMessage] {
  private case class PingMessageImpl(nonce : UInt64) extends PingMessage
  override def fromBytes(bytes: Seq[Byte]): PingMessage = {
    val pingMsg = RawPingMessageSerializer.read(bytes)
    PingMessageImpl(pingMsg.nonce)
  }

  def apply(nonce : UInt64): PingMessage = PingMessageImpl(nonce)
}