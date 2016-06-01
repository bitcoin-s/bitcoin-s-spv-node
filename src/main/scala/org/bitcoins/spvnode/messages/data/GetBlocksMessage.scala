package org.bitcoins.spvnode.messages.data

import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.protocol.CompactSizeUInt
import org.bitcoins.core.util.Factory
import org.bitcoins.spvnode.messages.GetBlocksMessage
import org.bitcoins.spvnode.serializers.messages.data.RawGetBlocksMessageSerializer
import org.bitcoins.spvnode.versions.ProtocolVersion

/**
  * Created by chris on 6/1/16.
  */
object GetBlocksMessage extends Factory[GetBlocksMessage] {

  private case class GetBlocksMessageImpl(protocolVersion: ProtocolVersion, hashCount : CompactSizeUInt,
                                          blockHeaderHashes : Seq[DoubleSha256Digest],
                                          stopHash : DoubleSha256Digest) extends GetBlocksMessage

  def apply(version : ProtocolVersion, hashCount : CompactSizeUInt, blockHeaderHashes : Seq[DoubleSha256Digest],
            stopHash : DoubleSha256Digest) : GetBlocksMessage = {
    GetBlocksMessageImpl(version,hashCount,blockHeaderHashes,stopHash)
  }

  def fromBytes(bytes : Seq[Byte]) : GetBlocksMessage = RawGetBlocksMessageSerializer.read(bytes)
}
