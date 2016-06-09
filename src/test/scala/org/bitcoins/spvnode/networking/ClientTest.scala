package org.bitcoins.spvnode.networking

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.io.Tcp
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import org.bitcoins.core.config.{NetworkParameters, TestNet3}
import org.bitcoins.spvnode.messages.VersionMessage
import org.bitcoins.spvnode.messages.control.VersionMessage
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpecLike, MustMatchers}

import scala.concurrent.duration._
/**
  * Created by chris on 6/7/16.
  */
class ClientTest extends TestKit(ActorSystem("ClientTest")) with FlatSpecLike with MustMatchers
  with BeforeAndAfter with BeforeAndAfterAll {


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
    val client = TestActorRef(Client(TestNet3, probe.ref, system))
    val conn : Tcp.Connected = probe.expectMsgType[Tcp.Connected]

    val versionMessage = VersionMessage(TestNet3, conn.localAddress.getAddress, conn.remoteAddress.getAddress)
    client ! versionMessage
    probe.expectMsgType[VersionMessage](10.seconds)
  }


  private def createSocket(network : NetworkParameters) : InetSocketAddress = {
    new InetSocketAddress(network.dnsSeeds.head, network.port)
  }
  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }
}
