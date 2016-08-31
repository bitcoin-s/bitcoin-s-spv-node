package org.bitcoins.spvnode.messages.control

import org.bitcoins.core.protocol.CompactSizeUInt
import org.bitcoins.core.util.Factory
import org.bitcoins.spvnode.messages.AddrMessage
import org.bitcoins.spvnode.serializers.messages.control.RawAddrMessageSerializer
import org.bitcoins.spvnode.util.NetworkIpAddress

/**
  * Created by chris on 6/3/16.
  * The companion object for an AddrMessage
  * https://bitcoin.org/en/developer-reference#addr
  */
object AddrMessage extends Factory[AddrMessage] {

  private case class AddrMessageImpl(ipCount : CompactSizeUInt, addresses : Seq[NetworkIpAddress]) extends AddrMessage

  def fromBytes(bytes : Seq[Byte]) : AddrMessage = RawAddrMessageSerializer.read(bytes)

  def apply(ipCount : CompactSizeUInt, addresses : Seq[NetworkIpAddress]) : AddrMessage = AddrMessageImpl(ipCount,addresses)

}
