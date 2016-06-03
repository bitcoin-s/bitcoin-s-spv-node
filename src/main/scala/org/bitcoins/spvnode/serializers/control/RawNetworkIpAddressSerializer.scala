package org.bitcoins.spvnode.serializers.control

import java.net.InetAddress

import org.bitcoins.core.serializers.RawBitcoinSerializer
import org.bitcoins.core.util.{BitcoinSLogger, BitcoinSUtil}
import org.bitcoins.spvnode.messages.control.ServiceIdentifier
import org.bitcoins.spvnode.util.NetworkIpAddress

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
    val ipAddress = writeAddress(networkIpAddress.address)
    val port = BitcoinSUtil.flipEndianess(BitcoinSUtil.longToHex(networkIpAddress.port))
    time + services + ipAddress + port
  }

  /**
    * Writes an ip address to the representation that the p2p network requires
    * An IPv6 address is in big endian byte order
    * An IPv4 address has to be mapped to an IPv6 address
    * https://en.wikipedia.org/wiki/IPv6#IPv4-mapped_IPv6_addresses
    * @param iNetAddress
    * @return
    */
  private def writeAddress(iNetAddress: InetAddress) : String = {
    if (iNetAddress.getAddress.size == 4) {
      //this means we need to convert the IPv4 address to an IPv6 address
      //first we have an 80 bit prefix of zeros
      val zeroBytes = for ( _ <- 0 until 10) yield 0.toByte
      //the next 16 bits are ones
      val oneBytes : Seq[Byte] = Seq(0xff.toByte,0xff.toByte)

      val prefix : Seq[Byte] = zeroBytes ++ oneBytes
      BitcoinSUtil.encodeHex(prefix) + BitcoinSUtil.encodeHex(iNetAddress.getAddress)
    } else BitcoinSUtil.encodeHex(iNetAddress.getAddress.reverse)
  }
}

object RawNetworkIpAddressSerializer extends RawNetworkIpAddressSerializer