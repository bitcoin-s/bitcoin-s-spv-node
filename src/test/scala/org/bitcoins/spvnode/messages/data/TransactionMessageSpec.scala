package org.bitcoins.spvnode.messages.data

import org.bitcoins.spvnode.gen.DataMessageGenerator
import org.bitcoins.spvnode.messages.TransactionMessage
import org.scalacheck.{Prop, Properties}

/**
  * Created by chris on 9/1/16.
  */
class TransactionMessageSpec extends Properties("TransactionMessageSpec") {

  property("serialization symmetry") =
    Prop.forAll(DataMessageGenerator.transactionMessage) { case txMsg: TransactionMessage =>
      TransactionMessage(txMsg.hex) == txMsg
    }
}
