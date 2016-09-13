package org.bitcoins.spvnode.messages.control

import org.bitcoins.core.number.UInt64
import org.bitcoins.core.util.Factory
import org.bitcoins.spvnode.messages.PongMessage
import org.bitcoins.spvnode.serializers.messages.control.RawPongMessageSerializer

/**
  * Created by chris on 7/5/16.
  */
object PongMessage extends Factory[PongMessage] {
  private case class PongMessageImpl(nonce: UInt64) extends PongMessage

  def fromBytes(bytes: Seq[Byte]): PongMessage = {
    val pongMsg = RawPongMessageSerializer.read(bytes)
    PongMessageImpl(pongMsg.nonce)
  }

  def apply(nonce: UInt64): PongMessage = PongMessageImpl(nonce)
}

