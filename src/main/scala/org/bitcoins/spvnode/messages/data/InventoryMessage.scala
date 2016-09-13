package org.bitcoins.spvnode.messages.data

import org.bitcoins.core.number.UInt64
import org.bitcoins.core.protocol.CompactSizeUInt
import org.bitcoins.core.util.Factory
import org.bitcoins.spvnode.messages.InventoryMessage
import org.bitcoins.spvnode.serializers.messages.data.RawInventoryMessageSerializer

/**
  * Created by chris on 6/1/16.
  * Creates an scala object that represents the inventory type on the p2p network
  * https://bitcoin.org/en/developer-reference#inv
  */
object InventoryMessage extends Factory[InventoryMessage] {

  private case class InventoryMessageImpl(inventoryCount: CompactSizeUInt, inventories: Seq[Inventory]) extends InventoryMessage
  override def fromBytes(bytes : Seq[Byte]) : InventoryMessage = RawInventoryMessageSerializer.read(bytes)

  def apply(inventoryCount : CompactSizeUInt, inventories : Seq[Inventory]) : InventoryMessage = {
    InventoryMessageImpl(inventoryCount,inventories)
  }

  def apply(inventories: Seq[Inventory]): InventoryMessage = {
    val count = CompactSizeUInt(UInt64(inventories.length))
    InventoryMessage(count, inventories)
  }
}

