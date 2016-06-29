package org.bitcoins.spvnode.util

import java.net.InetAddress

import org.bitcoins.core.util.{BitcoinSLogger, BitcoinSUtil}
import org.bitcoins.spvnode.NetworkMessage

import scala.annotation.tailrec

/**
  * Created by chris on 6/3/16.
  */
trait BitcoinSpvNodeUtil extends BitcoinSLogger {
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
    } else BitcoinSUtil.encodeHex(iNetAddress.getAddress)
  }

  /**
    * Akka sends messages as one byte stream. There is not a 1 to 1 relationship between byte streams received and
    * bitcoin protocol messages. This function parses our byte stream into individual network messages
    * @param bytes the bytes that need to be parsed into individual messages
    * @return the parsed [[NetworkMessage]]'s
    */
  def parseIndividualMessages(bytes: Seq[Byte]): Seq[NetworkMessage] = {
    @tailrec
    def loop(remainingBytes : Seq[Byte], accum : Seq[NetworkMessage]): Seq[NetworkMessage] = {
      if (remainingBytes.length <= 0) accum
      else {
        val message = NetworkMessage(remainingBytes)
        logger.debug("Parsed network message: " + message)
        val newRemainingBytes = remainingBytes.slice(message.bytes.length, remainingBytes.length)
        logger.debug("Command names accum: " + accum.map(_.header.commandName))
        logger.debug("New Remaining bytes: " + BitcoinSUtil.encodeHex(newRemainingBytes))
        loop(newRemainingBytes, message +: accum)
      }
    }
    val messages = loop(bytes, Seq()).reverse
    logger.debug("Parsed messages: " + messages)
    messages
  }
}

object BitcoinSpvNodeUtil extends BitcoinSpvNodeUtil
