package org.bitcoins.spvnode.serializers.messages.data

import org.bitcoins.core.protocol.CompactSizeUInt
import org.bitcoins.core.serializers.RawBitcoinSerializer
import org.bitcoins.core.util.BitcoinSUtil
import org.bitcoins.spvnode.messages.InventoryMessage
import org.bitcoins.spvnode.messages.data.{Inventory, InventoryMessage}

import scala.annotation.tailrec

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
  override def read(bytes : List[Byte]) : InventoryMessage = {
    val inventoryCount = CompactSizeUInt.parseCompactSizeUInt(bytes)
    val inventoryStart = inventoryCount.size.toInt
    val remainingBytes = bytes.slice(inventoryStart,bytes.size)
    val (inventories,_) = parseInventories(remainingBytes,inventoryCount)
    InventoryMessage(inventoryCount, inventories)
  }

  /**
    * Tranforms an inventory object into a hexadecimal string
    * @param inventoryMessage
    * @return
    */
  override def write(inventoryMessage: InventoryMessage) : String = {
    inventoryMessage.inventoryCount.hex + inventoryMessage.inventories.map(_.hex).mkString
  }


  /**
    * Parses the sequence of bytes into a sequence of inventories inside of the inventory message
    * @param bytes the bytes that need to be parsed into Inventories
    * @param requiredInventories the num of inventories inside this sequence of bytes
    * @return the sequence of inventories and the remaining bytes
    */
  private def parseInventories(bytes : Seq[Byte], requiredInventories : CompactSizeUInt) : (Seq[Inventory], Seq[Byte]) = {
    @tailrec
    def loop(remainingInventories : Long, remainingBytes : Seq[Byte], accum : List[Inventory]) : (Seq[Inventory], Seq[Byte]) = {
      if (remainingInventories <= 0) (accum.reverse,remainingBytes)
      else {
        val inventory = RawInventorySerializer.read(remainingBytes.slice(0,36))
        loop(remainingInventories - 1, remainingBytes.slice(36,remainingBytes.size), inventory :: accum )
      }
    }
    loop(requiredInventories.num.toInt, bytes, List())
  }
}

object RawInventoryMessageSerializer extends RawInventoryMessageSerializer