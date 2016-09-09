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

/**
  * Created by chris on 9/8/16.
  */
class BlockHeaderDAOTest  extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with FlatSpecLike with MustMatchers with BeforeAndAfter {
  val table = TableQuery[BlockHeaderTable]
  val dbConfig: DatabaseConfig[PostgresDriver] = DatabaseConfig.forConfig("databaseUrl")
  val database: Database = dbConfig.db

  before {
    database.run(table.schema.create)
  }

  "BlockHeaderDAO" must "store a blockheader in the database, then read it from the database" in {
    val probe = TestProbe()
    val blockHeader = BlockchainElementsGenerator.blockHeader.sample.get
    val blockHeaderDAO = TestActorRef(BlockHeaderDAO.props,probe.ref)
    blockHeaderDAO ! BlockHeaderDAO.Create(blockHeader)
    val createdHeader = probe.expectMsgType[BlockHeader]

    createdHeader must be (blockHeader)


  }

  after {
    database.run(table.schema.drop)
    database.close()
  }
}
