package org.bitcoins.spvnode.networking

import akka.actor.ActorSystem
import akka.io.Tcp
import akka.testkit.{TestKit, TestProbe}
import org.bitcoins.core.config.TestNet3
import org.bitcoins.core.util.{BitcoinSLogger, BitcoinSUtil}
import org.bitcoins.spvnode.NetworkMessage
import org.bitcoins.spvnode.headers.NetworkHeader
import org.bitcoins.spvnode.messages.NetworkPayload
import org.bitcoins.spvnode.messages.control.VersionMessage
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
    val client = Client(TestNet3, probe.ref, system)

    val conn : Tcp.Connected = probe.expectMsgType[Tcp.Connected]

    val versionMessage = VersionMessage(TestNet3, conn.localAddress.getAddress,conn.remoteAddress.getAddress)
    val networkMessage = NetworkMessage(TestNet3, versionMessage)
    client ! networkMessage
    val receivedMsg = probe.expectMsgType[Tcp.Received](5.seconds)
/*
    val header = NetworkHeader(receivedMsg.data.toList.take(24))
    val peerVersionMessage = VersionMessage(receivedMsg.data.toList.slice(24,receivedMsg.data.toList.size))
    logger.debug("Peer header: " + header)
    logger.debug("Peer version message: " + peerVersionMessage)
    peerVersionMessage.userAgent.contains("Satoshi") must be (true)

    val verackMessage = probe.expectMsgType[Tcp.Received](2.seconds)
    logger.debug("Verack message: " + BitcoinSUtil.encodeHex(verackMessage.data.toArray))
    val verack = NetworkHeader(verackMessage.data.toArray)
    verack.commandName must be (NetworkPayload.verAckCommandName)*/
/*    client ! Tcp.ConfirmedClose
    probe.expectMsg(2.seconds, Tcp.ConfirmedClose)
    //this is acknowledgement from the peer that they have closed their connection
    probe.expectMsg(5.seconds, Tcp.ConfirmedClosed)*/

  }


  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }
}
