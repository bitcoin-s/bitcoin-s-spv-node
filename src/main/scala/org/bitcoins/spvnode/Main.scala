package org.bitcoins.spvnode

import org.bitcoins.core.config.TestNet3
import org.bitcoins.core.crypto.{DoubleSha256Digest, Sha256Hash160Digest}
import org.bitcoins.core.protocol.blockchain.TestNetChainParams
import org.bitcoins.core.protocol.{BitcoinAddress, P2PKHAddress}
import org.bitcoins.core.util.BitcoinSUtil
import org.bitcoins.spvnode.constant.Constants
import org.bitcoins.spvnode.models.BlockHeaderDAO
import org.bitcoins.spvnode.modelsd.BlockHeaderTable
import org.bitcoins.spvnode.networking.PaymentActor
import org.bitcoins.spvnode.networking.sync.BlockHeaderSyncActor
import org.bitcoins.spvnode.store.BlockHeaderStore
import slick.driver.PostgresDriver.api._

import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
/**
  * Created by chris on 8/29/16.
  */
object Main extends App {


  override def main(args : Array[String]) = {
    /*    val blockHeaderSyncActor = BlockHeaderSyncActor(Constants.actorSystem)
    val gensisBlockHash = TestNetChainParams.genesisBlock.blockHeader.hash
    val startHeader = BlockHeaderSyncActor.StartHeaders(Seq(gensisBlockHash))
    blockHeaderSyncActor ! startHeader*/
    val table = TableQuery[BlockHeaderTable]
    Await.result(Constants.database.run(table.schema.create),3.seconds)
    val blockHeaderDAO = BlockHeaderDAO(Constants.actorSystem, Constants.database)
    val blockHeaders = BlockHeaderStore.read
    for {
      header <- blockHeaders
    } {
      blockHeaderDAO ! BlockHeaderDAO.Create(header)
    }
  }
}
