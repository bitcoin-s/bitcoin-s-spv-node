package org.bitcoins.spvnode.messages.data

import org.bitcoins.spvnode.gen.DataMessageGenerator
import org.scalacheck.{Prop, Properties}

/**
  * Created by chris on 7/8/16.
  */
class InventoryMessageSpec extends Properties("InventoryMessageSpec") {

  property("Serialization symmetry") =
    Prop.forAll(DataMessageGenerator.inventoryMessages) { invMessage =>
      InventoryMessage(invMessage.hex) == invMessage
    }
}
