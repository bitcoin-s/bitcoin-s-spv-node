package org.bitcoins.spvnode.serializers.control

import java.net.InetAddress

import org.bitcoins.core.serializers.RawBitcoinSerializer
import org.bitcoins.core.util.{BitcoinSLogger, BitcoinSUtil}
import org.bitcoins.spvnode.messages.VersionMessage
import org.bitcoins.spvnode.messages.control.{ServiceIdentifier, VersionMessage}
import org.bitcoins.spvnode.util.{BitcoinSpvNodeUtil, NetworkIpAddress}
import org.bitcoins.spvnode.versions.ProtocolVersion

/**
  * Created by chris on 6/2/16.
  * Responsible for serialization and deserialization of VersionMessages on the p2p network
  * https://bitcoin.org/en/developer-reference#version
  */
trait RawVersionMessageSerializer extends RawBitcoinSerializer[VersionMessage] with BitcoinSLogger {

  def read(bytes : List[Byte]) : VersionMessage = {
    val version = ProtocolVersion(bytes.take(4))
    val services = ServiceIdentifier(bytes.slice(4,12))
    val timestamp = BitcoinSUtil.toLong(bytes.slice(12,20))
    val addressReceiveServices = ServiceIdentifier(bytes.slice(20,28))
    val addressReceiveIpAddress = InetAddress.getByAddress(bytes.slice(28,44).toArray)
    val addressReceivePort = BitcoinSUtil.toLong(bytes.slice(44,46).reverse).toInt
    val addressTransServices = ServiceIdentifier(bytes.slice(46,54))

    val addressTransIpAddress = InetAddress.getByAddress(bytes.slice(54,70).toArray)
    val addressTransPort = BitcoinSUtil.toLong(bytes.slice(70,72).reverse).toInt
    val nonce = BigInt(bytes.slice(72,80).toArray)
    val userAgentSize = BitcoinSUtil.parseCompactSizeUInt(bytes.slice(80,bytes.size))
    logger.debug("User agent size: " + userAgentSize)
    val userAgentBytesStartIndex = 80 + userAgentSize.size.toInt
    logger.debug("User agent start index: " + userAgentBytesStartIndex)
    logger.debug("Remaining bytes: " + BitcoinSUtil.encodeHex(bytes.slice(userAgentBytesStartIndex, bytes.size)))
    val userAgentBytes = bytes.slice(userAgentBytesStartIndex, userAgentBytesStartIndex + userAgentSize.num.toInt)
    logger.debug("Last user agent byte: " + userAgentBytesStartIndex + userAgentSize.num.toInt)
    val userAgent = userAgentBytes.map(_.toChar).mkString
    logger.debug("User agent: " + userAgent)
    val startHeightStartIndex = (userAgentBytesStartIndex + userAgentSize.num).toInt
    logger.debug("Start height start index: " + startHeightStartIndex)
    logger.debug("Bytes size: " + bytes.size)
    logger.debug("Start height slice: " + BitcoinSUtil.encodeHex(bytes.slice(startHeightStartIndex, startHeightStartIndex + 4)))
    val startHeight = BitcoinSUtil.toLong(bytes.slice(startHeightStartIndex, startHeightStartIndex + 4)).toInt
    val relay = bytes(startHeightStartIndex + 4) != 0

    VersionMessage(version,services,timestamp, addressReceiveServices, addressReceiveIpAddress,
      addressReceivePort, addressTransServices, addressTransIpAddress, addressTransPort,
      nonce,userAgentSize,userAgent, startHeight, relay)
  }

  def write(versionMessage: VersionMessage) : String = {
    versionMessage.version.hex + versionMessage.services.hex +
      addPadding(16,BitcoinSUtil.longToHex(versionMessage.timestamp)) +
      versionMessage.addressReceiveServices.hex +
      BitcoinSpvNodeUtil.writeAddress(versionMessage.addressReceiveIpAddress) +
      addPadding(4,BitcoinSUtil.flipEndianess(BitcoinSUtil.longToHex(versionMessage.addressReceivePort))) +
      versionMessage.addressTransServices.hex +
      BitcoinSpvNodeUtil.writeAddress(versionMessage.addressTransIpAddress) +
      addPadding(4,BitcoinSUtil.flipEndianess(BitcoinSUtil.longToHex(versionMessage.addressTransPort))) +
      addPadding(16,BitcoinSUtil.encodeHex(versionMessage.nonce.toByteArray)) +
      versionMessage.userAgentSize.hex +
      BitcoinSUtil.encodeHex(versionMessage.userAgent.getBytes) +
      addPadding(8,BitcoinSUtil.longToHex(versionMessage.startHeight)) +
      (if (versionMessage.relay) "01" else "00")
  }

}

object RawVersionMessageSerializer extends RawVersionMessageSerializer
