package org.bitcoins.spvnode

import org.bitcoins.core.config.TestNet3
import org.bitcoins.core.crypto.Sha256Hash160Digest
import org.bitcoins.core.protocol.{BitcoinAddress, P2PKHAddress}
import org.bitcoins.spvnode.constant.Constants
import org.bitcoins.spvnode.networking.PaymentActor

/**
  * Created by chris on 8/29/16.
  */
object Main extends App {


  override def main(args : Array[String]) = {
    val pubKeyHash = Sha256Hash160Digest("415a05d63df2c212e1c750a70eba49d6d8af196d")
    //address is mmUW4R8SKtRA2uEKhiw5m3DsUYV76bsMZ9
    val address = BitcoinAddress("mmUW4R8SKtRA2uEKhiw5m3DsUYV76bsMZ9")
    val paymentActor = PaymentActor(Constants.actorSystem)
    paymentActor ! address
  }
}
