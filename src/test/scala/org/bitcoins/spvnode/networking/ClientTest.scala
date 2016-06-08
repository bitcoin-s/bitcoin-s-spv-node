package org.bitcoins.spvnode.networking

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.io.Tcp
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import org.bitcoins.core.config.{NetworkParameters, TestNet3}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpecLike, MustMatchers}

import scala.concurrent.duration._
/**
  * Created by chris on 6/7/16.
  */
class ClientTest extends TestKit(ActorSystem("ClientTest")) with FlatSpecLike with MustMatchers
  with BeforeAndAfter with BeforeAndAfterAll {


  "Client" must "connect to a node on the bitcoin network, then disconnect from that node" in {
    val probe = TestProbe()
    val socket = createSocket(TestNet3)
    val client = TestActorRef(Client(socket, probe.ref,system))
    probe.expectMsgType[Tcp.Connected]
    //send this message to our peer to let them know we are closing the connection
    client ! Tcp.ConfirmedClose
    probe.expectMsg(10.seconds, Tcp.ConfirmedClose)
    //this is acknowledgement from the peer that they have closed their connection
    probe.expectMsg(10.seconds, Tcp.ConfirmedClosed)
  }


  private def createSocket(network : NetworkParameters) : InetSocketAddress = {
    new InetSocketAddress(network.dnsSeeds.head, network.port)
  }
  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }
}
