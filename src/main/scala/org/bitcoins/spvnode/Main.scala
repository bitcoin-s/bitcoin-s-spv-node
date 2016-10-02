package org.bitcoins.spvnode

import org.bitcoins.core.protocol.blockchain.TestNetChainParams
import org.bitcoins.spvnode.constant.Constants
import org.bitcoins.spvnode.modelsd.BlockHeaderTable
import org.bitcoins.spvnode.networking.sync.BlockHeaderSyncActor
import slick.driver.PostgresDriver.api._

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
/**
  * Created by chris on 8/29/16.
  */
object Main extends App {


  override def main(args : Array[String]) = {
/*    val table = TableQuery[BlockHeaderTable]
    val db = Constants.database
    Await.result(Constants.database.run(table.schema.create),3.seconds)
    db.close()*/

/*
    val gensisBlockHash = TestNetChainParams.genesisBlock.blockHeader.hash
    val startHeader = BlockHeaderSyncActor.StartHeaders(Seq(gensisBlockHash))

    Constants.database.executor*/
    val blockHeaderSyncActor = BlockHeaderSyncActor(Constants.actorSystem, Constants.dbConfig, Constants.networkParameters)
    blockHeaderSyncActor ! BlockHeaderSyncActor.StartAtLastSavedHeader
  }

}
