package org.bitcoins.spvnode.messages.data

import org.bitcoins.core.protocol.CompactSizeUInt
import org.bitcoins.core.util.Factory
import org.bitcoins.spvnode.messages.NotFoundMessage
import org.bitcoins.spvnode.serializers.messages.data.RawNotFoundMessageSerializer

/**
  * Created by chris on 6/2/16.
  * The companion object factory used to create NotFoundMessages on the p2p network
  * https://bitcoin.org/en/developer-reference#notfound
  */
object NotFoundMessage extends Factory[NotFoundMessage] {

  private case class NotFoundMessageImpl(inventoryCount : CompactSizeUInt, inventories : Seq[Inventory]) extends NotFoundMessage

  def fromBytes(bytes : Seq[Byte]) : NotFoundMessage = RawNotFoundMessageSerializer.read(bytes)

  def apply(inventoryCount : CompactSizeUInt, inventories : Seq[Inventory]) : NotFoundMessage = {
    NotFoundMessageImpl(inventoryCount,inventories)
  }
}
