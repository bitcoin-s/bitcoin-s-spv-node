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

/*
    privKey: org.bitcoins.core.crypto.ECPrivateKey = ECPrivateKey(764c806e677bd60533fb8ab979e2bb928de41e229b03eae8486f605ae8b801a6,true)

    scala> val pubKey = privKey.publicKey
    pubKey: org.bitcoins.core.crypto.ECPublicKey = ECPublicKey(0338d4e90fe8087c9387313b0ab5ef0affd09eeda45e88529f91b4598448de0167)


    */

    val pubKeyHash = Sha256Hash160Digest("415a05d63df2c212e1c750a70eba49d6d8af196d")
    val paymentActor = PaymentActor(Constants.actorSystem)
    paymentActor ! pubKeyHash

/*    val ipAddr = InetAddress.getByName("173.31.39.168")
    val seed = new InetSocketAddress(Constants.networkParameters.dnsSeeds.head,Constants.networkParameters.port)
    val peerMsgHandler = PeerMessageHandler(Constants.actorSystem,seed)


    val bloomFilter = BloomFilter(10,0.01,UInt32.zero,BloomUpdateAll).insert(pubKeyHash)
    val filterLoadMsg = FilterLoadMessage(bloomFilter)
    val networkMsg = NetworkMessage(Constants.networkParameters,filterLoadMsg)
    peerMsgHandler ! networkMsg*/
  }
}
