package org.bitcoins.spvnode.messages

import org.scalatest.{FlatSpec, MustMatchers}

/**
  * Created by chris on 6/10/16.
  */
class NetworkPayloadTest extends FlatSpec with MustMatchers {

  "NetworkMessage" must "correctly pad the command name" in {
    //all commandNames must be 12 bytes in size
    val msg : NetworkPayload = new GetBlocksMessage {
      override def hex: String = ""
      def blockHeaderHashes: Seq[org.bitcoins.core.crypto.DoubleSha256Digest] = ???
      def hashCount: org.bitcoins.core.protocol.CompactSizeUInt = ???
      def protocolVersion: org.bitcoins.spvnode.versions.ProtocolVersion = ???
      def stopHash: org.bitcoins.core.crypto.DoubleSha256Digest = ???

    }

    msg.commandName.length must be (12)
  }
}
