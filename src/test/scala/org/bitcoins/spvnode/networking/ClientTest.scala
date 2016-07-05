package org.bitcoins.spvnode.networking

import akka.actor.ActorSystem
import akka.io.Tcp
import akka.testkit.{TestKit, TestProbe}
import org.bitcoins.core.config.TestNet3
import org.bitcoins.core.util.{BitcoinSLogger, BitcoinSUtil}
import org.bitcoins.spvnode.messages.control.VersionMessage
import org.bitcoins.spvnode.messages.{NetworkPayload, VersionMessage}
import org.bitcoins.spvnode.util.BitcoinSpvNodeUtil
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpecLike, MustMatchers}

import scala.concurrent.duration._
/**
  * Created by chris on 6/7/16.
  */
class ClientTest extends TestKit(ActorSystem("ClientTest")) with FlatSpecLike with MustMatchers
  with BeforeAndAfter with BeforeAndAfterAll with BitcoinSLogger {

  "Client" must "connect to a node on the bitcoin network, " +
    "send a version message to a peer on the network and receive a version message back, then close that connection" in {
    val probe = TestProbe()
    val client = Client(TestNet3, probe.ref)

    //val bound : Tcp.Bound = probe.expectMsgType[Tcp.Bound]
    val conn : Tcp.Connected = probe.expectMsgType[Tcp.Connected]

    val versionMessage = VersionMessage(TestNet3, conn.localAddress.getAddress,conn.remoteAddress.getAddress)
    client ! versionMessage
    val receivedMsg = probe.expectMsgType[Tcp.Received](5.seconds)
    logger.debug("ReceivedMsg: " + BitcoinSUtil.encodeHex(receivedMsg.data.toArray))
    val bytes = receivedMsg.data.toArray
    val (messages,_) = BitcoinSpvNodeUtil.parseIndividualMessages(bytes)

    val peerVersionMessage = messages.head
    logger.debug("Peer header: " + peerVersionMessage.header)
    logger.debug("Peer version message: " + peerVersionMessage)

    peerVersionMessage.payload match {
      case version : VersionMessage =>
        version.userAgent.contains("Satoshi") must be (true)
      case _ : NetworkPayload => throw new IllegalArgumentException("Must be a version message")
    }

    val verackMessage = messages(1)
    verackMessage.header.commandName must be (NetworkPayload.verAckCommandName)

  }


  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }


}
