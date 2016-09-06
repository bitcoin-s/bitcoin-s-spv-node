package org.bitcoins.spvnode.messages.data

import org.bitcoins.spvnode.gen.DataMessageGenerator
import org.scalacheck.{Prop, Properties}

/**
  * Created by chris on 9/6/16.
  */
class HeadersMessageSpec extends Properties("HeadersMessageSpec") {

  property("serialization symmetry") =
    Prop.forAll(DataMessageGenerator.headersMessage) { headersMsg =>
      HeadersMessage(headersMsg.hex) == headersMsg
    }
}
