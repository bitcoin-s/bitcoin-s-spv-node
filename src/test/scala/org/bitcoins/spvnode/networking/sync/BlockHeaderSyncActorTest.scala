package org.bitcoins.spvnode.networking.sync

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.gen.BlockchainElementsGenerator
import org.bitcoins.core.protocol.blockchain.{BlockHeader, TestNetChainParams}
import org.bitcoins.core.util.BitcoinSUtil
import org.bitcoins.spvnode.messages.data.HeadersMessage
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, MustMatchers}

/**
  * Created by chris on 9/13/16.
  */
class BlockHeaderSyncActorTest extends TestKit(ActorSystem("BlockHeaderSyncActorSpec"))
  with ImplicitSender with FlatSpecLike with MustMatchers with BeforeAndAfterAll {


  "BlockHeaderSyncActor" must "send us an error if we receive two block headers that are not connected" in {
    val blockHeaderSyncActor = TestActorRef(BlockHeaderSyncActor.props,self)
    val blockHeader1 = BlockchainElementsGenerator.blockHeader.sample.get
    val blockHeader2 = BlockchainElementsGenerator.blockHeader.sample.get
    val headersMsg = HeadersMessage(Seq(blockHeader2))
    blockHeaderSyncActor ! BlockHeaderSyncActor.StartHeaders(Seq(blockHeader1.hash))
    blockHeaderSyncActor ! headersMsg
    val errorMsg = expectMsgType[BlockHeaderSyncActor.BlockHeadersDoNotConnect]
    errorMsg must be (BlockHeaderSyncActor.BlockHeadersDoNotConnect(blockHeader1.hash,blockHeader2.hash))
  }

  it must "sync the first 5 headers on testnet" in {
    val genesisBlockHash = TestNetChainParams.genesisBlock.blockHeader.hash
    val firstBlockHash = DoubleSha256Digest(BitcoinSUtil.flipEndianess("00000000b873e79784647a6c82962c70d228557d24a747ea4d1b8bbe878e1206"))
    val secondBlockhash = DoubleSha256Digest(BitcoinSUtil.flipEndianess("000000006c02c8ea6e4ff69651f7fcde348fb9d557a06e6957b65552002a7820"))
    val thirdBlockhash = DoubleSha256Digest(BitcoinSUtil.flipEndianess("000000008b896e272758da5297bcd98fdc6d97c9b765ecec401e286dc1fdbe10"))
    val fourthBlockhash = DoubleSha256Digest(BitcoinSUtil.flipEndianess("000000008b5d0af9ffb1741e38b17b193bd12d7683401cecd2fd94f548b6e5dd"))
    //5th block hash on testnet
    val fifthBlockHash = DoubleSha256Digest(BitcoinSUtil.flipEndianess("00000000bc45ac875fbd34f43f7732789b6ec4e8b5974b4406664a75d43b21a1"))
    val blockHeaderSyncActor = TestActorRef(BlockHeaderSyncActor.props,self)

    blockHeaderSyncActor ! BlockHeaderSyncActor.GetHeaders(genesisBlockHash, fifthBlockHash)
    val headers = expectMsgType[Seq[BlockHeader]]
    //note the hash we started the sync at is not included in the expected blockheaders we recevie from our peer
    val expectedHashes = Seq(firstBlockHash,secondBlockhash,thirdBlockhash,fourthBlockhash,fifthBlockHash)
    val actualHashes = headers.map(_.hash)

    actualHashes.size must be (expectedHashes.size)
    actualHashes must be (expectedHashes)
  }

  it must "sync "



  override def afterAll = {
    TestKit.shutdownActorSystem(system)
  }
}
