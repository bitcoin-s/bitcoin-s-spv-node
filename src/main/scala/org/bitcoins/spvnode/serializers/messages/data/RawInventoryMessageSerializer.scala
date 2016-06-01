package org.bitcoins.spvnode.serializers.messages.data

import org.bitcoins.core.serializers.RawBitcoinSerializer
import org.bitcoins.spvnode.messages.InventoryMessage

/**
  * Created by chris on 5/31/16.
  * Serializes and deserializes inventory objects on the peer-to-peer network
  * https://bitcoin.org/en/developer-reference#inv
  */
trait RawInventoryMessageSerializer extends RawBitcoinSerializer[InventoryMessage] {

  /**
    * Transforms a sequence of bytes into a Inventory object
    * @param bytes
    * @return
    */
  override def read(bytes : List[Byte]) : InventoryMessage = ???

  /**
    * Tranforms an inventory object into a hexadecimal string
    * @param inventoryMessage
    * @return
    */
  override def write(inventoryMessage: InventoryMessage) : String = ???
}

object RawInventoryMessageSerializer extends RawInventoryMessageSerializer