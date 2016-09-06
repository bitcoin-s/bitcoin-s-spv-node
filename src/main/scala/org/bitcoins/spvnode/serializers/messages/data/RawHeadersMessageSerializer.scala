package org.bitcoins.spvnode.serializers.messages.data

import org.bitcoins.core.protocol.CompactSizeUInt
import org.bitcoins.core.protocol.blockchain.BlockHeader
import org.bitcoins.core.serializers.RawBitcoinSerializer
import org.bitcoins.spvnode.messages.HeadersMessage
import org.bitcoins.spvnode.messages.data.HeadersMessage

import scala.annotation.tailrec

/**
  * Created by chris on 7/5/16.
  */
trait RawHeadersMessageSerializer extends RawBitcoinSerializer[HeadersMessage] {

  def read(bytes: List[Byte]): HeadersMessage = {
    val compactSizeUInt = CompactSizeUInt.parseCompactSizeUInt(bytes)
    val headerStartIndex = compactSizeUInt.size.toInt
    val headerBytes = bytes.slice(headerStartIndex, bytes.length)
    val headers = parseBlockHeaders(headerBytes,compactSizeUInt)
    HeadersMessage(compactSizeUInt,headers)
  }

  def write(headersMessage: HeadersMessage): String = {
    headersMessage.count.hex + headersMessage.headers.map(_.hex + "00").mkString
  }

  private def parseBlockHeaders(bytes: Seq[Byte], compactSizeUInt: CompactSizeUInt): Seq[BlockHeader] = {
    @tailrec
    def loop(remainingBytes: Seq[Byte], remainingHeaders: Long, accum: List[BlockHeader]): Seq[BlockHeader] = {
      if (remainingHeaders <= 0) accum
      //81 is because HeadersMessage appends 0x00 at the end of every block header for some reason
      //read https://bitcoin.org/en/developer-reference#headers
      else {
        require(remainingBytes.size >= 80, "We do not have enough bytes for another block header, this probably means a tcp frame was not aligned")
        loop(remainingBytes.slice(81,remainingBytes.length), remainingHeaders - 1,
          BlockHeader(remainingBytes.take(80)) :: accum)
      }
    }
    loop(bytes,compactSizeUInt.num.toInt, List()).reverse
  }
}

object RawHeadersMessageSerializer extends RawHeadersMessageSerializer