package org.bitcoins.spvnode.networking

import java.net.InetSocketAddress

import akka.actor.{ActorSystem, Props}
import akka.io.Tcp
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.bitcoins.core.config.TestNet3
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, MustMatchers}

import scala.concurrent.duration._
/**
  * Created by chris on 6/7/16.
  */
class ClientTest extends TestKit(ActorSystem("ClientTest")) with FlatSpecLike with MustMatchers with BeforeAndAfterAll {

  "Client" must "connect to a node on the bitcoin network" in {
    val probe = TestProbe()
    val hostName = TestNet3.dnsSeeds.head
    val socket = new InetSocketAddress(hostName, TestNet3.port)
    val client = TestActorRef(Client(socket, probe.ref,system))
    client ! Tcp.Connect(socket)
    probe.expectMsgType[Tcp.Connected](10.seconds)
  }



  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }
}
