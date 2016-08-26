package org.bitcoins.spvnode.messages.control

import org.bitcoins.spvnode.gen.ControlMessageGenerator
import org.bitcoins.spvnode.messages.FilterAddMessage
import org.scalacheck.{Prop, Properties}

/**
  * Created by chris on 8/26/16.
  */
class FilterAddMessageSpec extends Properties("FilterAddMessageSpec") {

  property("Serialization symmetry") =
    Prop.forAll(ControlMessageGenerator.filterAddMessage) { case filterAddMsg: FilterAddMessage =>
        FilterAddMessage(filterAddMsg.hex) == filterAddMsg
    }
}
