package org.bitcoins.spvnode.messages.control

import org.bitcoins.spvnode.gen.ControlMessageGenerator
import org.bitcoins.spvnode.messages.RejectMessage
import org.scalacheck.{Prop, Properties}

/**
  * Created by chris on 8/31/16.
  */
class RejectMessageSpec extends Properties("RejectMessageSpec") {

  property("serialization symmetry") = {
    Prop.forAll(ControlMessageGenerator.rejectMessage) { case rejectMsg : RejectMessage =>
        RejectMessage(rejectMsg.hex) == rejectMsg

    }
  }
}
