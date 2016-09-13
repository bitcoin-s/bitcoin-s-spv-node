package org.bitcoins.spvnode.serializers.messages.data

import org.bitcoins.core.serializers.RawBitcoinSerializer
import org.bitcoins.spvnode.messages.NotFoundMessage
import org.bitcoins.spvnode.messages.data.{InventoryMessage, NotFoundMessage}

/**
  * Created by chris on 6/2/16.
  * Responsible for the serialization and deserialization of a NotFound message on the p2p network
  * https://bitcoin.org/en/developer-reference#notfound
  */
trait RawNotFoundMessageSerializer extends RawBitcoinSerializer[NotFoundMessage] {


  def read(bytes : List[Byte]) : NotFoundMessage = {
    //this seems funky, but according to the documentation inventory messages
    //and NotFoundMessages have the same structure, therefore we can piggy back
    //off of the serializer used by InventoryMessage
    val inventoryMessage = InventoryMessage(bytes)
    NotFoundMessage(inventoryMessage.inventoryCount, inventoryMessage.inventories)

  }

  def write(notFoundMessage: NotFoundMessage) : String = {
    //Since InventoryMessages and NotFoundMessages have the same format
    //we can just create an inventory message then piggy back off of the
    //serializer used by inventory message
    val inventoryMessage = InventoryMessage(notFoundMessage.inventoryCount, notFoundMessage.inventories)
    inventoryMessage.hex
  }

}

object RawNotFoundMessageSerializer extends RawNotFoundMessageSerializer
