package org.bitcoins.spvnode.networking

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.io.Tcp
import akka.testkit.{TestKit, TestProbe}
import org.bitcoins.core.config.TestNet3
import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.protocol.CompactSizeUInt
import org.bitcoins.core.util.{BitcoinSLogger, BitcoinSUtil}
import org.bitcoins.spvnode.NetworkMessage
import org.bitcoins.spvnode.constant.Constants
import org.bitcoins.spvnode.messages.{HeadersMessage, InventoryMessage, MsgBlock, NetworkPayload}
import org.bitcoins.spvnode.messages.data.{GetBlocksMessage, GetHeadersMessage, Inventory}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpecLike, MustMatchers}

import scala.concurrent.duration.DurationInt

/**
  * Created by chris on 7/1/16.
  */
class PeerMessageHandlerTest extends TestKit(ActorSystem("PeerMessageHandlerTest")) with FlatSpecLike with MustMatchers
  with BeforeAndAfter with BeforeAndAfterAll with BitcoinSLogger {

  val peerMsgHandler = PeerMessageHandler(system)

  "PeerMessageHandler" must "be able to send a GetHeadersMessage then receive a list of headers back" in {
    val hashStart = DoubleSha256Digest("0000000000000000000000000000000000000000000000000000000000000000")
    //this is the hash of block 2, so this test will send two blocks
    val hashStop = DoubleSha256Digest(BitcoinSUtil.flipEndianess("000000006c02c8ea6e4ff69651f7fcde348fb9d557a06e6957b65552002a7820"))
    val getHeadersMessage = GetHeadersMessage(Constants.version,Seq(hashStart),hashStop)

    val (peerRequest,probe) = buildPeerRequest(getHeadersMessage)

    peerMsgHandler ! peerRequest

    val headersMsg = probe.expectMsgType[HeadersMessage](10.seconds)
    headersMsg.commandName must be (NetworkPayload.headersCommandName)
    val firstHeader = headersMsg.headers.head
    firstHeader.hash.hex must be (BitcoinSUtil.flipEndianess("00000000b873e79784647a6c82962c70d228557d24a747ea4d1b8bbe878e1206"))

    val secondHeader = headersMsg.headers(1)
    secondHeader.hash.hex must be (BitcoinSUtil.flipEndianess("000000006c02c8ea6e4ff69651f7fcde348fb9d557a06e6957b65552002a7820"))


  }

  it must "send a getblocks message and receive a list of blocks back" in {
    val hashStart = DoubleSha256Digest("0000000000000000000000000000000000000000000000000000000000000000")
    //this is the hash of block 2, so this test will send two blocks
    val hashStop = DoubleSha256Digest(BitcoinSUtil.flipEndianess("000000006c02c8ea6e4ff69651f7fcde348fb9d557a06e6957b65552002a7820"))

    val getBlocksMsg = GetBlocksMessage(Constants.version,Seq(hashStart),hashStop)

    val (peerRequest,probe) = buildPeerRequest(getBlocksMsg)

    peerMsgHandler ! peerRequest

    val invMsg = probe.expectMsgType[InventoryMessage]

    invMsg.inventoryCount must be (CompactSizeUInt(1,1))
    invMsg.inventories.head.hash.hex must be (BitcoinSUtil.flipEndianess("00000000b873e79784647a6c82962c70d228557d24a747ea4d1b8bbe878e1206"))
    invMsg.inventories.head.typeIdentifier must be (MsgBlock)

  }


  private def buildPeerRequest(payload: NetworkPayload): (PeerRequest,TestProbe) = {
    val networkMsg = NetworkMessage(TestNet3, payload)
    val probe = TestProbe()
    val peerRequest = PeerRequest(networkMsg,probe.ref,TestNet3)
    (peerRequest,probe)
  }

  override def afterAll = {
    peerMsgHandler ! Tcp.Close
    TestKit.shutdownActorSystem(system)
  }
}

