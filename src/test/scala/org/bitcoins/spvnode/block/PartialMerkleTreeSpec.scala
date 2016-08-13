package org.bitcoins.spvnode.block

import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.spvnode.gen.MerkleGenerator
import org.scalacheck.{Prop, Properties}

/**
  * Created by chris on 8/12/16.
  */
class PartialMerkleTreeSpec extends Properties("PartialMerkleTreeSpec") {

  property("must be able to extract all of the txids we indicated to be matches") =
    Prop.forAll(MerkleGenerator.partialMerkleTree) {
      case (partialMerkleTree: PartialMerkleTree, txMatches: Seq[(Boolean,DoubleSha256Digest)]) =>
        val matchedTxs = txMatches.filter(_._1).map(_._2)
        partialMerkleTree.extractMatches == matchedTxs

    }
}
