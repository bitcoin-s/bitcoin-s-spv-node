package org.bitcoins.spvnode.networking.sync

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.bitcoins.core.gen.BlockchainElementsGenerator
import org.bitcoins.core.protocol.blockchain.{BlockHeader, TestNetChainParams}
import org.bitcoins.spvnode.constant.{Constants, TestConstants}
import org.bitcoins.spvnode.messages.data.HeadersMessage
import org.bitcoins.spvnode.modelsd.BlockHeaderTable
import org.bitcoins.spvnode.util.TestUtil
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpecLike, MustMatchers}
import slick.driver.PostgresDriver.api._

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

/**
  * Created by chris on 9/13/16.
  */
class BlockHeaderSyncActorTest extends TestKit(ActorSystem("BlockHeaderSyncActorSpec"))
  with ImplicitSender with FlatSpecLike with MustMatchers with BeforeAndAfter with  BeforeAndAfterAll {

  val genesisBlockHash = TestNetChainParams.genesisBlock.blockHeader.hash
  val table = TableQuery[BlockHeaderTable]
  val database: Database = TestConstants.database

  before {
    Await.result(database.run(table.schema.create), 10.seconds)
  }

  "BlockHeaderSyncActor" must "send us an error if we receive two block headers that are not connected" in {
    val (b,probe) = blockHeaderSyncActor
    val blockHeader1 = BlockchainElementsGenerator.blockHeader.sample.get
    val blockHeader2 = BlockchainElementsGenerator.blockHeader.sample.get
    val headersMsg = HeadersMessage(Seq(blockHeader2))
    b ! BlockHeaderSyncActor.StartHeaders(Seq(blockHeader1))
    b ! headersMsg
    val errorMsg = probe.expectMsgType[BlockHeaderSyncActor.BlockHeadersDoNotConnect]
    errorMsg must be (BlockHeaderSyncActor.BlockHeadersDoNotConnect(blockHeader1.hash,blockHeader2.hash))
  }


  it must "sync the first 5 headers on testnet" in {
    //genesis block hash is 43497fd7f826957108f4a30fd9cec3aeba79972084e90ead01ea330900000000
    val genesisBlockHash = TestNetChainParams.genesisBlock.blockHeader.hash
    val firstBlockHash = TestUtil.firstFiveTestNetBlockHeaders.head.hash
    val secondBlockHash = TestUtil.firstFiveTestNetBlockHeaders(1).hash
    val thirdBlockHash = TestUtil.firstFiveTestNetBlockHeaders(2).hash
    val fourthBlockHash = TestUtil.firstFiveTestNetBlockHeaders(3).hash
    //5th block hash on testnet
    val fifthBlockHash = TestUtil.firstFiveTestNetBlockHeaders.last.hash
    val (b,probe) = blockHeaderSyncActor

    b ! BlockHeaderSyncActor.GetHeaders(genesisBlockHash, fifthBlockHash)
    val headers = probe.expectMsgType[Seq[BlockHeader]](5.seconds)
    //note the hash we started the sync at is not included in the expected blockheaders we recevie from our peer
    val expectedHashes = Seq(firstBlockHash,secondBlockHash,thirdBlockHash,fourthBlockHash,fifthBlockHash)
    val actualHashes = headers.map(_.hash)

    actualHashes.size must be (expectedHashes.size)
    actualHashes must be (expectedHashes)
  }

   it must "fail to sync with a GetHeaders message if they are not connected" in {
     val (b,probe) = blockHeaderSyncActor
     val fifthBlockHash = TestUtil.firstFiveTestNetBlockHeaders.last.hash
     b ! BlockHeaderSyncActor.GetHeaders(genesisBlockHash, fifthBlockHash)

     val headers = TestUtil.firstFiveTestNetBlockHeaders.slice(0,2) ++ TestUtil.firstFiveTestNetBlockHeaders.slice(3,TestUtil.firstFiveTestNetBlockHeaders.size)
     val headersMsgMissingHeader = HeadersMessage(headers)
     b ! headersMsgMissingHeader

     probe.expectMsgType[BlockHeaderSyncActor.BlockHeadersDoNotConnect]
   }

  it must "stop syncing when we do not receive 2000 block headers from our peer" in {
    val (b,probe) = blockHeaderSyncActor
    b ! BlockHeaderSyncActor.StartHeaders(Seq(TestNetChainParams.genesisBlock.blockHeader))
    val headersMsg = HeadersMessage(TestUtil.firstFiveTestNetBlockHeaders)
    b ! headersMsg
    val lastHeader = probe.expectMsgType[BlockHeader]
    lastHeader must be (TestUtil.firstFiveTestNetBlockHeaders.last)
  }

  it must "start syncing at the genesis block when there are no headers in the database" in {
    val (b,probe) = blockHeaderSyncActor
    b ! BlockHeaderSyncActor.StartAtLastSavedHeader
    val lastSavedHeaderReply = probe.expectMsgType[BlockHeaderSyncActor.StartAtLastSavedHeaderReply]
    lastSavedHeaderReply.header must be (Constants.chainParams.genesisBlock.blockHeader)
  }

  /** The [[TestActorRef]] for a [[BlockHeaderSyncActor]] we use for testing */
  private def blockHeaderSyncActor : (TestActorRef[BlockHeaderSyncActor],TestProbe) = {
    val probe = TestProbe()
    val blockHeaderSyncActor: TestActorRef[BlockHeaderSyncActor] = TestActorRef(
      BlockHeaderSyncActor.props(TestConstants),probe.ref)
    (blockHeaderSyncActor,probe)
  }

  after {
    Await.result(database.run(table.schema.drop), 10.seconds)
  }


  override def afterAll = {
    database.close()
    TestKit.shutdownActorSystem(system)
  }
}
