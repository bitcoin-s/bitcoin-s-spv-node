package org.bitcoins.spvnode.serializers.messages.control

import org.bitcoins.core.protocol.CompactSizeUInt
import org.bitcoins.core.serializers.RawBitcoinSerializer
import org.bitcoins.core.util.BitcoinSUtil
import org.bitcoins.spvnode.messages.AddrMessage
import org.bitcoins.spvnode.messages.control.AddrMessage
import org.bitcoins.spvnode.util.NetworkIpAddress

import scala.annotation.tailrec

/**
  * Created by chris on 6/3/16.
  * Responsible for the serialization and deserialization of AddrMessages
  * https://bitcoin.org/en/developer-reference#addr
  */
trait RawAddrMessageSerializer extends RawBitcoinSerializer[AddrMessage] {

  def read(bytes : List[Byte]) : AddrMessage = {
    val ipCount = CompactSizeUInt.parseCompactSizeUInt(bytes)
    val ipAddressBytes = bytes.slice(ipCount.size.toInt, bytes.size)
    val (networkIpAddresses, remainingBytes) = parseNetworkIpAddresses(ipCount, ipAddressBytes)
    AddrMessage(ipCount, networkIpAddresses)
  }

  def write(addrMessage: AddrMessage) : String = {
    addrMessage.ipCount.hex + addrMessage.addresses.map(_.hex).mkString
  }


  /**
    * Parses ip addresses inside of an AddrMessage
    * @param ipCount the number of ip addresses we need to parse from the AddrMessage
    * @param bytes the bytes from which we need to parse the ip addresses
    * @return the parsed ip addresses and the remaining bytes
    */
  private def parseNetworkIpAddresses(ipCount : CompactSizeUInt, bytes : Seq[Byte]) : (Seq[NetworkIpAddress], Seq[Byte]) = {
    @tailrec
    def loop(remainingAddresses : BigInt, remainingBytes : Seq[Byte], accum : List[NetworkIpAddress]) : (Seq[NetworkIpAddress], Seq[Byte]) = {
      if (remainingAddresses <= 0) (accum.reverse, remainingBytes)
      else {
        val networkIpAddress = RawNetworkIpAddressSerializer.read(remainingBytes)
        val newRemainingBytes = remainingBytes.slice(networkIpAddress.size, remainingBytes.size)
        loop(remainingAddresses - 1, newRemainingBytes, networkIpAddress :: accum)
      }
    }
    loop(ipCount.num.toInt,bytes, List())
  }
}

object RawAddrMessageSerializer extends RawAddrMessageSerializer
