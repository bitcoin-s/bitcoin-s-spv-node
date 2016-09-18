package org.bitcoins.spvnode.networking.sync

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.gen.BlockchainElementsGenerator
import org.bitcoins.core.protocol.blockchain.{BlockHeader, TestNetChainParams}
import org.bitcoins.core.util.BitcoinSUtil
import org.bitcoins.spvnode.constant.{Constants, TestConstants}
import org.bitcoins.spvnode.messages.data.HeadersMessage
import org.bitcoins.spvnode.models.BlockHeaderDAO
import org.bitcoins.spvnode.modelsd.BlockHeaderTable
import org.bitcoins.spvnode.util.TestUtil
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, MustMatchers}
import slick.driver.PostgresDriver.api._

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

/**
  * Created by chris on 9/13/16.
  */
class BlockHeaderSyncActorTest extends TestKit(ActorSystem("BlockHeaderSyncActorSpec"))
  with ImplicitSender with FlatSpecLike with MustMatchers with BeforeAndAfterAll {

  val genesisBlockHash = TestNetChainParams.genesisBlock.blockHeader.hash

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
    //genesis block hash is 43497fd7f826957108f4a30fd9cec3aeba79972084e90ead01ea330900000000
    val genesisBlockHash = TestNetChainParams.genesisBlock.blockHeader.hash
    val firstBlockHash = TestUtil.firstFiveTestNetBlockHeaders.head.hash
    val secondBlockhash = TestUtil.firstFiveTestNetBlockHeaders(1).hash
    val thirdBlockhash = TestUtil.firstFiveTestNetBlockHeaders(2).hash
    val fourthBlockhash = TestUtil.firstFiveTestNetBlockHeaders(3).hash
    //5th block hash on testnet
    val fifthBlockHash = TestUtil.firstFiveTestNetBlockHeaders.last.hash
    val blockHeaderSyncActor = TestActorRef(BlockHeaderSyncActor.props,self)

    blockHeaderSyncActor ! BlockHeaderSyncActor.GetHeaders(genesisBlockHash, fifthBlockHash)
    val headers = expectMsgType[Seq[BlockHeader]]
    //note the hash we started the sync at is not included in the expected blockheaders we recevie from our peer
    val expectedHashes = Seq(firstBlockHash,secondBlockhash,thirdBlockhash,fourthBlockhash,fifthBlockHash)
    val actualHashes = headers.map(_.hash)

    actualHashes.size must be (expectedHashes.size)
    actualHashes must be (expectedHashes)
  }

  it must "fail to sync with a GetHeaders message if they are not connected" in {
    val probe = TestProbe()
    val blockHeaderSyncActor = TestActorRef(BlockHeaderSyncActor.props,probe.ref)
    val fifthBlockHash = TestUtil.firstFiveTestNetBlockHeaders.last.hash
    blockHeaderSyncActor ! BlockHeaderSyncActor.GetHeaders(genesisBlockHash, fifthBlockHash)

    val headers = TestUtil.firstFiveTestNetBlockHeaders.slice(0,2) ++ TestUtil.firstFiveTestNetBlockHeaders.slice(3,TestUtil.firstFiveTestNetBlockHeaders.size)
    val headersMsgMissingHeader = HeadersMessage(headers)
    blockHeaderSyncActor ! headersMsgMissingHeader

    probe.expectMsgType[BlockHeaderSyncActor.BlockHeadersDoNotConnect]
  }

  it must "stop syncing when we do not receive 2000 block headers from our peer" in {
    val probe = TestProbe()
    val blockHeaderSyncActor = TestActorRef(BlockHeaderSyncActor.props,probe.ref)
    blockHeaderSyncActor ! BlockHeaderSyncActor.StartHeaders(Seq(genesisBlockHash))

    val headersMsg = HeadersMessage(TestUtil.firstFiveTestNetBlockHeaders)
    blockHeaderSyncActor ! headersMsg

    val lastHeader = probe.expectMsgType[BlockHeader]
    lastHeader must be (TestUtil.firstFiveTestNetBlockHeaders.last)
  }

  it must "start syncing at the genesis block when there are no headers in the database" in {
    val table = TableQuery[BlockHeaderTable]
    val db = TestConstants.database
    Await.result(db.run(table.schema.create), 10.seconds)
    val probe = TestProbe()
    val blockHeaderSyncActor = TestActorRef(BlockHeaderSyncActor.props(TestConstants.database),probe.ref)
    blockHeaderSyncActor ! BlockHeaderSyncActor.StartAtLastSavedHeader
    val lastSavedHeaderReply = probe.expectMsgType[BlockHeaderSyncActor.StartAtLastSavedHeaderReply]
    lastSavedHeaderReply.header must be (Constants.chainParams.genesisBlock.blockHeader)
    Await.result(db.run(table.schema.drop), 10.seconds)
    db.close()
  }




  override def afterAll = {
    TestKit.shutdownActorSystem(system)
  }
}
