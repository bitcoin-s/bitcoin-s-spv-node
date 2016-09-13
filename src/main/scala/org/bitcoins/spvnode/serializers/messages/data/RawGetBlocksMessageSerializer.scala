package org.bitcoins.spvnode.serializers.messages.data

import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.protocol.CompactSizeUInt
import org.bitcoins.core.serializers.RawBitcoinSerializer
import org.bitcoins.core.util.BitcoinSUtil
import org.bitcoins.spvnode.messages.GetBlocksMessage
import org.bitcoins.spvnode.messages.data.GetBlocksMessage
import org.bitcoins.spvnode.versions.ProtocolVersion

import scala.annotation.tailrec

/**
  * Created by chris on 6/1/16.
  * This trait is responsible for the serialization and deserialization of
  * getblocks messages in on the p2p network
  * https://bitcoin.org/en/developer-reference#getblocks
  */
trait RawGetBlocksMessageSerializer extends RawBitcoinSerializer[GetBlocksMessage] {

  def read(bytes : List[Byte]) : GetBlocksMessage = {
    val version = ProtocolVersion(bytes.take(4))
    val hashCount = CompactSizeUInt.parseCompactSizeUInt(bytes.slice(4,bytes.size))
    val blockHeaderStartByte = (hashCount.size + 4).toInt
    val blockHeaderBytesStopHash = bytes.slice(blockHeaderStartByte, bytes.size)
    val (blockHashHeaders,remainingBytes) = parseBlockHeaders(blockHeaderBytesStopHash,hashCount)
    val stopHash = DoubleSha256Digest(remainingBytes.slice(0,32))
    GetBlocksMessage(version,hashCount,blockHashHeaders,stopHash)
  }

  def write(getBlocksMessage: GetBlocksMessage) : String = {
    getBlocksMessage.protocolVersion.hex + getBlocksMessage.hashCount.hex +
    getBlocksMessage.blockHeaderHashes.map(_.hex).mkString +
    getBlocksMessage.stopHash.hex
  }

  /**
    * Helper function to parse block headers from a sequence of bytes
    * Hashes are 32 bytes
    * @param bytes the bytes which need to be parsed into BlockHeader hashes
    * @param compactSizeUInt the p2p network object used to indicate how many block header hashes there are
    * @return the sequence of hashes and the remaining bytes that need to be parsed
    */
  private def parseBlockHeaders(bytes : Seq[Byte],compactSizeUInt: CompactSizeUInt) : (Seq[DoubleSha256Digest],Seq[Byte]) = {
    @tailrec
    def loop(remainingHeaders : Long, accum : List[DoubleSha256Digest],
             remainingBytes : Seq[Byte]) : (List[DoubleSha256Digest], Seq[Byte]) = {
      if (remainingHeaders <= 0) (accum.reverse,remainingBytes)
      else loop(remainingHeaders - 1, DoubleSha256Digest(remainingBytes.slice(0,32)) :: accum,
        remainingBytes.slice(32,remainingBytes.size))
    }
    loop(compactSizeUInt.num.toInt, List(), bytes)
  }
}

object RawGetBlocksMessageSerializer extends RawGetBlocksMessageSerializer
