package org.bitcoins.spvnode.messages

import org.bitcoins.spvnode.gen.ControlMessageGenerator
import org.bitcoins.spvnode.messages.control.PingMessage
import org.scalacheck.{Prop, Properties}

/**
  * Created by chris on 6/29/16.
  */
class PingMessageSpec extends Properties("PingMessageSpec") {

  property("Symmetry serialization") =
    Prop.forAll(ControlMessageGenerator.pingMessage) { pingMessage =>
      PingMessage(pingMessage.hex) == pingMessage
    }
}
