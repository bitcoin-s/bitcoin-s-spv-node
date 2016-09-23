package org.bitcoins.spvnode.serializers.messages.control

import java.net.InetAddress

import org.bitcoins.core.number.UInt32
import org.bitcoins.core.serializers.RawBitcoinSerializer
import org.bitcoins.core.util.{BitcoinSLogger, BitcoinSUtil, NumberUtil}
import org.bitcoins.spvnode.messages.control.ServiceIdentifier
import org.bitcoins.spvnode.util.{BitcoinSpvNodeUtil, NetworkIpAddress}

/**
  * Created by chris on 6/2/16.
  * Responsible for serializing and deserializing network ip address objects on the p2p network
  * https://bitcoin.org/en/developer-reference#addr
  */
trait RawNetworkIpAddressSerializer extends RawBitcoinSerializer[NetworkIpAddress] with BitcoinSLogger {

  def read(bytes : List[Byte]) : NetworkIpAddress = {
    val time = UInt32(bytes.take(4).reverse)
    val services = ServiceIdentifier(bytes.slice(4,12))
    val ipBytes = bytes.slice(12,28)
    val ipAddress = InetAddress.getByAddress(ipBytes.toArray)
    val port = NumberUtil.toLong(bytes.slice(28,30)).toInt
    NetworkIpAddress(time,services,ipAddress,port)
  }

  def write(networkIpAddress: NetworkIpAddress) : String = {
    val time = BitcoinSUtil.flipEndianness(networkIpAddress.time.bytes)
    val services = networkIpAddress.services.hex
    val ipAddress = BitcoinSpvNodeUtil.writeAddress(networkIpAddress.address)
    //uint16s are only 4 hex characters
    val port = BitcoinSUtil.encodeHex(networkIpAddress.port).slice(4,8)
    time + services + ipAddress + port
  }


}

object RawNetworkIpAddressSerializer extends RawNetworkIpAddressSerializer