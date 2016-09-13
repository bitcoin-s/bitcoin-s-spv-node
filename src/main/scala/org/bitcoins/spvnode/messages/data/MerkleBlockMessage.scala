package org.bitcoins.spvnode.messages.data

import org.bitcoins.core.util.Factory
import org.bitcoins.spvnode.block.MerkleBlock
import org.bitcoins.spvnode.messages.MerkleBlockMessage
import org.bitcoins.spvnode.serializers.messages.data.RawMerkleBlockMessageSerializer

/**
  * Created by chris on 6/2/16.
  * https://bitcoin.org/en/developer-reference#merkleblock
  */
object MerkleBlockMessage extends Factory[MerkleBlockMessage] {

  private case class MerkleBlockMessageImpl(merkleBlock : MerkleBlock) extends MerkleBlockMessage

  def fromBytes(bytes : Seq[Byte]) : MerkleBlockMessage = RawMerkleBlockMessageSerializer.read(bytes)

  def apply(merkleBlock: MerkleBlock) : MerkleBlockMessage = {
    MerkleBlockMessageImpl(merkleBlock)
  }
}
