package org.bitcoins.spvnode.serializers.headers

import org.bitcoins.core.util.BitcoinSUtil
import org.scalatest.{FlatSpec, MustMatchers}

/**
  * Created by chris on 5/31/16.
  */
class RawMessageHeaderSerializerTest extends FlatSpec with MustMatchers {
  val hex = "f9beb4d976657261636b000000000000000000005df6e0e2"
  "RawMessageHeaderSerializer" must "read hex string into a message header" in {
    //this example is from this section in the bitcoin developer reference
    //https://bitcoin.org/en/developer-reference#message-headers

    val messageHeader = RawMessageHeaderSerializer.read(hex)
    //this is the mainnet id
    BitcoinSUtil.encodeHex(messageHeader.network) must be ("f9beb4d9")

    messageHeader.commandName must be ("verack")

    messageHeader.payloadSize must be (0)

    BitcoinSUtil.encodeHex(messageHeader.checksum) must be ("5df6e0e2")
  }

  it must "write an object that was just read and get the original input" in {
    val messageHeader = RawMessageHeaderSerializer.read(hex)
    messageHeader.hex must be (hex)
  }


}
