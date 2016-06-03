package org.bitcoins.spvnode.serializers.control

import org.bitcoins.core.serializers.RawBitcoinSerializer
import org.bitcoins.core.util.BitcoinSUtil
import org.bitcoins.spvnode.messages.VersionMessage
import org.bitcoins.spvnode.messages.control.ServiceIdentifier

/**
  * Created by chris on 6/2/16.
  */
trait RawVersionMessageSerializer extends RawBitcoinSerializer[VersionMessage] {

  def read(bytes : List[Byte]) : VersionMessage = {
    val time = BitcoinSUtil.toLong(bytes.take(4))
    val services = ServiceIdentifier(bytes.slice(4,12))
    val timestamp = BitcoinSUtil.toLong(bytes.slice(12,20))
    val addressReceiveServices = ServiceIdentifier(bytes.slice(20,28))
    val addressReceiveIpAddress = bytes.slice(28,42).map(_.toChar).mkString
    val addressReceivePort = BitcoinSUtil.toLong(bytes.slice(42,44))
    val addressTransServices = ServiceIdentifier(bytes.slice(44,48))
    val addressTransIpAddress = bytes.slice(48,64).map(_.toChar).mkString
    val addressTransPort = BitcoinSUtil.toLong(bytes.slice(64,66))
    val nonce = BigInt(bytes.slice(66,74).toArray)
    ???
  }

  def write(versionMessage: VersionMessage) : String = ???
}
