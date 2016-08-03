package org.bitcoins.spvnode.serializers.control

import org.bitcoins.core.number.UInt32
import org.bitcoins.core.protocol.CompactSizeUInt
import org.bitcoins.core.serializers.RawBitcoinSerializer
import org.bitcoins.core.util.BitcoinSUtil
import org.bitcoins.spvnode.bloom.BloomFlag
import org.bitcoins.spvnode.messages.FilterLoadMessage
import org.bitcoins.spvnode.messages.control.FilterLoadMessage

/**
  * Created by chris on 7/19/16.
  * Serializes and deserializes a [[FilterLoadMessage]]
  * https://bitcoin.org/en/developer-reference#filterload
  */
trait RawFilterLoadMessageSerializer extends RawBitcoinSerializer[FilterLoadMessage] {

  override def read(bytes: List[Byte]): FilterLoadMessage = {
    val filterSize = CompactSizeUInt.parseCompactSizeUInt(bytes)
    val filter = bytes.slice(filterSize.size.toInt, filterSize.size.toInt + filterSize.num.toInt)
    val hashFuncsIndex = (filterSize.size + filterSize.num.toInt).toInt
    val hashFuncs = UInt32(BitcoinSUtil.flipEndianess(bytes.slice(hashFuncsIndex,hashFuncsIndex + 4)))
    val tweakIndex = hashFuncsIndex + 4
    val tweak = UInt32(BitcoinSUtil.flipEndianess(bytes.slice(tweakIndex, tweakIndex + 4)))
    val flags = BloomFlag(bytes(tweakIndex+4))
    FilterLoadMessage(filterSize,filter,hashFuncs,tweak,flags)

  }

  override def write(filterLoadMessage: FilterLoadMessage): String = {
    filterLoadMessage.filterSize.hex + BitcoinSUtil.encodeHex(filterLoadMessage.filter) +
      BitcoinSUtil.flipEndianess(filterLoadMessage.hashFuncs.hex) +
      BitcoinSUtil.flipEndianess(filterLoadMessage.tweak.hex) +
      BitcoinSUtil.encodeHex(filterLoadMessage.flags.byte)
  }
}

object RawFilterLoadMessageSerializer extends RawFilterLoadMessageSerializer