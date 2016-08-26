package org.bitcoins.spvnode.serializers.control

import org.bitcoins.core.number.UInt32
import org.bitcoins.core.protocol.CompactSizeUInt
import org.bitcoins.core.serializers.RawBitcoinSerializer
import org.bitcoins.core.util.BitcoinSUtil
import org.bitcoins.spvnode.bloom.{BloomFilter, BloomFlag}
import org.bitcoins.spvnode.messages.FilterLoadMessage
import org.bitcoins.spvnode.messages.control.FilterLoadMessage

/**
  * Created by chris on 7/19/16.
  * Serializes and deserializes a [[FilterLoadMessage]]
  * https://bitcoin.org/en/developer-reference#filterload
  */
trait RawFilterLoadMessageSerializer extends RawBitcoinSerializer[FilterLoadMessage] {

  override def read(bytes: List[Byte]): FilterLoadMessage = {
    val filter = RawBloomFilterSerializer.read(bytes)
    FilterLoadMessage(filter.filterSize,filter.data,filter.hashFuncs,filter.tweak,filter.flags)
  }

  override def write(filterLoadMessage: FilterLoadMessage): String = {
    val bloomFilter = BloomFilter(filterLoadMessage.filterSize, filterLoadMessage.filter,
      filterLoadMessage.hashFuncs, filterLoadMessage.tweak, filterLoadMessage.flags)
    RawBloomFilterSerializer.write(bloomFilter)
  }
}

object RawFilterLoadMessageSerializer extends RawFilterLoadMessageSerializer