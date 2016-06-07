package org.bitcoins.spvnode.networking

import java.net.InetSocketAddress

import akka.io.Tcp
import org.bitcoins.core.config.TestNet3
import org.scalatest.{FlatSpec, MustMatchers}

import scala.util.Try

/**
  * Created by chris on 6/7/16.
  */
class ServerTest extends FlatSpec with MustMatchers {
  "Server" must "bind a tcp server to an address on our machine" in {
    //if this fails this means that the port is in use before our test case is run
    isBound(TestNet3.port) must be (false)
    val actor = Server()
    actor ! Tcp.Bind(actor, new InetSocketAddress(TestNet3.port))
    Thread.sleep(1000)
    isBound(TestNet3.port) must be (true)
    Thread.sleep(1000)
    actor ! Tcp.Unbind
    Thread.sleep(1000)
    isBound(TestNet3.port) must be (false)
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
