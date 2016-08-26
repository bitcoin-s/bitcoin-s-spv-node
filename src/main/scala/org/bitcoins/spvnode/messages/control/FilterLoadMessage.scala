package org.bitcoins.spvnode.messages.control

import org.bitcoins.core.number.{UInt32, UInt64}
import org.bitcoins.core.protocol.CompactSizeUInt
import org.bitcoins.core.util.Factory
import org.bitcoins.spvnode.bloom.{BloomFilter, BloomFlag}
import org.bitcoins.spvnode.messages.FilterLoadMessage
import org.bitcoins.spvnode.serializers.control.RawFilterLoadMessageSerializer

/**
  * Created by chris on 7/19/16.
  */
object FilterLoadMessage extends Factory[FilterLoadMessage] {
  private case class FilterLoadMessageImpl(filterSize: CompactSizeUInt, filter: Seq[Byte], hashFuncs: UInt32,
                                           tweak: UInt32, flags: BloomFlag) extends FilterLoadMessage {
    require(filterSize.num.underlying <= BloomFilter.maxSize.underlying, "Can only have a maximum of 36,000 bytes in our filter, got: " + filter.size)
    require(hashFuncs <= BloomFilter.maxHashFuncs, "Can only have a maximum of 50 hashFuncs inside FilterLoadMessage, got: " + hashFuncs)
    require(filterSize.num.underlying == filter.size, "Filter Size compactSizeUInt and actual filter size were different, " +
      "filterSize: " + filterSize.num + " actual filter size: " + filter.length)
  }

  override def fromBytes(bytes: Seq[Byte]): FilterLoadMessage = RawFilterLoadMessageSerializer.read(bytes)

  def apply(filterSize: CompactSizeUInt, filter: Seq[Byte], hashFuncs: UInt32, tweak: UInt32, flags: BloomFlag): FilterLoadMessage = {
    FilterLoadMessageImpl(filterSize,filter,hashFuncs,tweak,flags)
  }

  def apply(filter: Seq[Byte], hashFuncs: UInt32, tweak: UInt32, flags: BloomFlag): FilterLoadMessage = {
    val filterSize = CompactSizeUInt(UInt64(filter.length))
    FilterLoadMessage(filterSize,filter,hashFuncs,tweak,flags)
  }
}
