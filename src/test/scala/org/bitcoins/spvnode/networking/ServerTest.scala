package org.bitcoins.spvnode.networking

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import akka.io.Tcp
import akka.testkit.TestKit
import org.bitcoins.core.config.TestNet3
import org.scalatest.{FlatSpecLike, MustMatchers}

import scala.util.Try

/**
  * Created by chris on 6/7/16.
  */
class ServerTest extends TestKit(ActorSystem("ServerTest")) with FlatSpecLike with MustMatchers  {
  "Server" must "bind a tcp server to an address on our machine" in {
    //if this fails this means that the port is in use before our test case is run
    val port = TestNet3.port
    isBound(port) must be (false)
    val actor = Server()
    actor ! Tcp.Bind(actor, new InetSocketAddress(port))
    Thread.sleep(250)
    isBound(port) must be (true)
    Thread.sleep(250)
    actor ! Tcp.Unbind
    Thread.sleep(250)
    isBound(port) must be (false)

  }


  /**
    * Tests if a specific port number is bound on our machine
    * @param port
    * @return
    */
  def isBound(port : Int) : Boolean = {
    val tryBinding : Try[Unit] = Try {
      val socket = new java.net.Socket()
      socket.connect(new java.net.InetSocketAddress(port),1000)
      socket.close()
    }

    tryBinding.isSuccess
  }
}
