package org.bitcoins.spvnode.networking

import java.net.InetSocketAddress

import akka.actor.{ActorRef, ActorSystem}
import akka.io.Tcp
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.bitcoins.core.config.TestNet3
import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.number.UInt64
import org.bitcoins.core.protocol.CompactSizeUInt
import org.bitcoins.core.util.{BitcoinSLogger, BitcoinSUtil}
import org.bitcoins.spvnode.NetworkMessage
import org.bitcoins.spvnode.constant.Constants
import org.bitcoins.spvnode.messages._
import org.bitcoins.spvnode.messages.control.PingMessage
import org.bitcoins.spvnode.messages.data.{GetBlocksMessage, GetDataMessage, GetHeadersMessage, Inventory}
import org.bitcoins.spvnode.util.BitcoinSpvNodeUtil
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpecLike, MustMatchers}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

/**
  * Created by chris on 7/1/16.
  */
class PeerMessageHandlerTest extends TestKit(ActorSystem("PeerMessageHandlerTest"))
  with FlatSpecLike with MustMatchers with ImplicitSender
  with BeforeAndAfter with BeforeAndAfterAll with BitcoinSLogger {

  def peerMsgHandlerRef: (ActorRef, TestProbe) = {
    val probe = TestProbe("TestProbe" + BitcoinSpvNodeUtil.createActorName(this.getClass))
    (TestActorRef(PeerMessageHandler.props,probe.ref,
      BitcoinSpvNodeUtil.createActorName(PeerMessageHandler.getClass)),probe)
  }

  "PeerMessageHandler" must "be able to send a GetHeadersMessage then receive a list of headers back" in {

    val hashStart = DoubleSha256Digest("0000000000000000000000000000000000000000000000000000000000000000")
    //this is the hash of block 2, so this test will send two blocks
    val hashStop = DoubleSha256Digest(BitcoinSUtil.flipEndianness("000000006c02c8ea6e4ff69651f7fcde348fb9d557a06e6957b65552002a7820"))
    val getHeadersMessage = GetHeadersMessage(Constants.version,Seq(hashStart),hashStop)

    val (peerMsgHandler,probe) = peerMsgHandlerRef

    probe.send(peerMsgHandler, getHeadersMessage)

    val headersMsg = probe.expectMsgType[HeadersMessage](10.seconds)
    headersMsg.commandName must be (NetworkPayload.headersCommandName)
    val firstHeader = headersMsg.headers.head
    firstHeader.hash.hex must be (BitcoinSUtil.flipEndianness("00000000b873e79784647a6c82962c70d228557d24a747ea4d1b8bbe878e1206"))

    val secondHeader = headersMsg.headers(1)
    secondHeader.hash.hex must be (BitcoinSUtil.flipEndianness("000000006c02c8ea6e4ff69651f7fcde348fb9d557a06e6957b65552002a7820"))
    peerMsgHandler ! Tcp.Close

    probe.expectMsg(Tcp.Closed)

  }

  it must "send a getblocks message and receive a list of blocks back" in {
    val hashStart = DoubleSha256Digest("0000000000000000000000000000000000000000000000000000000000000000")
    //this is the hash of block 2, so this test will send two blocks
    val hashStop = DoubleSha256Digest(BitcoinSUtil.flipEndianness("000000006c02c8ea6e4ff69651f7fcde348fb9d557a06e6957b65552002a7820"))

    val getBlocksMsg = GetBlocksMessage(Constants.version,Seq(hashStart),hashStop)

    val peerRequest = buildPeerRequest(getBlocksMsg)

    val (peerMsgHandler,probe) = peerMsgHandlerRef
    probe.send(peerMsgHandler,peerRequest)

    val invMsg = probe.expectMsgType[InventoryMessage](5.seconds)

    invMsg.inventoryCount must be (CompactSizeUInt(UInt64.one,1))
    invMsg.inventories.head.hash.hex must be (BitcoinSUtil.flipEndianness("00000000b873e79784647a6c82962c70d228557d24a747ea4d1b8bbe878e1206"))
    invMsg.inventories.head.typeIdentifier must be (MsgBlock)
    peerMsgHandler ! Tcp.Close
    probe.expectMsg(Tcp.Closed)
  }

  it must "request a full block from another node" in {
    //first block on testnet
    //https://tbtc.blockr.io/block/info/1
    val blockHash = DoubleSha256Digest(BitcoinSUtil.flipEndianness("00000000b873e79784647a6c82962c70d228557d24a747ea4d1b8bbe878e1206"))
    val getDataMessage = GetDataMessage(Inventory(MsgBlock, blockHash))
    val peerRequest = buildPeerRequest(getDataMessage)
    val (peerMsgHandler,probe) = peerMsgHandlerRef
    probe.send(peerMsgHandler,peerRequest)

    val blockMsg = probe.expectMsgType[BlockMessage](5.seconds)
    logger.debug("BlockMsg: " + blockMsg)
    blockMsg.block.blockHeader.hash must be (blockHash)

    blockMsg.block.transactions.length must be (1)
    blockMsg.block.transactions.head.txId must be
    (DoubleSha256Digest(BitcoinSUtil.flipEndianness("f0315ffc38709d70ad5647e22048358dd3745f3ce3874223c80a7c92fab0c8ba")))
    peerMsgHandler ! Tcp.Close
    probe.expectMsg(Tcp.Closed)

  }

  it must "request a transaction from another node" in {
    //this tx is the coinbase tx in the first block on testnet
    //https://tbtc.blockr.io/tx/info/f0315ffc38709d70ad5647e22048358dd3745f3ce3874223c80a7c92fab0c8ba
    val txId = DoubleSha256Digest(BitcoinSUtil.flipEndianness("a4dd00d23de4f0f96963e16b72afea547bc9ad1d0c1dda5653110eddd83fe0e2"))
    val getDataMessage = GetDataMessage(Inventory(MsgTx, txId))
    val peerRequest = buildPeerRequest(getDataMessage)
    val (peerMsgHandler,probe) = peerMsgHandlerRef
    probe.send(peerMsgHandler,peerRequest)
    //we cannot request an arbitrary tx from a node,
    //therefore the node responds with a [[NotFoundMessage]]
    probe.expectMsgType[NotFoundMessage](5.seconds)

    peerMsgHandler ! Tcp.Close
    probe.expectMsg(Tcp.Closed)
  }

  it must "send a GetAddressMessage and then receive an AddressMessage back" in {
    val (peerMsgHandler,probe) = peerMsgHandlerRef
    val peerRequest = buildPeerRequest(GetAddrMessage)
    probe.send(peerMsgHandler,peerRequest)
    val addrMsg = probe.expectMsgType[AddrMessage](15.seconds)
    peerMsgHandler ! Tcp.Close
    probe.expectMsg(Tcp.Closed)
  }

  it must "send a PingMessage to our peer and receive a PongMessage back" in {
    val (peerMsgHandler,probe) = peerMsgHandlerRef
    val nonce = UInt64(scala.util.Random.nextLong.abs)

    val peerRequest = buildPeerRequest(PingMessage(nonce))

    system.scheduler.schedule(2.seconds,30.seconds,peerMsgHandler,peerRequest)(global,probe.ref)
    val pongMessage = probe.expectMsgType[PongMessage](8.seconds)

    pongMessage.nonce must be (nonce)

    peerMsgHandler ! Tcp.Close
    probe.expectMsg(Tcp.Closed)
  }

  private def buildPeerRequest(payload: NetworkPayload): NetworkMessage = NetworkMessage(Constants.networkParameters, payload)


  override def afterAll = {
    TestKit.shutdownActorSystem(system)
  }
}

