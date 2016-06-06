package org.bitcoins.spvnode.serializers.control

import java.net.InetAddress

import org.bitcoins.core.serializers.RawBitcoinSerializer
import org.bitcoins.core.util.{BitcoinSLogger, BitcoinSUtil}
import org.bitcoins.spvnode.messages.control.ServiceIdentifier
import org.bitcoins.spvnode.util.{BitcoinSpvNodeUtil, NetworkIpAddress}

/**
  * Created by chris on 6/2/16.
  * Responsible for serializing and deserializing network ip address objects on the p2p network
  * https://bitcoin.org/en/developer-reference#addr
  */
trait RawNetworkIpAddressSerializer extends RawBitcoinSerializer[NetworkIpAddress] with BitcoinSLogger {

  def read(bytes : List[Byte]) : NetworkIpAddress = {
    val time = BitcoinSUtil.toLong(bytes.take(4))
    val services = ServiceIdentifier(bytes.slice(4,12))
    val ipBytes = bytes.slice(12,28)
    val ipAddress = InetAddress.getByAddress(ipBytes.toArray)
    val port = BitcoinSUtil.toLong(bytes.slice(28,30).reverse).toInt
    NetworkIpAddress(time,services,ipAddress,port)
  }

  def write(networkIpAddress: NetworkIpAddress) : String = {
    val time = BitcoinSUtil.longToHex(networkIpAddress.time)
    val services = networkIpAddress.services.hex
    val ipAddress = BitcoinSpvNodeUtil.writeAddress(networkIpAddress.address)
    val port = BitcoinSUtil.flipEndianess(BitcoinSUtil.longToHex(networkIpAddress.port))
    time + services + ipAddress + port
  }


}

object RawNetworkIpAddressSerializer extends RawNetworkIpAddressSerializer