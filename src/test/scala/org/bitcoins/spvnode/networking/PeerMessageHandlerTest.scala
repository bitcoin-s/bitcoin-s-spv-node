package org.bitcoins.spvnode.networking

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.bitcoins.core.util.BitcoinSLogger
import org.bitcoins.spvnode.messages.data.GetHeadersMessage
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpecLike, MustMatchers}

/**
  * Created by chris on 7/1/16.
  */
class PeerMessageHandlerTest extends TestKit(ActorSystem("ClientTest")) with FlatSpecLike with MustMatchers
  with BeforeAndAfter with BeforeAndAfterAll with BitcoinSLogger  {


  "PeerMessageHandler" must "be able to send a GetHeadersMessage then receive a list of headers back" in {
    //val getHeadersMessage = GetHeadersMessage()
  }
}
