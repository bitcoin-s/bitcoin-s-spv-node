package org.bitcoins.spvnode.messages.data

import org.bitcoins.spvnode.gen.DataMessageGenerator
import org.bitcoins.spvnode.messages.MerkleBlockMessage
import org.scalacheck.{Prop, Properties}

/**
  * Created by chris on 8/26/16.
  */
class MerkleBlockMessageSpec extends Properties("MerkleBlockMessageSpec") {
  property("serialization symmetry") =
    Prop.forAll(DataMessageGenerator.merkleBlockMessage) {
      case merkleBlockMsg: MerkleBlockMessage =>
        MerkleBlockMessage(merkleBlockMsg.hex) == merkleBlockMsg
    }
}
