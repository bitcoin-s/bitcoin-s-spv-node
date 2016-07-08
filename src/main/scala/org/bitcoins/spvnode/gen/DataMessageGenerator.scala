package org.bitcoins.spvnode.gen

import org.bitcoins.core.gen.CryptoGenerators
import org.bitcoins.core.number.UInt32
import org.bitcoins.spvnode.messages._
import org.bitcoins.spvnode.messages.data.{GetDataMessage, GetHeadersMessage, Inventory, InventoryMessage}
import org.scalacheck.Gen

import scala.annotation.tailrec

/**
  * Created by chris on 6/29/16.
  */
trait DataMessageGenerator {


  def getHeaderMessages: Gen[GetHeadersMessage] = for {
    version <- ControlMessageGenerator.protocolVersion
    numHashes <- Gen.choose(0,2000)
    hashes <- CryptoGenerators.doubleSha256DigestSeq(numHashes)
    hashStop <- CryptoGenerators.doubleSha256Digest
  } yield GetHeadersMessage(version,hashes,hashStop)


  def typeIdentifier: Gen[TypeIdentifier] = for {
    num <- Gen.choose(1,3)
  } yield TypeIdentifier(UInt32(num))

  def inventory: Gen[Inventory] = for {
    identifier <- typeIdentifier
    hash <- CryptoGenerators.doubleSha256Digest
  } yield Inventory(identifier,hash)

  def inventoryMessages: Gen[InventoryMessage] = for {
    numInventories <- Gen.choose(0,500)
    inventories <- Gen.listOfN(numInventories,inventory)
  } yield InventoryMessage(inventories)

  def getDataMessages: Gen[GetDataMessage] = for {
    invMsgs <- inventoryMessages
  } yield GetDataMessage(invMsgs.inventoryCount,invMsgs.inventories)

}

object DataMessageGenerator extends DataMessageGenerator
