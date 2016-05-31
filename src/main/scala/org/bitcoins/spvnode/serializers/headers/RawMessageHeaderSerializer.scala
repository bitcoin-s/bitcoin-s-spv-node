package org.bitcoins.spvnode.serializers.headers

import org.bitcoins.core.serializers.RawBitcoinSerializer
import org.bitcoins.spvnode.headers.MessageHeader

/**
  * Created by chris on 5/31/16.
  * Reads and writes a message header on the peer-to-peer network
  * https://bitcoin.org/en/developer-reference#message-headers
  */
trait RawMessageHeaderSerializer extends RawBitcoinSerializer[MessageHeader] {

  /**
    * Transforms a sequence of bytes into a message header
    * @param bytes the byte representation for a MessageHeader on the peer-to-peer network
    * @return the native object for the MessageHeader
    */
  def read(bytes : List[Byte]) : MessageHeader = ???

  /**
    * Takes in a message header and serializes it to hex
    * @param messageHeader the message header to be serialized
    * @return the hexadecimal representation of the message header
    */
  def write(messageHeader: MessageHeader) : String = ???

}

object RawMessageHeaderSerializer extends RawMessageHeaderSerializer
