package org.bitcoins.spvnode.serializers.messages

import org.bitcoins.core.serializers.RawBitcoinSerializer
import org.bitcoins.spvnode.messages.TypeIdentifier

/**
  * Created by chris on 5/31/16.
  * Reads and writes a type identifier on a peer-to-peer network
  * https://bitcoin.org/en/developer-reference#data-messages
  */
trait RawTypeIdentifierSerializer extends RawBitcoinSerializer[TypeIdentifier] {

  override def read(bytes : List[Byte]) : TypeIdentifier = ???

  override def write(typeIdentifier: TypeIdentifier) : String = ???
}

object RawTypeIdentifierSerializer extends RawTypeIdentifierSerializer
