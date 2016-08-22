package org.bitcoins.spvnode.block

import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.number.{UInt32, UInt64}
import org.bitcoins.core.protocol.{CompactSizeUInt, NetworkElement}
import org.bitcoins.core.protocol.blockchain.{Block, BlockHeader}
import org.bitcoins.core.protocol.transaction.Transaction
import org.bitcoins.core.util.{BitcoinSLogger, Factory}
import org.bitcoins.spvnode.bloom.BloomFilter
import org.bitcoins.spvnode.serializers.block.RawMerkleBlockSerializer

import scala.annotation.tailrec

/**
  * Created by chris on 8/7/16.
  */
trait MerkleBlock extends NetworkElement with BitcoinSLogger {

  def blockHeader: BlockHeader

  def transactionCount: UInt32

  /** The amount of hashes inside of the merkle block */
  def hashCount: CompactSizeUInt = CompactSizeUInt(UInt64(hashes.size))

  /** One or more hashes of both transactions and merkle nodes used to build the partial merkle tree */
  def hashes: Seq[DoubleSha256Digest] = partialMerkleTree.hashes

  /** The size of the flags field in bytes */
  def flagCount: CompactSizeUInt = CompactSizeUInt(UInt64(Math.ceil(flags.size.toDouble / 8).toInt))

  /** A sequence of bits packed eight in a byte with the least significant bit first.
    * May be padded to the nearest byte boundary but must not contain any more bits than that.
    * Used to assign the hashes to particular nodes in the merkle tree.
    */
  def flags: Seq[Boolean]

  /** The [[PartialMerkleTree]] for this merkle block */
  def partialMerkleTree: PartialMerkleTree

  def filter: Option[BloomFilter]

  def hex = RawMerkleBlockSerializer.write(this)
}



object MerkleBlock extends Factory[MerkleBlock] {

  private case class MerkleBlockImpl(blockHeader: BlockHeader, transactionCount: UInt32,
                                     //matchedTransactions: Seq[(Int,DoubleSha256Digest)],
                                     flags: Seq[Boolean],
                                     partialMerkleTree: PartialMerkleTree, filter: Option[BloomFilter]) extends MerkleBlock
  /**
    * Creates a [[MerkleBlock]] from the given [[Block]] and [[BloomFilter]]
    * This function iterates through each transaction inside our block checking if it is relevant to the given bloom filter
    * If it is relevant, it will set a flag to indicate we should include it inside of our [[PartialMerkleTree]]
    * @param block
    * @param filter
    * @return
    */
  def apply(block: Block, filter: BloomFilter): MerkleBlock = {
    @tailrec
    def loop(remainingTxs: Seq[Transaction], accumFilter: BloomFilter,
             txMatches: Seq[(Int,DoubleSha256Digest)], matches: Seq[Boolean]): (Seq[(Int,DoubleSha256Digest)], BloomFilter, Seq[Boolean]) = {
      if (remainingTxs.isEmpty) (txMatches.reverse,accumFilter,matches.reverse)
      else {
        val tx = remainingTxs.head
        val (newTxMatches,newFlags) = accumFilter.isRelevant(tx) match {
          case true =>
            val index = block.transactions.size - remainingTxs.size
            val newTxMatches = (index,tx.txId) +: txMatches
            (newTxMatches, true +: matches)
          case false =>
            (txMatches, false +: matches)
        }
        val newFilter =  accumFilter.update(tx)
        loop(remainingTxs.tail,newFilter,newTxMatches,newFlags)
      }
    }
    val (matchedTxs,newFilter,flags) = loop(block.transactions,filter,Nil,Nil)
    val txIds = block.transactions.map(_.txId)
    val partialMerkleTree = PartialMerkleTree(flags.zip(txIds))
    val txCount = UInt32(block.transactions.size)
    MerkleBlock(block.blockHeader, txCount, partialMerkleTree.bits, partialMerkleTree,Some(newFilter))
  }


  /** Creates a merkle block that matches the given txids if they appear inside the given block */
  def apply(block: Block, txIds: Seq[DoubleSha256Digest]): MerkleBlock = {
    //follows this function inside of bitcoin core
    //https://github.com/bitcoin/bitcoin/blob/master/src/merkleblock.cpp#L40
    @tailrec
    def loop(remainingTxs: Seq[Transaction], txMatches: Seq[(Int,DoubleSha256Digest)], matches: Seq[Boolean]): (Seq[(Int,DoubleSha256Digest)], Seq[Boolean]) = {
      if (remainingTxs.isEmpty) (txMatches.reverse, matches.reverse)
      else {
        val tx = remainingTxs.head
        val (newTxMatches, newFlags) = txIds.contains(tx.txId) match {
          case true =>
            val index = block.transactions.size - remainingTxs.size
            val newTxMatches = (index,tx.txId) +: txMatches
            (newTxMatches, true +: matches)
          case false =>
            (txMatches, false +: matches)
        }
        loop(remainingTxs.tail,newTxMatches,newFlags)
      }
    }

    val (matchedTxs, flags) = loop(block.transactions,Nil,Nil)
    val partialMerkleTree = PartialMerkleTree(flags.zip(txIds))
    val txCount = UInt32(block.transactions.size)
    MerkleBlock(block.blockHeader,txCount,flags,partialMerkleTree,None)
  }


  def apply(blockHeader: BlockHeader, txCount: UInt32,
            flags: Seq[Boolean], partialMerkleTree: PartialMerkleTree, filter: Option[BloomFilter]): MerkleBlock = {

    MerkleBlockImpl(blockHeader,txCount,flags,partialMerkleTree,filter)
  }


  def apply(blockHeader: BlockHeader, txCount: UInt32, hashes: Seq[DoubleSha256Digest], flags: Seq[Boolean]): MerkleBlock = {
    val partialMerkleTree = PartialMerkleTree(txCount,hashes,flags)
    MerkleBlock(blockHeader,txCount,flags,partialMerkleTree,None)
  }

  def fromBytes(bytes: Seq[Byte]): MerkleBlock = RawMerkleBlockSerializer.read(bytes)
}
