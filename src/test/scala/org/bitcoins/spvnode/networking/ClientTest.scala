package org.bitcoins.spvnode.networking

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.io.Tcp
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import akka.util.ByteString
import org.bitcoins.core.config.{NetworkParameters, TestNet3}
import org.bitcoins.core.protocol.blockchain.BlockHeader
import org.bitcoins.core.util.BitcoinSLogger
import org.bitcoins.spvnode.NetworkMessage
import org.bitcoins.spvnode.headers.NetworkHeader
import org.bitcoins.spvnode.messages.VersionMessage
import org.bitcoins.spvnode.messages.control.VersionMessage
import org.bitcoins.spvnode.versions.ProtocolVersion70002
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpecLike, MustMatchers}

import scala.concurrent.duration._
/**
  * Created by chris on 6/7/16.
  */
class ClientTest extends TestKit(ActorSystem("ClientTest")) with FlatSpecLike with MustMatchers
  with BeforeAndAfter with BeforeAndAfterAll with BitcoinSLogger {


  "Client" must "connect to a node on the bitcoin network, then disconnect from that node" in {
/*    val probe = TestProbe()
    val client = TestActorRef(Client(TestNet3, probe.ref, system))
    probe.expectMsgType[Tcp.Connected]
    //send this message to our peer to let them know we are closing the connection
    client ! Tcp.ConfirmedClose
    probe.expectMsg(2.seconds, Tcp.ConfirmedClose)
    //this is acknowledgement from the peer that they have closed their connection
    probe.expectMsg(5.seconds, Tcp.ConfirmedClosed)*/
  }

  it must "send a version message to a peer on the network and receive a version message back" in {
    val probe = TestProbe()
    //val socket = createSocket(TestNet3)
    val client = Client(TestNet3, probe.ref, system)

    val conn : Tcp.Connected = probe.expectMsgType[Tcp.Connected]

    val versionMessage = VersionMessage(TestNet3, conn.remoteAddress.getAddress, conn.localAddress.getAddress)
    val networkMessage = NetworkMessage(TestNet3,versionMessage)
    client ! networkMessage
    val receivedMsg = probe.expectMsgType[Tcp.Received](5.seconds)
    //val receivedMsg2 = probe.expectMsgType[Tcp.Received](5.seconds)

/*    val header = NetworkHeader(receivedMsg.toList.take(80))
    val peerVersionMessage = VersionMessage(receivedMsg.toList.slice(80,receivedMsg.toList.size))
    logger.debug("Peer header: " + header)
    logger.debug("Peer version message: " + peerVersionMessage)

    peerVersionMessage.userAgent must be ("/Satoshi:0.11.2/")
    peerVersionMessage.version must be (ProtocolVersion70002)


    peerVersionMessage.relay must be (true)*/

  }


  private def createSocket(network : NetworkParameters) : InetSocketAddress = {
    new InetSocketAddress(network.dnsSeeds.head, network.port)
  }
  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }
}
