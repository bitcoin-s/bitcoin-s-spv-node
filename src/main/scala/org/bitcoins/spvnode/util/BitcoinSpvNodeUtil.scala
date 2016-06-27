package org.bitcoins.spvnode.util

import java.net.InetAddress

import org.bitcoins.core.util.BitcoinSUtil

/**
  * Created by chris on 6/3/16.
  */
trait BitcoinSpvNodeUtil {
  /**
    * Writes an ip address to the representation that the p2p network requires
    * An IPv6 address is in big endian byte order
    * An IPv4 address has to be mapped to an IPv6 address
    * https://en.wikipedia.org/wiki/IPv6#IPv4-mapped_IPv6_addresses
    *
    * @param iNetAddress
    * @return
    */
  def writeAddress(iNetAddress: InetAddress) : String = {
    if (iNetAddress.getAddress.size == 4) {
      //this means we need to convert the IPv4 address to an IPv6 address
      //first we have an 80 bit prefix of zeros
      val zeroBytes = for ( _ <- 0 until 10) yield 0.toByte
      //the next 16 bits are ones
      val oneBytes : Seq[Byte] = Seq(0xff.toByte,0xff.toByte)

      val prefix : Seq[Byte] = zeroBytes ++ oneBytes
      val addr = BitcoinSUtil.encodeHex(prefix) + BitcoinSUtil.encodeHex(iNetAddress.getAddress)
      addr
    } else BitcoinSUtil.encodeHex(iNetAddress.getAddress.reverse)
  }
}

object BitcoinSpvNodeUtil extends BitcoinSpvNodeUtil
