package org.bitcoins.spvnode.headers

import org.bitcoins.core.config.NetworkParameters
import org.bitcoins.core.protocol.NetworkElement
import org.bitcoins.core.util.{BitcoinSLogger, BitcoinSUtil, CryptoUtil, Factory}
import org.bitcoins.spvnode.messages.NetworkPayload
import org.bitcoins.spvnode.serializers.headers.RawNetworkHeaderSerializer


/**
  * Created by chris on 5/31/16.
  * Represents a message header on the peer-to-peer network
  * https://bitcoin.org/en/developer-reference#message-headers
  */
sealed trait NetworkHeader extends NetworkElement with BitcoinSLogger {

  override def hex = RawNetworkHeaderSerializer.write(this)

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
  def payloadSize : Long

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


object NetworkHeader extends Factory[NetworkHeader] {

  private case class NetworkHeaderImpl(network : Seq[Byte], commandName : String,
                                       payloadSize : Long, checksum : Seq[Byte]) extends NetworkHeader

  override def fromBytes(bytes : Seq[Byte]) : NetworkHeader = RawNetworkHeaderSerializer.read(bytes)

  override def fromHex(hex : String) : NetworkHeader = fromBytes(BitcoinSUtil.decodeHex(hex))

  def apply(network : Seq[Byte], commandName : String, payloadSize : Long, checksum : Seq[Byte]) : NetworkHeader = {
    NetworkHeaderImpl(network, commandName, payloadSize, checksum)
  }

  /**
    * Builds a message header from a message
    * @param message
    * @return
    */
  def apply(network : NetworkParameters, message : NetworkPayload) : NetworkHeader = {
    val checksum = CryptoUtil.doubleSHA256(message.bytes)
    NetworkHeader(network.magicBytes, message.commandName, message.bytes.size, checksum.bytes.take(4))
  }
}
