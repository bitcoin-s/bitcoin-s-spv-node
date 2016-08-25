package org.bitcoins.spvnode.block

import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.util.BitcoinSLogger
import org.bitcoins.spvnode.gen.MerkleGenerator
import org.scalacheck.{Prop, Properties}

/**
  * Created by chris on 8/12/16.
  */
class MerkleBlockSpec extends Properties("MerkleBlockSpec") with BitcoinSLogger {

/*  property("Serialization symmetry") =
    Prop.forAll(MerkleGenerator.merkleBlockWithInsertedTxIds) {
      case (merkleBlock: MerkleBlock, txIds : Seq[DoubleSha256Digest]) =>
        MerkleBlock(merkleBlock.hex) == merkleBlock
    }*/

  property("contains all inserted txids") =
    Prop.forAllNoShrink(MerkleGenerator.merkleBlockWithInsertedTxIds) {
      case (merkleBlock: MerkleBlock, txIds: Seq[DoubleSha256Digest]) =>
        val extractedMatches = merkleBlock.partialMerkleTree.extractMatches

        logger.warn("Extracted matches: " + extractedMatches)
        logger.warn("Txids: " + txIds)
        logger.warn("Partial merkle tree bits: " + merkleBlock.partialMerkleTree.bits)
        extractedMatches == txIds
    }
}
