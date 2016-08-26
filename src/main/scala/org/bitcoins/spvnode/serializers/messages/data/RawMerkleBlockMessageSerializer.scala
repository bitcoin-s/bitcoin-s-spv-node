package org.bitcoins.spvnode.serializers.messages.data

import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.number.UInt32
import org.bitcoins.core.protocol.CompactSizeUInt
import org.bitcoins.core.serializers.RawBitcoinSerializer
import org.bitcoins.core.serializers.blockchain.RawBlockHeaderSerializer
import org.bitcoins.core.util.{BitcoinSLogger, BitcoinSUtil}
import org.bitcoins.spvnode.block.MerkleBlock
import org.bitcoins.spvnode.messages.MerkleBlockMessage
import org.bitcoins.spvnode.messages.data.MerkleBlockMessage

import scala.annotation.tailrec

/**
  * Created by chris on 6/2/16.
  * Responsible for serialization and deserialization of MerkleBlockMessages
  * https://bitcoin.org/en/developer-reference#merkleblock
  */
trait RawMerkleBlockMessageSerializer extends RawBitcoinSerializer[MerkleBlockMessage] with BitcoinSLogger {

  def read(bytes : List[Byte]) : MerkleBlockMessage = {
    val merkleBlock = MerkleBlock(bytes)
    MerkleBlockMessage(merkleBlock)
  }

  def write(merkleBlockMessage: MerkleBlockMessage) : String = merkleBlockMessage.merkleBlock.hex

}

object RawMerkleBlockMessageSerializer extends RawMerkleBlockMessageSerializer
