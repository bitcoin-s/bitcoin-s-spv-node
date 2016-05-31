package org.bitcoins.spvnode.serializers.headers

import org.bitcoins.core.serializers.RawBitcoinSerializer
import org.bitcoins.core.util.{BitcoinSLogger, BitcoinSUtil}
import org.bitcoins.spvnode.headers.MessageHeader

/**
  * Created by chris on 5/31/16.
  * Reads and writes a message header on the peer-to-peer network
  * https://bitcoin.org/en/developer-reference#message-headers
  */
trait RawMessageHeaderSerializer extends RawBitcoinSerializer[MessageHeader] with BitcoinSLogger {

  /**
    * Transforms a sequence of bytes into a message header
    * @param bytes the byte representation for a MessageHeader on the peer-to-peer network
    * @return the native object for the MessageHeader
    */
  def read(bytes : List[Byte]) : MessageHeader = {
    val network = bytes.take(4)
    //.trim removes the null characters appended to the command name
    val commandName = bytes.slice(4,16).map(_.toChar).mkString.trim
    val payloadSize = BitcoinSUtil.toLong(bytes.slice(16,20))
    val checksum = bytes.slice(20,24)
    MessageHeader(network,commandName,payloadSize,checksum)
  }

  /**
    * Takes in a message header and serializes it to hex
    * @param messageHeader the message header to be serialized
    * @return the hexadecimal representation of the message header
    */
  def write(messageHeader: MessageHeader) : String = {
    val network = BitcoinSUtil.encodeHex(messageHeader.network)
    val commandNameNoPadding = BitcoinSUtil.encodeHex(messageHeader.commandName.map(_.toByte))
    //command name needs to be 12 bytes in size, or 24 chars in hex
    val commandName = addPadding(24, commandNameNoPadding)
    val payloadSizeNoPadding = BitcoinSUtil.longToHex(messageHeader.payloadSize)
    //payload size needs to be 4 bytes, or 8 chars in hex
    val payloadSize = addPadding(8, payloadSizeNoPadding)
    val checksum = BitcoinSUtil.encodeHex(messageHeader.checksum)
    network + commandName + payloadSize + checksum
  }

}

object RawMessageHeaderSerializer extends RawMessageHeaderSerializer
