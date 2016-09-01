package org.bitcoins.spvnode.serializers.messages.control

import org.bitcoins.core.protocol.CompactSizeUInt
import org.bitcoins.core.serializers.RawBitcoinSerializer
import org.bitcoins.core.util.{BitcoinSUtil, Factory}
import org.bitcoins.spvnode.messages.RejectMessage
import org.bitcoins.spvnode.messages.control.RejectMessage

/**
  * Created by chris on 8/31/16.
  */
trait RawRejectMessageSerializer extends RawBitcoinSerializer[RejectMessage] {

  def read(bytes: List[Byte]): RejectMessage = {
    val messageSize = CompactSizeUInt.parseCompactSizeUInt(bytes)
    val message: String = bytes.slice(messageSize.size.toInt, messageSize.size.toInt +
      messageSize.num.toInt).map(_.toChar).mkString
    val code: Char = bytes(messageSize.size.toInt + messageSize.num.toInt).toChar
    val reasonSizeStartIndex = messageSize.size.toInt + messageSize.num.toInt + 1
    val reasonSize = CompactSizeUInt.parseCompactSizeUInt(bytes.slice(reasonSizeStartIndex.toInt,bytes.size))
    val reason = bytes.slice((reasonSizeStartIndex + reasonSize.size).toInt,
      (reasonSizeStartIndex + reasonSize.size.toInt + reasonSize.num.toInt)).map(_.toChar).mkString
    val extraStartIndex = (reasonSizeStartIndex + reasonSize.size.toInt + reasonSize.num.toInt)
    val extra = bytes.slice(extraStartIndex,bytes.size)
    RejectMessage(messageSize,message,code,reasonSize,reason,extra)
  }

  def write(rejectMessage: RejectMessage): String = {
    rejectMessage.messageSize.hex +
      BitcoinSUtil.encodeHex(rejectMessage.message.map(_.toByte)) +
      BitcoinSUtil.encodeHex(rejectMessage.code.toByte) +
      rejectMessage.reasonSize.hex + BitcoinSUtil.encodeHex(rejectMessage.reason.map(_.toByte)) +
      BitcoinSUtil.encodeHex(rejectMessage.extra)
  }
}

object RawRejectMessageSerializer extends RawRejectMessageSerializer
