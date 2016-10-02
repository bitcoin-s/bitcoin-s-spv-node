package org.bitcoins.spvnode.gen

import org.bitcoins.core.gen.{BlockchainElementsGenerator, CryptoGenerators, TransactionGenerators}
import org.bitcoins.core.number.UInt32
import org.bitcoins.spvnode.messages._
import org.bitcoins.spvnode.messages.data.{GetDataMessage, GetHeadersMessage, InventoryMessage, MerkleBlockMessage, _}
import org.scalacheck.Gen

import scala.annotation.tailrec

/**
  * Created by chris on 6/29/16.
  * Responsible for generating random [[DataMessage]]
  * [[https://bitcoin.org/en/developer-reference#data-messages]]
  */
trait DataMessageGenerator {


  /**
    * Generates a random [[GetHeadersMessage]]
    * [[https://bitcoin.org/en/developer-reference#getheaders]]
    * @return
    */
  def getHeaderMessages: Gen[GetHeadersMessage] = for {
    version <- ControlMessageGenerator.protocolVersion
    numHashes <- Gen.choose(0,2000)
    hashes <- CryptoGenerators.doubleSha256DigestSeq(numHashes)
    hashStop <- CryptoGenerators.doubleSha256Digest
  } yield GetHeadersMessage(version,hashes,hashStop)

  def headersMessage: Gen[HeadersMessage] = for {
    randomNum <- Gen.choose(1,10)
    //we have a maximum of 2000 block headers in a HeadersMessage
    blockHeaders <- Gen.listOfN(randomNum,BlockchainElementsGenerator.blockHeader).suchThat(_.size <= 10)
  } yield HeadersMessage(blockHeaders)

  /**
    * Generates a random [[TypeIdentifier]]
    * [[https://bitcoin.org/en/developer-reference#data-messages]]
    * @return
    */
  def typeIdentifier: Gen[TypeIdentifier] = for {
    num <- Gen.choose(1,3)
  } yield TypeIdentifier(UInt32(num))

  /**
    * Generates a random [[Inventory]]
    * [[https://bitcoin.org/en/developer-reference#term-inventory]]
    * @return
    */
  def inventory: Gen[Inventory] = for {
    identifier <- typeIdentifier
    hash <- CryptoGenerators.doubleSha256Digest
  } yield Inventory(identifier,hash)

  /**
    * Generates a random [[InventoryMessage]]
    * [[https://bitcoin.org/en/developer-reference#inv]]
    * @return
    */
  def inventoryMessages: Gen[InventoryMessage] = for {
    numInventories <- Gen.choose(0,500)
    inventories <- Gen.listOfN(numInventories,inventory)
  } yield InventoryMessage(inventories)

  /**
    * Generate a random [[GetDataMessage]]
    * [[https://bitcoin.org/en/developer-reference#getdata]]
    * @return
    */
  def getDataMessages: Gen[GetDataMessage] = for {
    invMsgs <- inventoryMessages
  } yield GetDataMessage(invMsgs.inventoryCount,invMsgs.inventories)

  /**
    * Generates a random [[MerkleBlockMessage]]
    * [[https://bitcoin.org/en/developer-reference#merkleblock]]
    * @return
    */
  def merkleBlockMessage: Gen[MerkleBlockMessage] = for {
    (merkleBlock,_,_) <- MerkleGenerator.merkleBlockWithInsertedTxIds
  } yield MerkleBlockMessage(merkleBlock)

  /** Generates a [[org.bitcoins.spvnode.messages.TransactionMessage]]
    * [[https://bitcoin.org/en/developer-reference#tx]]
    * */
  def transactionMessage: Gen[TransactionMessage] = for {
    tx <- TransactionGenerators.transactions
    txMsg = TransactionMessage(tx)
  } yield txMsg
}

object DataMessageGenerator extends DataMessageGenerator
