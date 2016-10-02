package org.bitcoins.spvnode.networking.sync

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.bitcoins.core.config.{MainNet, TestNet3}
import org.bitcoins.core.gen.BlockchainElementsGenerator
import org.bitcoins.core.protocol.blockchain.{BlockHeader, MainNetChainParams, TestNetChainParams}
import org.bitcoins.spvnode.constant.{Constants, TestConstants}
import org.bitcoins.spvnode.messages.data.HeadersMessage
import org.bitcoins.spvnode.models.BlockHeaderDAO
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
    b ! PoisonPill
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
    val headersReply = probe.expectMsgType[BlockHeaderSyncActor.GetHeadersReply](5.seconds)
    //note the hash we started the sync at is not included in the expected blockheaders we recevie from our peer
    val expectedHashes = Seq(firstBlockHash,secondBlockHash,thirdBlockHash,fourthBlockHash,fifthBlockHash)
    val actualHashes = headersReply.headers.map(_.hash)

    actualHashes.size must be (expectedHashes.size)
    actualHashes must be (expectedHashes)
    b ! PoisonPill
  }

   it must "fail to sync with a GetHeaders message if they are not connected" in {
     val (b,probe) = blockHeaderSyncActor
     val fifthBlockHash = TestUtil.firstFiveTestNetBlockHeaders.last.hash
     b ! BlockHeaderSyncActor.GetHeaders(genesisBlockHash, fifthBlockHash)

     val headers = TestUtil.firstFiveTestNetBlockHeaders.slice(0,2) ++ TestUtil.firstFiveTestNetBlockHeaders.slice(3,TestUtil.firstFiveTestNetBlockHeaders.size)
     val headersMsgMissingHeader = HeadersMessage(headers)
     b ! headersMsgMissingHeader

     probe.expectMsgType[BlockHeaderSyncActor.BlockHeadersDoNotConnect]
     b ! PoisonPill
   }

  it must "stop syncing when we do not receive 2000 block headers from our peer" in {
    val (b,probe) = blockHeaderSyncActor
    b ! BlockHeaderSyncActor.StartHeaders(Seq(TestNetChainParams.genesisBlock.blockHeader))
    val headersMsg = HeadersMessage(TestUtil.firstFiveTestNetBlockHeaders)
    b ! headersMsg
    val reply = probe.expectMsgType[BlockHeaderSyncActor.SuccessfulSyncReply](7.seconds)
    reply.lastHeader must be (TestUtil.firstFiveTestNetBlockHeaders.last)
    b ! PoisonPill
  }

  it must "start syncing at the genesis block when there are no headers in the database" in {
    val (b,probe) = blockHeaderSyncActor
    b ! BlockHeaderSyncActor.StartAtLastSavedHeader
    val lastSavedHeaderReply = probe.expectMsgType[BlockHeaderSyncActor.StartAtLastSavedHeaderReply]
    lastSavedHeaderReply.header must be (Constants.chainParams.genesisBlock.blockHeader)
    b ! PoisonPill
  }

  it must "successfully check two block headers if their difficulty is the same" in {
    val firstHeader = BlockchainElementsGenerator.blockHeader.sample.get
    //note that this header properly references the previous header, but nBits are different
    val secondHeader = BlockchainElementsGenerator.blockHeader(firstHeader.hash,firstHeader.nBits).sample.get
    val checkHeaderResult = BlockHeaderSyncActor.checkHeaders(Some(firstHeader), Seq(secondHeader),0,MainNet)

    checkHeaderResult.error.isDefined must be (false)
    checkHeaderResult.headers must be (Seq(secondHeader))
  }

  it must "successfully check the header of ONLY the genesis block" in {
    val genesisBlockHeader = MainNetChainParams.genesisBlock.blockHeader
    val checkHeaderResult = BlockHeaderSyncActor.checkHeaders(None,Seq(genesisBlockHeader),0,MainNet)
    checkHeaderResult.error.isDefined must be (false)
    checkHeaderResult.headers must be (Seq(genesisBlockHeader))
  }

  it must "successfully check a sequence of headers if their is a difficulty change on the 2016 block" in {
    val firstHeaders = BlockchainElementsGenerator.validHeaderChain(2015).sample.get
    val lastHeader = BlockchainElementsGenerator.blockHeader(firstHeaders.last.hash).sample.get
    val headers = firstHeaders ++ Seq(lastHeader)
    val checkHeaderResult = BlockHeaderSyncActor.checkHeaders(None,headers,0,MainNet)
    checkHeaderResult.error must be (None)
    checkHeaderResult.headers must be (headers)
  }

  it must "fail a checkHeader on a sequence of headers if their is a difficulty change on the 2015 or 2017 block" in {
    val firstHeaders = BlockchainElementsGenerator.validHeaderChain(2014).sample.get
    val lastHeader = BlockchainElementsGenerator.blockHeader(firstHeaders.last.hash).sample.get
    val headers = firstHeaders ++ Seq(lastHeader)
    val checkHeaderResult = BlockHeaderSyncActor.checkHeaders(None,headers,0,MainNet)
    checkHeaderResult.error.isDefined must be (true)
    checkHeaderResult.headers must be (headers)

    val firstHeaders2 = BlockchainElementsGenerator.validHeaderChain(2016).sample.get
    val lastHeader2 = BlockchainElementsGenerator.blockHeader(firstHeaders2.last.hash).sample.get
    val headers2 = firstHeaders ++ Seq(lastHeader2)
    val checkHeaderResult2 = BlockHeaderSyncActor.checkHeaders(None,headers2,0,MainNet)
    checkHeaderResult2.error.isDefined must be (true)
    checkHeaderResult2.headers must be (headers2)
  }

  it must "fail to check two block headers if the network difficulty isn't correct" in {
    val firstHeader = BlockchainElementsGenerator.blockHeader.sample.get
    //note that this header properly references the previous header, but nBits are different
    val secondHeader = BlockchainElementsGenerator.blockHeader(firstHeader.hash).sample.get
    val checkHeaderResult = BlockHeaderSyncActor.checkHeaders(Some(firstHeader), Seq(secondHeader),0,MainNet)

    val errorMsg = checkHeaderResult.error.get.asInstanceOf[BlockHeaderSyncActor.BlockHeaderDifficultyFailure]

    errorMsg.previousBlockHeader must be (firstHeader)
    errorMsg.blockHeader must be (secondHeader)
  }

  /** The [[TestActorRef]] for a [[BlockHeaderSyncActor]] we use for testing */
  private def blockHeaderSyncActor : (TestActorRef[BlockHeaderSyncActor],TestProbe) = {
    val probe = TestProbe()
    val blockHeaderSyncActor: TestActorRef[BlockHeaderSyncActor] = TestActorRef(
      BlockHeaderSyncActor.props(TestConstants, TestNet3),probe.ref)
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
