package org.bitcoins.spvnode

import java.net.{InetAddress, InetSocketAddress}

import org.bitcoins.core.crypto.{DoubleSha256Digest, Sha256Hash160Digest}
import org.bitcoins.core.number.UInt32
import org.bitcoins.core.util.BitcoinSUtil
import org.bitcoins.spvnode.bloom.{BloomFilter, BloomUpdateAll}
import org.bitcoins.spvnode.constant.Constants
import org.bitcoins.spvnode.messages.control.FilterLoadMessage
import org.bitcoins.spvnode.networking.{PaymentActor, PeerMessageHandler}

/**
  * Created by chris on 8/29/16.
  */
object Main extends App {


  override def main(args : Array[String]) = {
    val pubKeyHash = Sha256Hash160Digest("415a05d63df2c212e1c750a70eba49d6d8af196d")
    val paymentActor = PaymentActor(Constants.actorSystem)
    paymentActor ! pubKeyHash
  }
}
