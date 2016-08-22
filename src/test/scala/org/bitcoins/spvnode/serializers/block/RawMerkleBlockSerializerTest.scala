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
    val merkleBlock = MerkleBlock(BlockHeader(UInt32(3844235037L),
      DoubleSha256Digest("5c16b73e2cb32871dee29a256dcedab0c67a08a10615c4fb11582ce1644d5e8c"),
      DoubleSha256Digest("5aaec95fd80aa3a86517239d1fb7260daff9df9dd16dcc575c2723be1cff1d03"),
      UInt32(2289024085L),UInt32(618007227),UInt32(4071309592L)),UInt32(4),
      List(true, true, true, true, true, false, true),
      PartialMerkleTree(Node(DoubleSha256Digest("dbedb01cb25681ce28d9c29ddb35614b02eb081d30a672d0e2efa2a6faa69201"),
        Node(DoubleSha256Digest("5f258dce875d50455f068ca71831032df8e1cadf47c754efd3732f86ec212f65"),
    Leaf(DoubleSha256Digest("56c93158b1c58f4133df5c8b9d5c13886dbda73e05c3067f5f7c53ac9db74b00")),
    Leaf(DoubleSha256Digest("ec040cb932516acfb3f9b84e898e4ee5bfb79091151f4806de1e841e881eb195"))),
    Node(DoubleSha256Digest("9a9794096e4292d7833896489b182aa572ff85b625af1970ec26fab5fb8cb086"),
      Leaf(DoubleSha256Digest("0f2385c8fc5b15aa0ea04a91327337ba5f9c9ba97b1dd280a1a309d4b083155d")),
    Leaf(DoubleSha256Digest("57b356452d59889f4d7f975c7b7d818b8004d5b691c1a4ef725b142d599d7e19")))),
      4,List(true, true, true, true, true, false, true),
    List(DoubleSha256Digest("56c93158b1c58f4133df5c8b9d5c13886dbda73e05c3067f5f7c53ac9db74b00"),
      DoubleSha256Digest("ec040cb932516acfb3f9b84e898e4ee5bfb79091151f4806de1e841e881eb195"),
      DoubleSha256Digest("0f2385c8fc5b15aa0ea04a91327337ba5f9c9ba97b1dd280a1a309d4b083155d"),
    DoubleSha256Digest("57b356452d59889f4d7f975c7b7d818b8004d5b691c1a4ef725b142d599d7e19"))),None)
    val hex = "1d5f22e55c16b73e2cb32871dee29a256dcedab0c67a08a10615c4fb11582ce1644d5e8c5aaec95fd80aa3a86517239d1fb7260daff9df9dd16dcc575c2723be1cff1d0355bc6f88bb0ad6241841abf2040000000456c93158b1c58f4133df5c8b9d5c13886dbda73e05c3067f5f7c53ac9db74b00ec040cb932516acfb3f9b84e898e4ee5bfb79091151f4806de1e841e881eb1950f2385c8fc5b15aa0ea04a91327337ba5f9c9ba97b1dd280a1a309d4b083155d57b356452d59889f4d7f975c7b7d818b8004d5b691c1a4ef725b142d599d7e19015f"

    val actualMerkleBlock = MerkleBlock(hex)

/*    actualMerkleBlock.blockHeader must be (merkleBlock.blockHeader)
    actualMerkleBlock.flags.size must be (merkleBlock.flags.size)
    actualMerkleBlock.flags must be (merkleBlock.flags)*/
    merkleBlock.hex must be (hex)
  }
}
