package org.bitcoins.spvnode.networking

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import org.bitcoins.core.config.TestNet3
import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.util.BitcoinSLogger
import org.bitcoins.spvnode.constant.Constants
import org.bitcoins.spvnode.messages.HeadersMessage
import org.bitcoins.spvnode.messages.data.GetHeadersMessage
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpecLike, MustMatchers}

/**
  * Created by chris on 7/1/16.
  */
class PeerMessageHandlerTest extends TestKit(ActorSystem("ClientTest")) with FlatSpecLike with MustMatchers
  with BeforeAndAfter with BeforeAndAfterAll with BitcoinSLogger {


  "PeerMessageHandler" must "be able to send a GetHeadersMessage then receive a list of headers back" in {
    val hashStop = DoubleSha256Digest("0000000000000000000000000000000000000000000000000000000000000000")
    val getHeadersMessage = GetHeadersMessage(Constants.version,Seq(),hashStop)
    val probe = TestProbe()
    val peerRequest = PeerRequest(getHeadersMessage,probe.ref,TestNet3)
    val peerMsgHandler = PeerMessageHandler(system)

    peerMsgHandler ! peerRequest

    val headerMsg = probe.expectMsgType[HeadersMessage]
  }

  override def afterAll = {
    TestKit.shutdownActorSystem(system)
  }
}

