package org.bitcoins.spvnode.serializers.messages.control

import org.bitcoins.core.protocol.CompactSizeUInt
import org.bitcoins.core.serializers.RawBitcoinSerializer
import org.bitcoins.core.util.BitcoinSUtil
import org.bitcoins.spvnode.messages.FilterAddMessage
import org.bitcoins.spvnode.messages.control.FilterAddMessage
/**
  * Created by chris on 8/26/16.
  * Responsible for serializing and deserializing a [[FilterAddMessage]]
  * [[https://bitcoin.org/en/developer-reference#filteradd]]
  */
trait RawFilterAddMessageSerializer extends RawBitcoinSerializer[FilterAddMessage] {


  override def read(bytes: List[Byte]): FilterAddMessage = {
    val elementSize = CompactSizeUInt.parseCompactSizeUInt(bytes)
    val element = bytes.slice(elementSize.size.toInt, bytes.size)
    FilterAddMessage(elementSize, element)
  }

  override def write(filterAddMessage: FilterAddMessage): String = {
    filterAddMessage.elementSize.hex ++ BitcoinSUtil.encodeHex(filterAddMessage.element)
  }
}

object RawFilterAddMessageSerializer extends RawFilterAddMessageSerializer
