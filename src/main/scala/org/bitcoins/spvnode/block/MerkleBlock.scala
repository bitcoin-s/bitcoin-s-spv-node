package org.bitcoins.spvnode.block

import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.protocol.blockchain.{Block, BlockHeader}
import org.bitcoins.core.protocol.transaction.Transaction
import org.bitcoins.spvnode.bloom.BloomFilter

import scala.annotation.tailrec

/**
  * Created by chris on 8/7/16.
  */
trait MerkleBlock {




  def block: Block

  /** Transactions inside of the block that matched our bloom filter and the index they are inside of the block */
  def matchedTransactions: Seq[(Int,DoubleSha256Digest)]

  def filter: BloomFilter

  def flags: Seq[Boolean]

  /** Transaction ids inside of the block */
  def txIds: Seq[DoubleSha256Digest] = block.transactions.map(_.txId)

  /** The [[PartialMerkleTree]] for this merkle block */
  def partialMerkleTree: PartialMerkleTree
}



object MerkleBlock {

  private case class MerkleBlockImpl(block: Block, matchedTransactions : Seq[(Int,DoubleSha256Digest)], flags: Seq[Boolean],
                                     filter: BloomFilter,partialMerkleTree: PartialMerkleTree) extends MerkleBlock
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
        val newFilter = accumFilter.update(tx)
        loop(remainingTxs.tail,newFilter,newTxMatches,newFlags)
      }
    }
    val (matchedTxs,newFilter,flags) = loop(block.transactions,filter,Nil,Nil)
    val txIds = block.transactions.map(_.txId)
    val partialMerkleTree = PartialMerkleTree(txIds,flags)
    MerkleBlockImpl(block, matchedTxs, flags, newFilter, partialMerkleTree)
  }

}
