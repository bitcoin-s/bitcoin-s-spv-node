package org.bitcoins.spvnode.messages.data

import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.protocol.CompactSizeUInt
import org.bitcoins.core.protocol.blockchain.BlockHeader
import org.bitcoins.core.util.Factory
import org.bitcoins.spvnode.messages.MerkleBlockMessage
import org.bitcoins.spvnode.serializers.messages.data.RawMerkleBlockMessageSerializer

/**
  * Created by chris on 6/2/16.
  * https://bitcoin.org/en/developer-reference#merkleblock
  */
object MerkleBlockMessage extends Factory[MerkleBlockMessage] {

  private case class MerkleBlockMessageImpl(blockHeader : BlockHeader, transactionCount : Long,
                                            hashCount : CompactSizeUInt, hashes : Seq[DoubleSha256Digest],
                                            flagCount : CompactSizeUInt, flags : Seq[Byte]) extends MerkleBlockMessage

  def fromBytes(bytes : Seq[Byte]) : MerkleBlockMessage = RawMerkleBlockMessageSerializer.read(bytes)

  def apply(blockHeader: BlockHeader, transactionCount : Long, hashCount : CompactSizeUInt, hashes : Seq[DoubleSha256Digest],
            flagCount : CompactSizeUInt, flags : Seq[Byte]) : MerkleBlockMessage = {
    MerkleBlockMessageImpl(blockHeader, transactionCount,hashCount,hashes,flagCount,flags)
  }
}
