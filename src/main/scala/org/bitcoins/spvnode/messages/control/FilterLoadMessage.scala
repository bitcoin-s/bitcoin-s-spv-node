package org.bitcoins.spvnode.messages.control

import org.bitcoins.core.number.{UInt32, UInt64}
import org.bitcoins.core.protocol.CompactSizeUInt
import org.bitcoins.core.util.Factory
import org.bitcoins.spvnode.messages.FilterLoadMessage
import org.bitcoins.spvnode.serializers.control.RawFilterLoadMessageSerializer

/**
  * Created by chris on 7/19/16.
  */
object FilterLoadMessage extends Factory[FilterLoadMessage] {
  private case class FilterLoadMessageImpl(filterSize: CompactSizeUInt, filter: Seq[Byte], hashFuncs: UInt32,
                                           tweak: UInt32, flags: Byte) extends FilterLoadMessage {
    require(filter.bytes.size <= 36000, "Can only have a maximum of 36,000 bytes in our filter, got: " + filter.bytes.size)
    require(hashFuncs <= UInt32(50), "Can only have a maximum of 50 hashFuncs inside FilterLoadMessage, got: " + hashFuncs)
  }

  override def fromBytes(bytes: Seq[Byte]): FilterLoadMessage = RawFilterLoadMessageSerializer.read(bytes)

  def apply(filterSize: CompactSizeUInt, filter: Seq[Byte], hashFuncs: UInt32, tweak: UInt32, flags: Byte): FilterLoadMessage = {
    FilterLoadMessageImpl(filterSize,filter,hashFuncs,tweak,flags)
  }

  def apply(filter: Seq[Byte], hashFuncs: UInt32, tweak: UInt32, flags: Byte): FilterLoadMessage = {
    val filterSize = CompactSizeUInt(UInt64(filter.length))
    FilterLoadMessage(filterSize,filter,hashFuncs,tweak,flags)
  }
}
