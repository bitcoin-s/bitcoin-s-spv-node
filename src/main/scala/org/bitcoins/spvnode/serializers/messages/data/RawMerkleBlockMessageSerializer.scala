package org.bitcoins.spvnode.serializers.messages.data

import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.protocol.CompactSizeUInt
import org.bitcoins.core.serializers.RawBitcoinSerializer
import org.bitcoins.core.serializers.blockchain.RawBlockHeaderSerializer
import org.bitcoins.core.util.BitcoinSUtil
import org.bitcoins.spvnode.messages.MerkleBlockMessage
import org.bitcoins.spvnode.messages.data.MerkleBlockMessage
import org.bitcoins.spvnode.messages.data.MerkleBlockMessage.MerkleBlockMessageImpl

import scala.annotation.tailrec

/**
  * Created by chris on 6/2/16.
  * Responsible for serialization and deserialization of MerkleBlockMessages
  * https://bitcoin.org/en/developer-reference#merkleblock
  */
trait RawMerkleBlockMessageSerializer extends RawBitcoinSerializer[MerkleBlockMessage] {

  def read(bytes : List[Byte]) : MerkleBlockMessage = {
    val blockHeader = RawBlockHeaderSerializer.read(bytes)
    val bytesAfterBlockHeaderParsing = bytes.slice(blockHeader.bytes.size, bytes.size)
    val transactionCount = BitcoinSUtil.toLong(bytesAfterBlockHeaderParsing.slice(0,4))
    val hashCount = BitcoinSUtil.parseCompactSizeUInt(
      bytesAfterBlockHeaderParsing.slice(4,bytesAfterBlockHeaderParsing.size))
    val txHashStartIndex = (4 + hashCount.size).toInt
    val bytesAfterHashCountParsing = bytesAfterBlockHeaderParsing.slice(txHashStartIndex,bytesAfterBlockHeaderParsing.size)
    val (txHashes, bytesAfterTxHashParsing) = parseTransactionHashes(bytesAfterHashCountParsing,hashCount)

    val flagCount = BitcoinSUtil.parseCompactSizeUInt(bytesAfterBlockHeaderParsing)
    val flags = bytesAfterTxHashParsing.slice(flagCount.size.toInt, bytesAfterTxHashParsing.size)

    MerkleBlockMessage(blockHeader, transactionCount,hashCount,txHashes,flagCount,flags)
  }

  def write(merkleBlockMessage: MerkleBlockMessage) : String = {
    merkleBlockMessage.blockHeader.hex + BitcoinSUtil.longToHex(merkleBlockMessage.transactionCount) +
    merkleBlockMessage.hashCount.hex + merkleBlockMessage.hashes.map(_.hex).mkString +
    merkleBlockMessage.flagCount.hex + BitcoinSUtil.encodeHex(merkleBlockMessage.flags)
  }


  /**
    * Parses a sequence of transactions hashes from inside of a merkle block message
    * @param bytes the bytes from which the tx hashes are parsed from
    * @param hashCount the amount of tx hashes we need to parse from bytes
    * @return the sequence of tx hashes and the remaining bytes to be parsed into a MerkleBlockMessage
    */
  private def parseTransactionHashes(bytes : Seq[Byte], hashCount : CompactSizeUInt) : (Seq[DoubleSha256Digest], Seq[Byte]) = {
    @tailrec
    def loop(remainingHashes : Long, remainingBytes : Seq[Byte],
             accum : List[DoubleSha256Digest]) : (Seq[DoubleSha256Digest], Seq[Byte]) = {
      if (remainingHashes <= 0) (accum.reverse,remainingBytes)
      else loop(remainingHashes-1, remainingBytes.slice(32,remainingBytes.size), DoubleSha256Digest(remainingBytes.take(32)) :: accum)
    }

    loop(hashCount.num, bytes, List())
  }
}

object RawMerkleBlockMessageSerializer extends RawMerkleBlockMessageSerializer
