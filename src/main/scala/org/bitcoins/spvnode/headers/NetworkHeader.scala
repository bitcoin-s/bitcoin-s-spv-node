package org.bitcoins.spvnode.headers

import org.bitcoins.core.config.NetworkParameters
import org.bitcoins.core.number.UInt32
import org.bitcoins.core.protocol.NetworkElement
import org.bitcoins.core.util.{BitcoinSLogger, CryptoUtil, Factory}
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
  def payloadSize : UInt32

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
                                       payloadSize : UInt32, checksum : Seq[Byte]) extends NetworkHeader {
    require(bytes.length == 24,"NetworkHeaders must be 24 bytes")
  }

  override def fromBytes(bytes : Seq[Byte]) : NetworkHeader = RawNetworkHeaderSerializer.read(bytes)

  /**
    * Creates a [[NetworkHeader]] from all of its individual components
    * @param network the [[NetworkParameters]] object indicating what network this header is sent on
    * @param commandName the name of the command being sent in the header
    * @param payloadSize the size of the payload being sent by this header
    * @param checksum the checksum of the payload to ensure that the entire payload was sent
    * @return
    */
  def apply(network : Seq[Byte], commandName : String, payloadSize : UInt32, checksum : Seq[Byte]) : NetworkHeader = {
    NetworkHeaderImpl(network, commandName, payloadSize, checksum)
  }

  /**
    * Creates a [[NetworkHeader]] from it's [[NetworkParameters]] and [[NetworkPayload]]
    * @param network the [[NetworkParameters]] object that indicates what network the payload needs to be sent on
    * @param payload the [[NetworkPayload]] object that needs to be sent on the network
    * @return
    */
  def apply(network : NetworkParameters, payload : NetworkPayload) : NetworkHeader = {
    val checksum = CryptoUtil.doubleSHA256(payload.bytes)
    NetworkHeader(network.magicBytes, payload.commandName, UInt32(payload.bytes.size), checksum.bytes.take(4))
  }
}
