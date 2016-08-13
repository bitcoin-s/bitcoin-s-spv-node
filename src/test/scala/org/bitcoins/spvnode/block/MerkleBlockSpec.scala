package org.bitcoins.spvnode.block

import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.spvnode.gen.MerkleGenerator
import org.scalacheck.{Prop, Properties}

/**
  * Created by chris on 8/12/16.
  */
class MerkleBlockSpec extends Properties("MerkleBlockSpec") {

/*  property("TxIds that were matched in the bloom filter must be extracted from the partial merkle tree") =
    Prop.forAll(MerkleGenerator.merkleBlockWithInsertedTxIds) {
      case (merkleBlock: MerkleBlock, txIds: Seq[DoubleSha256Digest]) =>
      merkleBlock.matchedTransactions == merkleBlock.partialMerkleTree.extractMatches
    }*/
}
