package org.bitcoins.spvnode.serializers.messages.control

import org.bitcoins.core.number.UInt64
import org.bitcoins.core.serializers.RawBitcoinSerializer
import org.bitcoins.spvnode.messages.PingMessage
import org.bitcoins.spvnode.messages.control.PingMessage

/**
  * Created by chris on 6/29/16.
  * https://bitcoin.org/en/developer-reference#ping
  */
trait RawPingMessageSerializer extends RawBitcoinSerializer[PingMessage] {

  override def read(bytes: List[Byte]): PingMessage = {
    val nonce = UInt64(bytes.take(8))
    PingMessage(nonce)
  }

  override def write(pingMessage: PingMessage): String = pingMessage.nonce.hex
}

object RawPingMessageSerializer extends RawPingMessageSerializer