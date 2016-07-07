package org.bitcoins.spvnode.networking

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import org.bitcoins.core.config.TestNet3
import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.util.{BitcoinSLogger, BitcoinSUtil}
import org.bitcoins.spvnode.NetworkMessage
import org.bitcoins.spvnode.constant.Constants
import org.bitcoins.spvnode.messages.HeadersMessage
import org.bitcoins.spvnode.messages.data.GetHeadersMessage
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpecLike, MustMatchers}

import scala.concurrent.duration.DurationInt

/**
  * Created by chris on 7/1/16.
  */
class PeerMessageHandlerTest extends TestKit(ActorSystem("PeerMessageHandlerTest")) with FlatSpecLike with MustMatchers
  with BeforeAndAfter with BeforeAndAfterAll with BitcoinSLogger {


  "PeerMessageHandler" must "be able to send a GetHeadersMessage then receive a list of headers back" in {

    val hashtStart = DoubleSha256Digest("0000000000000000000000000000000000000000000000000000000000000000")
    val hashStop = DoubleSha256Digest(BitcoinSUtil.flipEndianess("000000006c02c8ea6e4ff69651f7fcde348fb9d557a06e6957b65552002a7820"))
    val getHeadersMessage = GetHeadersMessage(Constants.version,Seq(hashtStart),hashStop)
    val networkMsg = NetworkMessage(TestNet3, getHeadersMessage)
    val probe = TestProbe()
    val peerRequest = PeerRequest(networkMsg,probe.ref,TestNet3)
    val peerMsgHandler = PeerMessageHandler(system)



    peerMsgHandler ! peerRequest

    val headerMsg = probe.expectMsgType[HeadersMessage](10.seconds)




  }


  override def afterAll = {
    TestKit.shutdownActorSystem(system)
  }
}

