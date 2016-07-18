package org.bitcoins.spvnode.messages.data

import org.bitcoins.core.number.UInt64
import org.bitcoins.core.protocol.CompactSizeUInt
import org.bitcoins.core.protocol.blockchain.BlockHeader
import org.bitcoins.core.util.Factory
import org.bitcoins.spvnode.messages.HeadersMessage
import org.bitcoins.spvnode.serializers.messages.data.RawHeadersMessageSerializer

/**
  * Created by chris on 7/5/16.
  */
object HeadersMessage extends Factory[HeadersMessage] {
  private case class HeadersMessageImpl(count: CompactSizeUInt, headers: Seq[BlockHeader]) extends HeadersMessage

  def fromBytes(bytes: Seq[Byte]): HeadersMessage = RawHeadersMessageSerializer.read(bytes)

  def apply(count: CompactSizeUInt, headers: Seq[BlockHeader]): HeadersMessage = HeadersMessageImpl(count,headers)

  def apply(headers: Seq[BlockHeader]): HeadersMessage = {
    val count = CompactSizeUInt(UInt64(headers.length))
    HeadersMessageImpl(count,headers)
  }
}
