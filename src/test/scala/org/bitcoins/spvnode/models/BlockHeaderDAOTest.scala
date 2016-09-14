package org.bitcoins.spvnode.models

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.bitcoins.core.gen.BlockchainElementsGenerator
import org.bitcoins.core.protocol.blockchain.BlockHeader
import org.bitcoins.spvnode.constant.TestConstants
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

  before {
    //Awaits need to be used to make sure this is fully executed before the next test case starts
    //TODO: Figure out a way to make this asynchronous
    Await.result(database.run(table.schema.create), 10.seconds)
  }

  "BlockHeaderDAO" must "store a blockheader in the database, then read it from the database" in {
    val probe = TestProbe()
    val blockHeader = BlockchainElementsGenerator.blockHeader.sample.get
    val blockHeaderDAO = TestActorRef(BlockHeaderDAO.props(database),probe.ref)
    blockHeaderDAO ! BlockHeaderDAO.Create(blockHeader)
    val createdHeader = probe.expectMsgType[BlockHeaderDAO.CreatedHeader]
    createdHeader.header must be (blockHeader)

    blockHeaderDAO ! BlockHeaderDAO.Read(blockHeader.hash)
    val readHeader = probe.expectMsgType[Option[BlockHeader]]
    readHeader.get must be (blockHeader)
  }

  it must "be able to create multiple block headers in our database at once" in {
    val probe = TestProbe()
    val blockHeaderDAO = TestActorRef(BlockHeaderDAO.props(database),probe.ref)
    val blockHeader1 = BlockchainElementsGenerator.blockHeader.sample.get
    val blockHeader2 = BlockchainElementsGenerator.blockHeader.sample.get

    val headers = Seq(blockHeader1,blockHeader2)

    blockHeaderDAO ! BlockHeaderDAO.CreateAll(headers)

    val actualBlockHeaders = probe.expectMsgType[BlockHeaderDAO.CreatedHeaders]
    actualBlockHeaders.headers must be (headers)


  }

  it must "delete a block header in the database" in {
    val probe = TestProbe()
    val blockHeader = BlockchainElementsGenerator.blockHeader.sample.get
    val blockHeaderDAO = TestActorRef(BlockHeaderDAO.props(database),probe.ref)
    blockHeaderDAO ! BlockHeaderDAO.Create(blockHeader)
    val createdHeader = probe.expectMsgType[BlockHeaderDAO.CreatedHeader]

    //delete the header in the db
    blockHeaderDAO ! BlockHeaderDAO.Delete(blockHeader)
    val affectedRows = probe.expectMsgType[Int]
    affectedRows must be (1)

    //make sure we cannot read our deleted header
    blockHeaderDAO ! BlockHeaderDAO.Read(blockHeader.hash)
    val readHeader = probe.expectMsgType[Option[BlockHeader]]
    readHeader must be (None)
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
