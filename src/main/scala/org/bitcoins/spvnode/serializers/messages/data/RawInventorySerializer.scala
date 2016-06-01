package org.bitcoins.spvnode.serializers.messages.data

import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.serializers.RawBitcoinSerializer
import org.bitcoins.spvnode.messages.TypeIdentifier
import org.bitcoins.spvnode.messages.data.Inventory

/**
  * Created by chris on 6/1/16.
  * Serializes/deserializes a inventory
  * https://bitcoin.org/en/developer-reference#term-inventory
  */
trait RawInventorySerializer extends RawBitcoinSerializer[Inventory] {

  def read(bytes : List[Byte]) : Inventory = {
    val typeIdentifier = TypeIdentifier(bytes.take(4))
    val hash = DoubleSha256Digest(bytes.slice(4,bytes.size))
    Inventory(typeIdentifier,hash)
  }

  def write(inventory : Inventory) : String = {
    inventory.typeIdentifier.hex + inventory.hash.hex
  }
}

object RawInventorySerializer extends RawInventorySerializer
