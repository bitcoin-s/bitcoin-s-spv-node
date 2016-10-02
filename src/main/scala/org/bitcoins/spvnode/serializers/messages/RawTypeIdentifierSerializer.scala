package org.bitcoins.spvnode.serializers.messages

import org.bitcoins.core.number.UInt32
import org.bitcoins.core.serializers.RawBitcoinSerializer
import org.bitcoins.core.util.BitcoinSUtil
import org.bitcoins.spvnode.messages.TypeIdentifier

/**
  * Created by chris on 5/31/16.
  * Reads and writes a type identifier on a peer-to-peer network
  * https://bitcoin.org/en/developer-reference#data-messages
  */
trait RawTypeIdentifierSerializer extends RawBitcoinSerializer[TypeIdentifier] {

  override def read(bytes: List[Byte]): TypeIdentifier = {
    TypeIdentifier(UInt32(BitcoinSUtil.flipEndianness(bytes)))
  }

  override def write(typeIdentifier: TypeIdentifier): String = {
    BitcoinSUtil.flipEndianness(typeIdentifier.num.bytes)
  }
}
object RawTypeIdentifierSerializer extends RawTypeIdentifierSerializer
