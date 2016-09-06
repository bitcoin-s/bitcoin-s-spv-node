package org.bitcoins.spvnode

import org.bitcoins.core.config.TestNet3
import org.bitcoins.core.crypto.{DoubleSha256Digest, Sha256Hash160Digest}
import org.bitcoins.core.protocol.blockchain.TestNetChainParams
import org.bitcoins.core.protocol.{BitcoinAddress, P2PKHAddress}
import org.bitcoins.core.util.BitcoinSUtil
import org.bitcoins.spvnode.constant.Constants
import org.bitcoins.spvnode.networking.PaymentActor
import org.bitcoins.spvnode.networking.sync.BlockHeaderSyncActor

/**
  * Created by chris on 8/29/16.
  */
object Main extends App {


  override def main(args : Array[String]) = {
    val blockHeaderSyncActor = BlockHeaderSyncActor(Constants.actorSystem)
    val gensisBlockHash = TestNetChainParams.genesisBlock.blockHeader.hash
    val startHeader = BlockHeaderSyncActor.StartHeaders(Seq(gensisBlockHash))
    blockHeaderSyncActor ! startHeader
  }
}
