package org.bitcoins.spvnode.messages.data

import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.protocol.CompactSizeUInt
import org.bitcoins.core.util.Factory
import org.bitcoins.spvnode.messages.GetHeadersMessage
import org.bitcoins.spvnode.serializers.messages.data.RawGetHeadersMessageSerializer
import org.bitcoins.spvnode.versions.ProtocolVersion

/**
  * Created by chris on 6/29/16.
  */
object GetHeadersMessage extends Factory[GetHeadersMessage] {
  private case class GetHeadersMessageImpl(version: ProtocolVersion, hashCount : CompactSizeUInt,
                                           hashes : Seq[DoubleSha256Digest], hashStop : DoubleSha256Digest) extends GetHeadersMessage

  override def fromBytes(bytes : Seq[Byte]): GetHeadersMessage = RawGetHeadersMessageSerializer.read(bytes)

  def apply(version: ProtocolVersion, hashCount : CompactSizeUInt,
            hashes : Seq[DoubleSha256Digest], hashStop : DoubleSha256Digest): GetHeadersMessage = {
    GetHeadersMessageImpl(version,hashCount,hashes,hashStop)
  }
}
