package org.bitcoins.spvnode.messages.data

import org.bitcoins.core.protocol.CompactSizeUInt
import org.bitcoins.core.util.Factory
import org.bitcoins.spvnode.messages._
import org.bitcoins.spvnode.serializers.messages.data.{RawInventoryMessageSerializer}

/**
  * Created by chris on 6/1/16.
  * Creates an scala object that represents the inventory type on the p2p network
  * https://bitcoin.org/en/developer-reference#inv
  */
object InventoryMessage extends Factory[InventoryMessage] {

  override def fromBytes(bytes : Seq[Byte]) : InventoryMessage = InventoryMessageRequest(bytes)

  def apply(inventoryCount : CompactSizeUInt, inventories : Seq[Inventory]) : InventoryMessage = {
    InventoryMessageRequest(inventoryCount,inventories)
  }
}

/**
  * This object creates a [[InventoryMessageRequest]]
  * We need this since [[InventoryMessage]] can be both requests and responses
  */
object InventoryMessageRequest extends Factory[InventoryMessageRequest] {

  private case class InventoryMessageRequestImpl(inventoryCount : CompactSizeUInt,
                                                 inventories : Seq[Inventory]) extends InventoryMessageRequest
  override def fromBytes(bytes : Seq[Byte]) : InventoryMessageRequest = {
    val invMessage = RawInventoryMessageSerializer.read(bytes)
    InventoryMessageRequestImpl(invMessage.inventoryCount, invMessage.inventories)
  }

  def apply(inventoryCount : CompactSizeUInt, inventories : Seq[Inventory]) : InventoryMessageRequest = {
    InventoryMessageRequestImpl(inventoryCount,inventories)
  }
}

/**
  * This object creates a [[InventoryMessageResponse]]
  * We need this since [[InventoryMessage]] can be both requests and responses
  */
object InventoryMessageResponse extends Factory[InventoryMessageResponse] {

  private case class InventoryMessageResponseImpl(inventoryCount : CompactSizeUInt,
                                                  inventories : Seq[Inventory]) extends InventoryMessageResponse

  override def fromBytes(bytes : Seq[Byte]) : InventoryMessageResponse = {
    val invMessage = RawInventoryMessageSerializer.read(bytes)
    InventoryMessageResponseImpl(invMessage.inventoryCount, invMessage.inventories)
  }

  def apply(inventoryCount : CompactSizeUInt, inventories : Seq[Inventory]) : InventoryMessageResponse = {
    InventoryMessageResponseImpl(inventoryCount,inventories)
  }
}
