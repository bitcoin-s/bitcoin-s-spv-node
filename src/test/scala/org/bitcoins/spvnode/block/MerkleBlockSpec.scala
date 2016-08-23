package org.bitcoins.spvnode.block

import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.util.BitcoinSLogger
import org.bitcoins.spvnode.gen.MerkleGenerator
import org.scalacheck.{Prop, Properties}

/**
  * Created by chris on 8/12/16.
  */
class MerkleBlockSpec extends Properties("MerkleBlockSpec") with BitcoinSLogger {

/*  property("TxIds that were matched in the bloom filter must be extracted from the partial merkle tree") =
    Prop.forAll(MerkleGenerator.merkleBlockWithInsertedTxIds) {
      case (merkleBlock: MerkleBlock, txIds: Seq[DoubleSha256Digest]) =>
      merkleBlock.matchedTransactions == merkleBlock.partialMerkleTree.extractMatches
    }*/
  property("Serialization symmetry") =
    Prop.forAllNoShrink(MerkleGenerator.merkleBlockWithInsertedTxIds) {
      case (merkleBlock: MerkleBlock, txIds : Seq[DoubleSha256Digest]) =>
        logger.debug("Merkle block hex: " + merkleBlock.hex)
        val actualMerkleBlock = MerkleBlock(merkleBlock.hex)
        if (merkleBlock.blockHeader != actualMerkleBlock.blockHeader) {
          println("merkle block header: " + merkleBlock.blockHeader)
          println("actual block header: " + actualMerkleBlock.blockHeader)
          false
        }
        else if (merkleBlock.transactionCount != actualMerkleBlock.transactionCount) {
          println("merkle block tx count: " + merkleBlock.transactionCount)
          println("actual block tx count: " + actualMerkleBlock.transactionCount)
          false
        }
        else if (merkleBlock.hashCount != actualMerkleBlock.hashCount)  {
          println("merkle block hash count: " + merkleBlock.hashCount)
          println("actual block hash count: " + actualMerkleBlock.hashCount)
          false
        }
        else if (merkleBlock.hashes != actualMerkleBlock.hashes) {
          println("merkle block hashes: " + merkleBlock.hashes)
          println("actual block hashes: " + actualMerkleBlock.hashes)
          false
        } else if (merkleBlock.partialMerkleTree != actualMerkleBlock.partialMerkleTree) {
          println("merkle block partial merkle tree: " + merkleBlock.partialMerkleTree)
          println("actual block partial merkle tree: " + actualMerkleBlock.partialMerkleTree)

          false
        }
        else true
    }
}
