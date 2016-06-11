package org.bitcoins.spvnode.serializers

import org.bitcoins.core.serializers.RawBitcoinSerializer
import org.bitcoins.spvnode.NetworkMessage
import org.bitcoins.spvnode.headers.NetworkHeader
import org.bitcoins.spvnode.messages.NetworkPayload

/**
  * Created by chris on 6/11/16.
  */
trait RawNetworkMessageSerializer extends RawBitcoinSerializer[NetworkMessage] {

  def read(bytes : List[Byte]) : NetworkMessage = {
    //first 80 bytes are the header
    val header = NetworkHeader(bytes.take(80))
    val payload = NetworkPayload(header, bytes.slice(80,bytes.size))
    NetworkMessage(header,payload)
  }

  def write(networkMessage: NetworkMessage) : String = ???
}

object RawNetworkMessageSerializer extends RawNetworkMessageSerializer
