package org.bitcoins.spvnode.serializers.messages.data

import org.bitcoins.core.serializers.RawBitcoinSerializer
import org.bitcoins.spvnode.messages.GetDataMessage
import org.bitcoins.spvnode.messages.data.{GetDataMessage, InventoryMessage}

/**
  * Created by chris on 7/8/16.
  * https://bitcoin.org/en/developer-reference#getdata
  */
trait RawGetDataMessageSerializer extends RawBitcoinSerializer[GetDataMessage] {
  //InventoryMessages & GetDataMessages have the same structure and are serialized the same
  //so we can piggy back off of the serialilzers for InventoryMessages

  def read(bytes: List[Byte]): GetDataMessage = {
    val inv = InventoryMessage(bytes)
    GetDataMessage(inv.inventoryCount,inv.inventories)
  }

  def write(getDataMessage: GetDataMessage): String = {
    val inv = InventoryMessage(getDataMessage.inventoryCount, getDataMessage.inventories)
    inv.hex
  }
}

object RawGetDataMessageSerializer extends RawGetDataMessageSerializer
