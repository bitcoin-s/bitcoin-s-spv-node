package org.bitcoins.spvnode.headers

import org.bitcoins.core.protocol.NetworkElement
import org.bitcoins.core.util.{BitcoinSLogger, BitcoinSUtil, Factory}

/**
  * Created by chris on 5/31/16.
  * Represents a message header on the peer-to-peer network
  * https://bitcoin.org/en/developer-reference#message-headers
  */
sealed trait MessageHeader extends NetworkElement with BitcoinSLogger {

  override def hex = RawMessageHeaderSerializer.write(this)

  /**
    * Magic bytes indicating the originating network;
    * used to seek to next message when stream state is unknown.
    * @return
    */
  def network : Seq[Byte]

  /**
    * ASCII string which identifies what message type is contained in the payload.
    * Followed by nulls (0x00) to pad out byte count; for example: version\0\0\0\0\0.
    *
    * @return
    */
  def commandName : String

  /**
    * Number of bytes in payload. The current maximum number of bytes (MAX_SIZE) allowed in the payload
    * by Bitcoin Core is 32 MiB—messages with a payload size larger than this will be dropped or rejected.
    *
    * @return
    */
  def payloadSize : Int

  /**
    * Added in protocol version 209.
    * First 4 bytes of SHA256(SHA256(payload)) in internal byte order.
    * If payload is empty, as in verack and getaddr messages,
    * the checksum is always 0x5df6e0e2 (SHA256(SHA256(""))).
    *
    * @return
    */
  def checksum : Seq[Byte]

}


object MessageHeader extends Factory[MessageHeader] {

  private case class MessageHeaderImpl(network : Seq[Byte], commandName : String,
                                       payloadSize : Int, checksum : Seq[Byte]) extends MessageHeader

  override def fromBytes(bytes : Seq[Byte]) : MessageHeader = RawMessageHeaderSerializer.read(bytes)

  override def fromHex(hex : String) : MessageHeader = fromBytes(BitcoinSUtil.decodeHex(hex))

  def apply(hex : String) : MessageHeader = fromHex(hex)

  def apply(bytes : Seq[Byte]) : MessageHeader = fromBytes(bytes)
}
