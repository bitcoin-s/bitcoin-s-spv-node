package org.bitcoins.spvnode.networking

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.io.Tcp
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.bitcoins.core.config.TestNet3
import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.protocol.CompactSizeUInt
import org.bitcoins.core.util.{BitcoinSLogger, BitcoinSUtil}
import org.bitcoins.spvnode.NetworkMessage
import org.bitcoins.spvnode.constant.Constants
import org.bitcoins.spvnode.messages._
import org.bitcoins.spvnode.messages.data.{GetBlocksMessage, GetDataMessage, GetHeadersMessage, Inventory}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpecLike, MustMatchers}

import scala.concurrent.duration.DurationInt

/**
  * Created by chris on 7/1/16.
  */
class PeerMessageHandlerTest extends TestKit(ActorSystem("PeerMessageHandlerTest")) with FlatSpecLike
  with MustMatchers with ImplicitSender
  with BeforeAndAfter with BeforeAndAfterAll with BitcoinSLogger {

  val peerMsgHandler = TestActorRef(PeerMessageHandler.props(system))

  "PeerMessageHandler" must "be able to send a GetHeadersMessage then receive a list of headers back" in {
    val hashStart = DoubleSha256Digest("0000000000000000000000000000000000000000000000000000000000000000")
    //this is the hash of block 2, so this test will send two blocks
    val hashStop = DoubleSha256Digest(BitcoinSUtil.flipEndianess("000000006c02c8ea6e4ff69651f7fcde348fb9d557a06e6957b65552002a7820"))
    val getHeadersMessage = GetHeadersMessage(Constants.version,Seq(hashStart),hashStop)

    val peerRequest = buildPeerRequest(getHeadersMessage)

    peerMsgHandler ! peerRequest

    val successMsg = expectMsgType[PeerMessageHandlerSuccess](10.seconds)
    successMsg.peerRequest must be (peerRequest)
  }

  it must "send a getblocks message and receive a list of blocks back" in {
    val hashStart = DoubleSha256Digest("0000000000000000000000000000000000000000000000000000000000000000")
    //this is the hash of block 2, so this test will send two blocks
    val hashStop = DoubleSha256Digest(BitcoinSUtil.flipEndianess("000000006c02c8ea6e4ff69651f7fcde348fb9d557a06e6957b65552002a7820"))

    val getBlocksMsg = GetBlocksMessage(Constants.version,Seq(hashStart),hashStop)

    val peerRequest = buildPeerRequest(getBlocksMsg)

    peerMsgHandler ! peerRequest

    val successMsg = expectMsgType[PeerMessageHandlerSuccess]

    successMsg.peerRequest must be (peerRequest)

  }

  it must "request a full block from another node" in {
    //first block on testnet
    //https://tbtc.blockr.io/block/info/1
    val blockHash = DoubleSha256Digest(BitcoinSUtil.flipEndianess("00000000b873e79784647a6c82962c70d228557d24a747ea4d1b8bbe878e1206"))
    val getDataMessage = GetDataMessage(Inventory(MsgBlock, blockHash))
    val peerRequest  = buildPeerRequest(getDataMessage)

    peerMsgHandler ! peerRequest

    val successMsg = expectMsgType[PeerMessageHandlerSuccess]

    successMsg.peerRequest must be (peerRequest)
  }


  it must "request a transaction from another node" in {
    //this tx is the coinbase tx in the first block on testnet
    //https://tbtc.blockr.io/tx/info/f0315ffc38709d70ad5647e22048358dd3745f3ce3874223c80a7c92fab0c8ba
    val txId = DoubleSha256Digest(BitcoinSUtil.flipEndianess("a4dd00d23de4f0f96963e16b72afea547bc9ad1d0c1dda5653110eddd83fe0e2"))
    val getDataMessage = GetDataMessage(Inventory(MsgTx, txId))
    val peerRequest = buildPeerRequest(getDataMessage)

    peerMsgHandler ! peerRequest

    logger.debug("Serialized request: " + peerRequest.request.hex)
    val successMsg = expectMsgType[PeerMessageHandlerSuccess]

    successMsg.peerRequest must be (peerRequest)
  }

  private def buildPeerRequest(payload: NetworkPayload): PeerRequest = {
    val networkMsg = NetworkMessage(TestNet3, payload)
    val peerRequest = PeerRequest(networkMsg,peerMsgHandler,TestNet3)
    peerRequest
  }

  override def afterAll = {
    //peerMsgHandler ! Tcp.Close
    TestKit.shutdownActorSystem(system)
  }
}

