package org.bitcoins.spvnode.serializers.messages.control

import org.bitcoins.core.number.UInt64
import org.bitcoins.core.serializers.RawBitcoinSerializer
import org.bitcoins.spvnode.messages.PongMessage
import org.bitcoins.spvnode.messages.control.PongMessage

/**
  * Created by chris on 7/5/16.
  */
trait RawPongMessageSerializer extends RawBitcoinSerializer[PongMessage] {

  def read(bytes: List[Byte]): PongMessage = {
    PongMessage(UInt64(bytes.take(8)))
  }

  def write(pongMessage: PongMessage): String = {
    pongMessage.nonce.hex
  }
}

object RawPongMessageSerializer extends RawPongMessageSerializer
