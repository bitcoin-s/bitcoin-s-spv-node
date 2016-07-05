package org.bitcoins.spvnode.messages.control

import org.bitcoins.spvnode.gen.ControlMessageGenerator
import org.scalacheck.{Prop, Properties}

/**
  * Created by chris on 7/5/16.
  */
class PongMessageSpec extends Properties("PongMessageSpec") {

  property("Serializatoin symmetry") =
    Prop.forAll(ControlMessageGenerator.pongMessage) { pongMsg =>

      PongMessageRequest(pongMsg.hex) == pongMsg

    }
}
