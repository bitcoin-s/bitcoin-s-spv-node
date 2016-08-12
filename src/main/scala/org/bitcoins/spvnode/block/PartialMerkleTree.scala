package org.bitcoins.spvnode.block

import org.bitcoins.core.consensus.Merkle
import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.protocol.blockchain.Block
import org.bitcoins.core.number.UInt32
import org.bitcoins.core.util._
import scala.math._
import scala.annotation.tailrec

/**
  * Created by chris on 8/7/16.
  * Represents a subset of known txids inside of a [[Block]]
  * in a way that allows recovery of the txids & merkle root
  * without having to store them all explicitly.
  *
  * Encoding procedure:
  * Traverse the tree in depth first order, storing a bit for each traversal.
  * This bit signifies if the node is a parent of at least one
  * matched leaf txid (or a matched leaf txid) itself.
  * In case we are the leaf level, or this bit is 0, it's merkle
  * node hash is stored and it's children are not explored any further.
  * Otherwise no hash is stored, but we recurse all of this node's child branches.
  *
  * Decoding procedure:
  * The same depth first decoding procedure is performed, but we consume the
  * bits and hashes that we used during encoding
  *
  */
trait PartialMerkleTree extends BitcoinSLogger {
  /** The total number of transactions in this block */
  def numTransactions: Int

  /** Maximum height of the [[tree]] */
  private def maxHeight = Math.ceil((log(numTransactions) / log(2)))

  /** The actual tree used to represent this partial merkle tree*/
  def tree: BinaryTree[DoubleSha256Digest]

  /** A sequence representing if this node is the parent of another node that matched a txid */
  def bits: Seq[Boolean]

  /** Extracts the txids that were matched inside of the bloom filter used to create this partial merkle tree */
  def extractMatches: Seq[DoubleSha256Digest] = {
    logger.info("starting bits: " + bits)
    def loop(subTree: BinaryTree[DoubleSha256Digest],
             remainingBits: Seq[Boolean], height: Int, accumMatches: Seq[DoubleSha256Digest]): (Seq[DoubleSha256Digest], Seq[Boolean]) = {
      if (height == maxHeight) {
        if (remainingBits.head) {
          //means we have a txid node that matched the filter
          subTree match {
            case l : Leaf[DoubleSha256Digest] =>
              logger.info("Adding " + l.v + " to matches")
              logger.info("Remaining bits: " + remainingBits.tail)
              (l.v +: accumMatches, remainingBits.tail)
            case x @ (_ : Node[DoubleSha256Digest] | Empty) => ???
          }
        } else {
          //means we have a txid node, but it did not match the filter
          (accumMatches,remainingBits.tail)
        }
      } else {
        //means we have a nontxid node
        if (remainingBits.head) {
          //means we have a match underneath this node
          subTree match {
            case n: Node[DoubleSha256Digest] =>
              //since we are just trying to extract bloom filter matches, recurse into the two subtrees
              val (leftTreeMatches,leftRemainingBits) = loop(n.l,remainingBits.tail,height+1,accumMatches)
              val (rightTreeMatches,rightRemainingBits) = loop(n.r,leftRemainingBits, height+1, leftTreeMatches)
              (rightTreeMatches,rightRemainingBits)
            case l : Leaf[DoubleSha256Digest] =>
              (accumMatches, remainingBits.tail)
            case Empty => ???

          }
        } else {
          (accumMatches, remainingBits.tail)
        }
      }
    }
    val (matches,remainingBits) = loop(tree,bits,0,Nil)
    require(remainingBits.isEmpty,"We cannot have any left over bits after traversing the tree, got: " + remainingBits)
    matches
  }
}


object PartialMerkleTree extends BitcoinSLogger {

  private case class PartialMerkleTreeImpl(tree: BinaryTree[DoubleSha256Digest], bits: Seq[Boolean], numTransactions: Int) extends PartialMerkleTree

  def apply(txMatches: Seq[(Boolean,DoubleSha256Digest)]): PartialMerkleTree = {
    val txIds = txMatches.map(_._2)
    val merkleTree: Merkle.MerkleTree = Merkle.build(txIds)
    val (tree,bits) = build(merkleTree,txMatches)
    PartialMerkleTreeImpl(tree,bits,txIds.size)
  }


