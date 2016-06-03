package org.bitcoins.spvnode.serializers.control

import org.bitcoins.core.serializers.RawBitcoinSerializer
import org.bitcoins.core.util.BitcoinSUtil
import org.bitcoins.spvnode.messages.control.ServiceIdentifier

/**
  * Created by chris on 6/2/16.
  * Responsible for serializing and deserializing the
  * service identifier in a network message
  * https://bitcoin.org/en/developer-reference#version
  */
trait RawServiceIdentifierSerializer extends RawBitcoinSerializer[ServiceIdentifier] {

  def read(bytes : List[Byte]) : ServiceIdentifier = {
    val serviceBytes = bytes.take(8)
    //since bitcoin uses big endian for numbers, we need to convert to little endian
    ServiceIdentifier(BigInt(serviceBytes.reverse.toArray))
  }

  def write(serviceIdentifier: ServiceIdentifier) : String = {
    val hex = BitcoinSUtil.encodeHex(serviceIdentifier.num.toByteArray)
    addPadding(16,hex)
  }
}

object RawServiceIdentifierSerializer extends RawServiceIdentifierSerializer
