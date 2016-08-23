package org.bitcoins.spvnode.serializers.block

import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.number.{UInt32, UInt64}
import org.bitcoins.core.protocol.CompactSizeUInt
import org.bitcoins.core.protocol.blockchain.BlockHeader
import org.bitcoins.core.util.{Leaf, Node}
import org.bitcoins.spvnode.block.{MerkleBlock, PartialMerkleTree}
import org.bitcoins.spvnode.bloom.{BloomFilter, BloomUpdateAll}
import org.scalatest.{FlatSpec, MustMatchers}

/**
  * Created by chris on 8/22/16.
  */
class RawMerkleBlockSerializerTest extends FlatSpec with MustMatchers {


  "RawMerkleBlockSerializer" must "serialize a merkle block generated inside of scalacheck" in {

    val (merkleBlock,txIds) = (MerkleBlock(BlockHeader(UInt32(49150652),
      DoubleSha256Digest("6cf34aac6e3de2bf4b429d114ed4572a7ce4b1c44f2091ae6825ee9774dbae2f"),
      DoubleSha256Digest("4487def8ba376b38c1e4e5910d3c9efd27e740cb9be8d452598cbf2e243fad8a"),
      UInt32(2941790316L),UInt32(1626267458),UInt32(1688549344)),UInt32(1),
      PartialMerkleTree(Leaf(DoubleSha256Digest(
        "442abdc8e74ad35ebd9571f88fda91ff511dcda8d241a5aed52cea1e00d69e03")),
        1,
        List(false, false, false, false, false, false, false, false),
        List(DoubleSha256Digest("442abdc8e74ad35ebd9571f88fda91ff511dcda8d241a5aed52cea1e00d69e03"))),
      None),List())

    val hex = "bcfaed026cf34aac6e3de2bf4b429d114ed4572a7ce4b1c44f2091ae6825ee9774dbae2f4487def8ba376b38c1e4e5910d3c9efd27e740cb9be8d452598cbf2e243fad8a6c2858af42dfee60e037a5640100000001442abdc8e74ad35ebd9571f88fda91ff511dcda8d241a5aed52cea1e00d69e030100"
    val actualMerkleBlock = MerkleBlock(hex)
    actualMerkleBlock must equal (merkleBlock)
  }


  it must "serialize a merkle block that was generated with a bloom filter" in {

    val (merkleBlock,txIds) = (MerkleBlock(BlockHeader(UInt32(3292725001L),
      DoubleSha256Digest("283c25723a89f0334e9a78ea9ffd7aabff53503d848de0086641371c6d0e62c6"),
      DoubleSha256Digest("019289cb4a4d4496b5c5783584121a6c4689d41188dfd637303d95bb04eff1ec"),UInt32(64514862)
    ,UInt32(3900178612L),UInt32(1905530622)),UInt32(7),
    PartialMerkleTree(Node(
      DoubleSha256Digest("084cbd87fd24af35e1a128d9392692ead9019dba52c6300642cbbabee59f608c"),
      Node(DoubleSha256Digest("7487e9c0ed4067d42152540d0c25427795db027be73afb0d4ae8c645f5e99a41"),
        Node(DoubleSha256Digest("9eb20dde2d7668df0b94c4206d4df22e49bea6e148c357b38ac04e5909fff08e"),
          Leaf(DoubleSha256Digest("7828090300611dca2d72f0481c48f4602e8700528aec67d2a8e79e30cd891846")),
          Leaf(DoubleSha256Digest("68e64a40403304c56817604b7ee1ef022015da6adffb5dcf0f059c592e676a4e"))),
        Node(DoubleSha256Digest("eed3046727a0940ecd2240f6b981f5fb5f6089cce253d067bb7f767fc75e39d9"),
          Leaf(DoubleSha256Digest("3eaad7883e243c155540156288f529961bbb133617502ea0cfcd2ceb1fc93a2f")),
          Leaf(DoubleSha256Digest("b0b5e6e65718c6dc24ed53e634076a24693c733ceea950546342ee761ffefb52")))),
      Node(DoubleSha256Digest("fb31d3dcd6dd3fc06ed1c66b6d07297d779669bf609bb1d0dd7c10dd84e324cb"),
        Node(DoubleSha256Digest("d3f77d612d8ad17cd0f9e46c6675b80883c6d5c58b5898a1d0a8df21be3eafa9"),
          Leaf(DoubleSha256Digest("81f256027ca7a786ed85118359de306ba454508fa6ab573d58654c1a183cacd4")),
          Leaf(DoubleSha256Digest("e0e9edc15b68a746decee473c26547538d2dc265af731f178833ec83fa25131b"))),
        Node(DoubleSha256Digest("3e8ab8cce03abe99e9f2e7b3336b0ef414703eb698b574f68247187934304ddf"),
          Leaf(DoubleSha256Digest("8ce4cd3f3418c036dc14601af2dd06f8172fdb55d8b34dd53e7e9132d4d0c146")),
          Leaf(DoubleSha256Digest("8ce4cd3f3418c036dc14601af2dd06f8172fdb55d8b34dd53e7e9132d4d0c146"))))),
      7,List(true, true, true, true, true, true, true, true, true, true, true, true, true, true, false, false),
      List(DoubleSha256Digest("7828090300611dca2d72f0481c48f4602e8700528aec67d2a8e79e30cd891846"),
        DoubleSha256Digest("68e64a40403304c56817604b7ee1ef022015da6adffb5dcf0f059c592e676a4e"),
        DoubleSha256Digest("3eaad7883e243c155540156288f529961bbb133617502ea0cfcd2ceb1fc93a2f"),
        DoubleSha256Digest("b0b5e6e65718c6dc24ed53e634076a24693c733ceea950546342ee761ffefb52"),
        DoubleSha256Digest("81f256027ca7a786ed85118359de306ba454508fa6ab573d58654c1a183cacd4"),
        DoubleSha256Digest("e0e9edc15b68a746decee473c26547538d2dc265af731f178833ec83fa25131b"),
        DoubleSha256Digest("8ce4cd3f3418c036dc14601af2dd06f8172fdb55d8b34dd53e7e9132d4d0c146"))),
    Some(BloomFilter(CompactSizeUInt(UInt64(20),1),List(8, 1, 16, 0, 16, 4, 0, 17, 1, 64, 16, 1, 44, 0, 0, 1, 16, 0, 0, -128),UInt32(1),
      UInt32(3653529924L),BloomUpdateAll))),
    List(DoubleSha256Digest("7828090300611dca2d72f0481c48f4602e8700528aec67d2a8e79e30cd891846"),
      DoubleSha256Digest("68e64a40403304c56817604b7ee1ef022015da6adffb5dcf0f059c592e676a4e"),
      DoubleSha256Digest("3eaad7883e243c155540156288f529961bbb133617502ea0cfcd2ceb1fc93a2f"),
      DoubleSha256Digest("b0b5e6e65718c6dc24ed53e634076a24693c733ceea950546342ee761ffefb52"),
      DoubleSha256Digest("81f256027ca7a786ed85118359de306ba454508fa6ab573d58654c1a183cacd4"),
      DoubleSha256Digest("e0e9edc15b68a746decee473c26547538d2dc265af731f178833ec83fa25131b"),
      DoubleSha256Digest("8ce4cd3f3418c036dc14601af2dd06f8172fdb55d8b34dd53e7e9132d4d0c146")))


    val m = MerkleBlock(merkleBlock.hex)
    val expectedHex = merkleBlock.hex
    m.hex must be (expectedHex)
  }
}
