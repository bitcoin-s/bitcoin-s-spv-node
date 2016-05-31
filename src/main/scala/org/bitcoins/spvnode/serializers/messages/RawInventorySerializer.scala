package org.bitcoins.spvnode.serializers.messages

import org.bitcoins.core.serializers.RawBitcoinSerializer
import org.bitcoins.spvnode.messages.Inventory

/**
  * Created by chris on 5/31/16.
  * Serializes and deserializes inventory objects on the peer-to-peer network
  * https://bitcoin.org/en/developer-reference#data-messages
  */
trait RawInventorySerializer extends RawBitcoinSerializer[Inventory] {

  /**
    * Transforms a sequence of bytes into a Inventory object
    * @param bytes
    * @return
    */
  override def read(bytes : List[Byte]) : Inventory = ???

  /**
    * Tranforms an inventory object into a hexadecimal string
    * @param inventory
    * @return
    */
  override def write(inventory: Inventory) : String = ???
}

object RawInventorySerializer extends RawInventorySerializer