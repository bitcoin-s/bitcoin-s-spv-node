package org.bitcoins.spvnode.serializers.messages.data

import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.protocol.CompactSizeUInt
import org.bitcoins.core.serializers.RawBitcoinSerializer
import org.bitcoins.spvnode.messages.GetHeadersMessage
import org.bitcoins.spvnode.messages.data.GetHeadersMessage
import org.bitcoins.spvnode.versions.ProtocolVersion

import scala.annotation.tailrec

/**
  * Created by chris on 6/29/16.
  */
trait RawGetHeadersMessageSerializer extends RawBitcoinSerializer[GetHeadersMessage] {

  override def read(bytes: List[Byte]): GetHeadersMessage = {
    val version = ProtocolVersion(bytes.take(4))
    val hashCount = CompactSizeUInt.parseCompactSizeUInt(bytes.slice(4,bytes.length))
    val hashesStartIndex = (hashCount.size + 4).toInt
    val (hashes, remainingBytes) = parseHashes(bytes.slice(hashesStartIndex, bytes.length), hashCount)
    val hashStop = DoubleSha256Digest(remainingBytes.take(32))
    GetHeadersMessage(version,hashCount,hashes,hashStop)
  }
  override def write(getHeadersMessage: GetHeadersMessage): String = {
    getHeadersMessage.version.hex + getHeadersMessage.hashCount.hex +
    getHeadersMessage.hashes.map(_.hex).mkString + getHeadersMessage.hashStop.hex
  }


  /**
    * Parses hashes inside of [[GetHeadersMessage]]
    * @param bytes the bytes which the hashes are parsed from
    * @param numHashes the number of hases that need to be parsed
    * @return the parsed hases and the remaining bytes in the network message
    */
  private def parseHashes(bytes : Seq[Byte], numHashes : CompactSizeUInt): (Seq[DoubleSha256Digest], Seq[Byte]) = {
    @tailrec
    def loop(remainingBytes : Seq[Byte], remainingHashes : Long, accum : Seq[DoubleSha256Digest]): (Seq[DoubleSha256Digest], Seq[Byte]) = {
      if (remainingHashes <= 0) (accum.reverse, remainingBytes)
      else {
        val hash = DoubleSha256Digest(remainingBytes.take(32))
        loop(remainingBytes.slice(32,remainingBytes.length), remainingHashes-1, hash +: accum)
      }
    }
    loop(bytes, numHashes.num.toInt, Seq())
  }
}

object RawGetHeadersMessageSerializer extends RawGetHeadersMessageSerializer