  /**
    *
    * @param fullMerkleTree the full merkle tree which we are going to trim to get a partial merkle tree
    * @param txMatches indicates whether the given txid matches the bloom filter, the full merkle branch needs
    *                  to be included inside of the [[PartialMerkleTree]]
    * @return
    */
  def build(fullMerkleTree: Merkle.MerkleTree, txMatches: Seq[(Boolean,DoubleSha256Digest)]): (BinaryTree[DoubleSha256Digest], Seq[Boolean]) = {
    val maxHeight = Math.ceil((log(txMatches.size) / log(2))).toInt
    logger.info("Tx matches: " + txMatches)
    logger.info("Tx matches size: " + txMatches.size)
    logger.info("max height: "+ maxHeight)

    /**
      * This loops through our merkle tree building [[bits]] so we can instruct another node how to create the partial merkle tree
      * @param tree the tree we are determining how to build it's merkle branches
      * @param bits the accumulator for bits indicating how to reconsctruct the partial merkle tree
      * @param hashes the relevant hashes used with bits to reconstruct the merkle tree
      * @param height the transaction index we are currently looking at -- if it was matched in our bloom filter we need the entire merkle branch
      * @return
      */
    def loop(tree: BinaryTree[DoubleSha256Digest], bits: Seq[Boolean], hashes: Seq[DoubleSha256Digest], height: Int, pos: Int): (BinaryTree[DoubleSha256Digest],
      Seq[Boolean], Seq[DoubleSha256Digest]) = tree match {
      case n : Node[DoubleSha256Digest] =>
        if (matchesTx(maxHeight,height,pos,txMatches)) {
          val newBits = bits ++ Seq(true)
          val (leftTree,leftBits,leftHashes) = loop(n.l,newBits,hashes,height+1,pos)
          val (rightTree,rightBits,rightHashes) = loop(n.r,leftBits,leftHashes,height+1,pos+1)
          (Node(n.v,leftTree,rightTree),rightBits,rightHashes)
        } else {
          val newBits = bits ++ Seq(false)
          val newHashes = hashes ++ Seq(n.v)
          (Leaf(n.v),newBits,newHashes)
        }
      case l : Leaf[DoubleSha256Digest] =>
        if (matchesTx(maxHeight,height,pos,txMatches)) {
          val newBits = bits ++ Seq(true)
          val newHashes = hashes ++ Seq(l.v)
          (l,newBits,newHashes)
        } else {
          val newBits = bits ++ Seq(false)
          val newHashes = hashes ++ Seq(l.v)
          (l,newBits,newHashes)
        }
      case Empty => ???
    }
    val hashes = txMatches.map(_._2)
    val (tree,bits,_) = loop(fullMerkleTree, Nil, hashes, 0,0)
    (tree, bits)
  }


  /** Checks if a node at given the given height and position matches a transaction in the sequence */
  def matchesTx(maxHeight: Int, height: Int, pos: Int, matchedTx: Seq[(Boolean,DoubleSha256Digest)]): Boolean = {
    val startIndex = (maxHeight - height) * pos * 2
    //we have to use min() to check if we have a merkle node that is a a leaf node, but does NOT
    //contain a transaction id hash, it is a duplicated merkle node hash to balance
    //out the merkle tree
    val endIndex = min(matchedTx.size,startIndex + NumberUtil.pow2(maxHeight - height).toInt)
    if (endIndex > matchedTx.size) false
    else {
      val matches = for (i <- startIndex until min(matchedTx.size,endIndex)) yield matchedTx(i)._1
      matches.exists(_ == true)
    }

  }


  def apply(numTransaction: Int, hashes: Seq[DoubleSha256Digest], matches: Seq[Boolean]): BinaryTree[DoubleSha256Digest] = {
    reconstruct(numTransaction,hashes,matches)
  }

  /** Builds a partial merkle tree the information inside of a [[org.bitcoins.spvnode.messages.MerkleBlockMessage]]
    * [[https://bitcoin.org/en/developer-reference#parsing-a-merkleblock-message]]
    *
    * @param numTransaction
    * @param hashes
    * @param matches
    * @return
    */
  def reconstruct(numTransaction: Int, hashes: Seq[DoubleSha256Digest], matches: Seq[Boolean]): BinaryTree[DoubleSha256Digest] = {
    //when we are traversing the tree, if the node is at this height in the tree, we have txid node, otherwise a nontxid node
    val maxHeight = NumberUtil.pow2(numTransaction)

    def loop(remainingHashes: Seq[DoubleSha256Digest], remainingMatches: Seq[Boolean], height: Int) : (BinaryTree[DoubleSha256Digest],Seq[DoubleSha256Digest], Seq[Boolean]) = {
      if (height == maxHeight) {
        //means we have a txid node
        (Leaf(remainingHashes.head),remainingHashes.tail,matches.tail)
      } else {
        //means we have a non txid node
        if (matches.head) {
          val (leftNode,leftRemainingHashes,leftRemainingBits) = loop(remainingHashes,remainingMatches.tail,height+1)
          val (rightNode,rightRemainingHashes, rightRemainingBits) = loop(leftRemainingHashes,leftRemainingBits,height+1)
          val node = Node(remainingHashes.head,leftNode,rightNode)
          (node,rightRemainingHashes,rightRemainingBits)
        } else (Leaf(remainingHashes.head),remainingHashes.tail,matches.tail)
      }
    }
    val (tree,remainingHashes,remainingMatches) = loop(hashes,matches,0)
    require(remainingHashes.size == 0,"We should not have any left over hashes after building our partial merkle tree")
    require(remainingMatches.size == 0, "We should not have any remaining matches after building our partial merkle tree")
    tree
  }

}
