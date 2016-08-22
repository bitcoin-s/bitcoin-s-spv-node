package org.bitcoins.spvnode.serializers.block

import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.number.UInt32
import org.bitcoins.core.protocol.blockchain.BlockHeader
import org.bitcoins.core.util.{Leaf, Node}
import org.bitcoins.spvnode.block.{MerkleBlock, PartialMerkleTree}
import org.scalatest.{FlatSpec, MustMatchers}

/**
  * Created by chris on 8/22/16.
  */
class RawMerkleBlockSerializerTest extends FlatSpec with MustMatchers {


  "RawMerkleBlockSerializer" must "serialize a merkle block generated inside of scalacheck" in {

    val merkleBlock = (MerkleBlock(BlockHeader(UInt32(2021395539),
      DoubleSha256Digest("82207719b5dc825398d520519dc96f5136749e5420ded246909d692ce3361a55"),
      DoubleSha256Digest("31c433bc5b2864f063fcd804526fbdb4f3865ee878dde49d2adbee68ced2a1de"),
      UInt32(111251572),UInt32(2005219688),UInt32(1820698264)),UInt32(1),
      PartialMerkleTree(Leaf(DoubleSha256Digest("ac66aa5cef330f19eb3ab4c78c6cd9f87b82b67a7abe39ce7e7382e15072c3e6")),
        1,List(false, false, false, false, false, false, false, false),
        List(DoubleSha256Digest("ac66aa5cef330f19eb3ab4c78c6cd9f87b82b67a7abe39ce7e7382e15072c3e6"))),
      None))

    val hex = "530c7c7882207719b5dc825398d520519dc96f5136749e5420ded246909d692ce3361a5531c433bc5b2864f063fcd804526fbdb4f3865ee878dde49d2adbee68ced2a1de7490a1066839857798a6856c0100000001ac66aa5cef330f19eb3ab4c78c6cd9f87b82b67a7abe39ce7e7382e15072c3e60100"
    val actualMerkleBlock = MerkleBlock(hex)
    actualMerkleBlock.blockHeader must be (merkleBlock.blockHeader)
    actualMerkleBlock.partialMerkleTree.bits.size must be (merkleBlock.partialMerkleTree.bits.size)
    actualMerkleBlock.partialMerkleTree.bits must be (merkleBlock.partialMerkleTree.bits)
    merkleBlock.hex must be (hex)
    actualMerkleBlock must be (merkleBlock)
  }
}
