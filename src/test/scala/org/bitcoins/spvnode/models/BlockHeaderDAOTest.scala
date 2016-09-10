package org.bitcoins.spvnode.models

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.bitcoins.core.gen.BlockchainElementsGenerator
import org.bitcoins.core.protocol.blockchain.BlockHeader
import org.bitcoins.spvnode.modelsd.BlockHeaderTable
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpecLike, MustMatchers}
import slick.backend.DatabaseConfig
import slick.driver.PostgresDriver
import slick.driver.PostgresDriver.api._

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

/**
  * Created by chris on 9/8/16.
  */
class BlockHeaderDAOTest  extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with FlatSpecLike with MustMatchers with BeforeAndAfter with BeforeAndAfterAll {

  val table = TableQuery[BlockHeaderTable]
  val dbConfig: DatabaseConfig[PostgresDriver] = DatabaseConfig.forConfig("databaseUrl")
  val database: Database = dbConfig.db

  before {
    Await.result(database.run(table.schema.create), 10.seconds)
  }

  "BlockHeaderDAO" must "store a blockheader in the database, then read it from the database" in {
    val probe = TestProbe()
    val blockHeader = BlockchainElementsGenerator.blockHeader.sample.get
    val blockHeaderDAO = TestActorRef(BlockHeaderDAO.props,probe.ref)
    blockHeaderDAO ! BlockHeaderDAO.Create(blockHeader)
    val createdHeader = probe.expectMsgType[BlockHeader]
    createdHeader must be (blockHeader)

    blockHeaderDAO ! BlockHeaderDAO.Read(blockHeader.hash)
    val readHeader = probe.expectMsgType[Option[BlockHeader]]
    readHeader.get must be (blockHeader)
  }

  it must "delete a block header in the database" in {
    val probe = TestProbe()
    val blockHeader = BlockchainElementsGenerator.blockHeader.sample.get
    val blockHeaderDAO = TestActorRef(BlockHeaderDAO.props,probe.ref)
    blockHeaderDAO ! BlockHeaderDAO.Create(blockHeader)
    val createdHeader = probe.expectMsgType[BlockHeader]

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
    Await.result(database.run(table.schema.drop),10.seconds)
  }

  override def afterAll = {
    database.close()
    TestKit.shutdownActorSystem(system)
  }
}
