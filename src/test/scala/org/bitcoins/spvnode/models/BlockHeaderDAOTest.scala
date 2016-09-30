package org.bitcoins.spvnode.models

import akka.actor.{ActorSystem, PoisonPill}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.bitcoins.core.gen.BlockchainElementsGenerator
import org.bitcoins.core.protocol.blockchain.BlockHeader
import org.bitcoins.spvnode.constant.{Constants, TestConstants}
import org.bitcoins.spvnode.modelsd.BlockHeaderTable
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpecLike, MustMatchers}
import slick.driver.PostgresDriver.api._

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

/**
  * Created by chris on 9/8/16.
  */
class BlockHeaderDAOTest  extends TestKit(ActorSystem("BlockHeaderDAOTest")) with ImplicitSender
  with FlatSpecLike with MustMatchers with BeforeAndAfter with BeforeAndAfterAll {

  val table = TableQuery[BlockHeaderTable]
  val database: Database = TestConstants.database
  val genesisHeader = Constants.chainParams.genesisBlock.blockHeader
  before {
    //Awaits need to be used to make sure this is fully executed before the next test case starts
    //TODO: Figure out a way to make this asynchronous
    Await.result(database.run(table.schema.create), 10.seconds)
    //we need to seed our database with the genesis header to be able to insert subsequent headers
    val (blockHeaderDAO,probe) = blockHeaderDAORef
    blockHeaderDAO ! BlockHeaderDAO.Create(genesisHeader)
    probe.expectMsgType[BlockHeaderDAO.CreateReply](10.seconds)
    blockHeaderDAO ! PoisonPill
  }

  "BlockHeaderDAO" must "insert and read the genesis block header back" in {
    val (blockHeaderDAO,probe) = blockHeaderDAORef

    blockHeaderDAO ! BlockHeaderDAO.Read(genesisHeader.hash)
    val readHeader = probe.expectMsgType[BlockHeaderDAO.ReadReply]
    readHeader.hash.get must be (genesisHeader)

    //also make sure we can query it by height
    blockHeaderDAO ! BlockHeaderDAO.GetAtHeight(0)
    val headersAtHeight0 = probe.expectMsgType[BlockHeaderDAO.GetAtHeightReply]
    headersAtHeight0.headers must be (Seq(genesisHeader))
    blockHeaderDAO ! PoisonPill
  }

  it  must "store a blockheader in the database, then read it from the database" in {
    val (blockHeaderDAO,probe) = blockHeaderDAORef
    val blockHeader = BlockchainElementsGenerator.blockHeader(genesisHeader.hash).sample.get
    blockHeaderDAO ! BlockHeaderDAO.Create(blockHeader)
    val createReply = probe.expectMsgType[BlockHeaderDAO.CreateReply]
    createReply.blockHeader must be (blockHeader)

    blockHeaderDAO ! BlockHeaderDAO.Read(blockHeader.hash)
    val readHeader = probe.expectMsgType[BlockHeaderDAO.ReadReply]
    readHeader.hash.get must be (blockHeader)
    blockHeaderDAO ! PoisonPill
  }

  it must "be able to create multiple block headers in our database at once" in {
    val (blockHeaderDAO,probe) = blockHeaderDAORef
    val blockHeader1 = BlockchainElementsGenerator.blockHeader(genesisHeader.hash).sample.get
    val blockHeader2 = BlockchainElementsGenerator.blockHeader(blockHeader1.hash).sample.get

    val headers = Seq(blockHeader1,blockHeader2)

    blockHeaderDAO ! BlockHeaderDAO.CreateAll(headers)

    val actualBlockHeaders = probe.expectMsgType[BlockHeaderDAO.CreateAllReply]
    actualBlockHeaders.headers must be (headers)
    blockHeaderDAO ! PoisonPill
  }

  it must "delete a block header in the database" in {
    val (blockHeaderDAO,probe) = blockHeaderDAORef
    val blockHeader = BlockchainElementsGenerator.blockHeader(genesisHeader.hash).sample.get
    blockHeaderDAO ! BlockHeaderDAO.Create(blockHeader)
    val CreateReply = probe.expectMsgType[BlockHeaderDAO.CreateReply]

    //delete the header in the db
    blockHeaderDAO ! BlockHeaderDAO.Delete(blockHeader)
    val deleteReply = probe.expectMsgType[BlockHeaderDAO.DeleteReply]
    deleteReply.blockHeader.get must be (blockHeader)

    //make sure we cannot read our deleted header
    blockHeaderDAO ! BlockHeaderDAO.Read(blockHeader.hash)
    val readHeader = probe.expectMsgType[BlockHeaderDAO.ReadReply]
    readHeader.hash must be (None)
    blockHeaderDAO ! PoisonPill
  }

  it must "retrieve the last block header saved in the database" in {
    val (blockHeaderDAO,probe) = blockHeaderDAORef
    val blockHeader = BlockchainElementsGenerator.blockHeader(genesisHeader.hash).sample.get
    blockHeaderDAO ! BlockHeaderDAO.Create(blockHeader)
    probe.expectMsgType[BlockHeaderDAO.CreateReply]

    blockHeaderDAO ! BlockHeaderDAO.LastSavedHeader
    val lastSavedHeader = probe.expectMsgType[BlockHeaderDAO.LastSavedHeaderReply]
    lastSavedHeader.headers.head must be (blockHeader)

    //insert another header and make sure that is the new last header
    val blockHeader2 = BlockchainElementsGenerator.blockHeader(blockHeader.hash).sample.get
    blockHeaderDAO ! BlockHeaderDAO.Create(blockHeader2)
    probe.expectMsgType[BlockHeaderDAO.CreateReply]

    blockHeaderDAO ! BlockHeaderDAO.LastSavedHeader
    val lastSavedHeader2 = probe.expectMsgType[BlockHeaderDAO.LastSavedHeaderReply]
    lastSavedHeader2.headers.head must be (blockHeader2)
    blockHeaderDAO ! PoisonPill
  }


  it must "return the genesis block when retrieving block headers from an empty database" in {
    val (blockHeaderDAO,probe) = blockHeaderDAORef
    blockHeaderDAO ! BlockHeaderDAO.LastSavedHeader
    val lastSavedHeader = probe.expectMsgType[BlockHeaderDAO.LastSavedHeaderReply]
    lastSavedHeader.headers.headOption must be (Some(genesisHeader))
    blockHeaderDAO ! PoisonPill
  }

  it must "retrieve a block header by height" in {
    val (blockHeaderDAO,probe) = blockHeaderDAORef
    val blockHeader = BlockchainElementsGenerator.blockHeader(genesisHeader.hash).sample.get
    blockHeaderDAO ! BlockHeaderDAO.Create(blockHeader)

    probe.expectMsgType[BlockHeaderDAO.CreateReply]

    blockHeaderDAO ! BlockHeaderDAO.GetAtHeight(1)

    val blockHeaderAtHeight = probe.expectMsgType[BlockHeaderDAO.GetAtHeightReply]
    blockHeaderAtHeight.headers.head must be (blockHeader)
    blockHeaderAtHeight.height must be (1)

    //create one at height 2
    val blockHeader2 = BlockchainElementsGenerator.blockHeader(blockHeader.hash).sample.get
    blockHeaderDAO ! BlockHeaderDAO.Create(blockHeader2)

    probe.expectMsgType[BlockHeaderDAO.CreateReply]

    blockHeaderDAO ! BlockHeaderDAO.GetAtHeight(2)

    val blockHeaderAtHeight2 = probe.expectMsgType[BlockHeaderDAO.GetAtHeightReply]

    blockHeaderAtHeight2.headers.head must be (blockHeader2)
    blockHeaderAtHeight2.height must be (2)
    blockHeaderDAO ! PoisonPill
  }


  it must "find the height of a block header" in {
   val (blockHeaderDAO,probe) = blockHeaderDAORef
   val blockHeader = BlockchainElementsGenerator.blockHeader(genesisHeader.hash).sample.get
   blockHeaderDAO ! BlockHeaderDAO.Create(blockHeader)

   probe.expectMsgType[BlockHeaderDAO.CreateReply]

   blockHeaderDAO ! BlockHeaderDAO.FindHeight(blockHeader.hash)
   val foundMessage = probe.expectMsgType[BlockHeaderDAO.FoundHeight]

   foundMessage.headerAtHeight.get._1 must be (1)
   foundMessage.headerAtHeight.get._2 must be (blockHeader)
   blockHeaderDAO ! PoisonPill
  }

  it must "not find the height of a header that DNE in the database" in {
   val (blockHeaderDAO,probe) = blockHeaderDAORef
   val blockHeader = BlockchainElementsGenerator.blockHeader(genesisHeader.hash).sample.get

   blockHeaderDAO ! BlockHeaderDAO.FindHeight(blockHeader.hash)

   val foundMessage = probe.expectMsgType[BlockHeaderDAO.FoundHeight]

   foundMessage.headerAtHeight must be (None)
   blockHeaderDAO ! PoisonPill
  }

  it must "find the height of the longest chain" in {
   val (blockHeaderDAO,probe) = blockHeaderDAORef
   val blockHeader = BlockchainElementsGenerator.blockHeader(genesisHeader.hash).sample.get
   blockHeaderDAO ! BlockHeaderDAO.Create(blockHeader)

   probe.expectMsgType[BlockHeaderDAO.CreateReply]

   blockHeaderDAO ! BlockHeaderDAO.MaxHeight

   val heightReply1 = probe.expectMsgType[BlockHeaderDAO.MaxHeightReply]
   heightReply1.height must be (1)

   val blockHeader2  = BlockchainElementsGenerator.blockHeader(blockHeader.hash).sample.get

   blockHeaderDAO ! BlockHeaderDAO.Create(blockHeader2)
   probe.expectMsgType[BlockHeaderDAO.CreateReply]

   blockHeaderDAO ! BlockHeaderDAO.MaxHeight
   val heightReply2 = probe.expectMsgType[BlockHeaderDAO.MaxHeightReply]
   heightReply2.height must be (2)
   blockHeaderDAO ! PoisonPill
  }

  it must "find the height of two headers that are competing to be the longest chain" in {
    val (blockHeaderDAO,probe) = blockHeaderDAORef
    val blockHeader = BlockchainElementsGenerator.blockHeader(genesisHeader.hash).sample.get
    blockHeaderDAO ! BlockHeaderDAO.Create(blockHeader)
    probe.expectMsgType[BlockHeaderDAO.CreateReply]

    val blockHeader1 = BlockchainElementsGenerator.blockHeader(genesisHeader.hash).sample.get
    blockHeaderDAO ! BlockHeaderDAO.Create(blockHeader1)
    probe.expectMsgType[BlockHeaderDAO.CreateReply]

    //now make sure they are both at height 1
    blockHeaderDAO ! BlockHeaderDAO.GetAtHeight(1)

    val getAtHeightReply = probe.expectMsgType[BlockHeaderDAO.GetAtHeightReply]
    getAtHeightReply must be (BlockHeaderDAO.GetAtHeightReply(1,Seq(blockHeader,blockHeader1)))
    blockHeaderDAO ! PoisonPill
  }


  private def blockHeaderDAORef: (TestActorRef[BlockHeaderDAO], TestProbe) = {
    val probe = TestProbe()
    val blockHeaderDAO: TestActorRef[BlockHeaderDAO] = TestActorRef(BlockHeaderDAO.props(TestConstants),probe.ref)
    (blockHeaderDAO,probe)
  }

  after {
    //Awaits need to be used to make sure this is fully executed before the next test case starts
    //TODO: Figure out a way to make this asynchronous
    Await.result(database.run(table.schema.drop),10.seconds)
  }

  override def afterAll = {
    database.close()
    TestKit.shutdownActorSystem(system)
  }
}
