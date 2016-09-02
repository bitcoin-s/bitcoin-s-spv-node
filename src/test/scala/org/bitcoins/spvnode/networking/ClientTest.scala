package org.bitcoins.spvnode.networking

import java.net.{InetSocketAddress, ServerSocket}

import akka.actor.ActorSystem
import akka.io.{Inet, Tcp}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.bitcoins.core.config.TestNet3
import org.bitcoins.core.util.{BitcoinSLogger, BitcoinSUtil}
import org.bitcoins.spvnode.messages.control.VersionMessage
import org.bitcoins.spvnode.messages.{NetworkPayload, VersionMessage}
import org.bitcoins.spvnode.util.BitcoinSpvNodeUtil
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpecLike, MustMatchers}

import scala.concurrent.duration._
import scala.util.Try
/**
  * Created by chris on 6/7/16.
  */
class ClientTest extends TestKit(ActorSystem("ClientTest")) with FlatSpecLike
  with MustMatchers with ImplicitSender
  with BeforeAndAfter with BeforeAndAfterAll with BitcoinSLogger {

  "Client" must "connect to a node on the bitcoin network, " +
    "send a version message to a peer on the network and receive a version message back, then close that connection" in {
    val probe = TestProbe()

    val client = TestActorRef(Client.props,probe.ref)

    val remote = new InetSocketAddress(TestNet3.dnsSeeds(0), TestNet3.port)
    val randomPort = 23521
    //random port
    client ! Tcp.Connect(remote, Some(new InetSocketAddress(randomPort)))

    //val bound : Tcp.Bound = probe.expectMsgType[Tcp.Bound]
    val conn : Tcp.Connected = probe.expectMsgType[Tcp.Connected]

    //make sure the socket is currently bound
    Try(new ServerSocket(randomPort)).isSuccess must be (false)
    client ! Tcp.Abort
    val confirmedClosed = probe.expectMsg(Tcp.Aborted)

    //make sure the port is now available
    val boundSocket = Try(new ServerSocket(randomPort))
    boundSocket.isSuccess must be (true)

    boundSocket.get.close()

  }

  it must "bind connect to two nodes on one port" in {
    //NOTE if this test case fails it is more than likely because one of the two dns seeds
    //below is offline
    val remote1 = new InetSocketAddress(TestNet3.dnsSeeds(0), TestNet3.port)
    val remote2 = new InetSocketAddress(TestNet3.dnsSeeds(2), TestNet3.port)

    val probe1 = TestProbe()
    val probe2 = TestProbe()


    val client1 = TestActorRef(Client.props, probe1.ref)
    val client2 = TestActorRef(Client.props, probe2.ref)

    val local1 = new InetSocketAddress(TestNet3.port)
    val options = List(Inet.SO.ReuseAddress(true))
    client1 ! Tcp.Connect(remote1,Some(local1),options)


    probe1.expectMsgType[Tcp.Connected]
    client1 ! Tcp.Abort

    val local2 = new InetSocketAddress(TestNet3.port)
    client2 ! Tcp.Connect(remote2,Some(local2),options)
    probe2.expectMsgType[Tcp.Connected](5.seconds)
    client2 ! Tcp.Abort
  }

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }


}
