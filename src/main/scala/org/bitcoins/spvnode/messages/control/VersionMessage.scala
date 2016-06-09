package org.bitcoins.spvnode.messages.control

import java.net.{InetAddress, InetSocketAddress}

import org.bitcoins.core.config.NetworkParameters
import org.bitcoins.core.protocol.CompactSizeUInt
import org.bitcoins.core.util.{BitcoinSUtil, Factory}
import org.bitcoins.spvnode.messages._
import org.bitcoins.spvnode.BuildInfo
import org.bitcoins.spvnode.serializers.control.RawVersionMessageSerializer
import org.bitcoins.spvnode.versions.{ProtocolVersion, ProtocolVersion70012}
import org.joda.time.DateTime


/**
  * Created by chris on 6/3/16.
  * Companion object responsible for creating VersionMessages on the p2p network
  * https://bitcoin.org/en/developer-reference#version
  */
object VersionMessage extends Factory[VersionMessage] {



  override def fromBytes(bytes : Seq[Byte]) : VersionMessage = RawVersionMessageSerializer.read(bytes)

  def apply(version : ProtocolVersion, services : ServiceIdentifier, timestamp : Long,
            addressReceiveServices : ServiceIdentifier, addressReceiveIpAddress : InetAddress,
            addressReceivePort : Int, addressTransServices : ServiceIdentifier,
            addressTransIpAddress : InetAddress, addressTransPort : Int,
            nonce : BigInt, userAgentSize : CompactSizeUInt, userAgent : String,
            startHeight : Int, relay : Boolean) : VersionMessage = {
    VersionMessageRequest(version, services,timestamp, addressReceiveServices, addressReceiveIpAddress,
      addressReceivePort, addressTransServices, addressTransIpAddress, addressTransPort,
      nonce, userAgentSize, userAgent, startHeight, relay)
  }

  def apply(network : NetworkParameters, transmittingIpAddress : InetAddress) : VersionMessage = {
    val receivingIpAddress = InetAddress.getLocalHost
    val nonce = 0
    val userAgent = "/" + BuildInfo.name + "/" + BuildInfo.version
    val userAgentSize = CompactSizeUInt.calculateCompactSizeUInt(userAgent.map(_.toByte))
    val startHeight = 0
    val relay = false
    VersionMessageRequest(ProtocolVersion70012, UnnamedService, DateTime.now.getMillis, UnnamedService, receivingIpAddress,
      network.port, NodeNetwork, transmittingIpAddress, network.port, nonce, userAgentSize, userAgent, startHeight, relay)
  }

  def apply(network : NetworkParameters, receivingIpAddress : InetAddress, transmittingIpAddress : InetAddress) : VersionMessage = {
    val nonce = 0
    val userAgent = "/" + BuildInfo.name + "/" + BuildInfo.version
    val userAgentSize = CompactSizeUInt.calculateCompactSizeUInt(userAgent.map(_.toByte))
    val startHeight = 0
    val relay = false
    VersionMessageRequest(ProtocolVersion70012, UnnamedService, DateTime.now.getMillis, UnnamedService, receivingIpAddress,
      network.port, NodeNetwork, transmittingIpAddress, network.port, nonce, userAgentSize, userAgent, startHeight, relay)
  }
}

object VersionMessageRequest extends Factory[VersionMessageRequest] {

  private case class VersionMessageRequestImpl(version : ProtocolVersion, services : ServiceIdentifier, timestamp : Long,
                                               addressReceiveServices : ServiceIdentifier, addressReceiveIpAddress : InetAddress,
                                               addressReceivePort : Int, addressTransServices : ServiceIdentifier,
                                               addressTransIpAddress : InetAddress, addressTransPort : Int,
                                               nonce : BigInt, userAgentSize : CompactSizeUInt, userAgent : String,
                                               startHeight : Int, relay : Boolean) extends VersionMessageRequest
  override def fromBytes(bytes : Seq[Byte]) : VersionMessageRequest = {
    val versionMessage = RawVersionMessageSerializer.read(bytes)
    VersionMessageRequestImpl(versionMessage.version, versionMessage.services, versionMessage.timestamp,
      versionMessage.addressReceiveServices, versionMessage.addressReceiveIpAddress, versionMessage.addressReceivePort,
      versionMessage.addressTransServices, versionMessage.addressTransIpAddress, versionMessage.addressTransPort,
      versionMessage.nonce, versionMessage.userAgentSize, versionMessage.userAgent, versionMessage.startHeight,
      versionMessage.relay)
  }

  def apply(version : ProtocolVersion, services : ServiceIdentifier, timestamp : Long,
            addressReceiveServices : ServiceIdentifier, addressReceiveIpAddress : InetAddress,
            addressReceivePort : Int, addressTransServices : ServiceIdentifier,
            addressTransIpAddress : InetAddress, addressTransPort : Int,
            nonce : BigInt, userAgentSize : CompactSizeUInt, userAgent : String,
            startHeight : Int, relay : Boolean) : VersionMessageRequest = {
    VersionMessageRequestImpl(version, services,timestamp, addressReceiveServices, addressReceiveIpAddress,
      addressReceivePort, addressTransServices, addressTransIpAddress, addressTransPort,
      nonce, userAgentSize, userAgent, startHeight, relay)
  }

  def apply(network : NetworkParameters, transmittingIpAddress : InetAddress) : VersionMessageRequest = {
    val receivingIpAddress = new InetSocketAddress(network.port).getAddress
    val nonce = 0
    val userAgent = "/" + BuildInfo.name + "/" + BuildInfo.version
    val userAgentSize = BitcoinSUtil.parseCompactSizeUInt(userAgent.map(_.toByte))
    val startHeight = 0
    val relay = false
    VersionMessageRequestImpl(ProtocolVersion70012, UnnamedService, DateTime.now.getMillis, UnnamedService, receivingIpAddress,
      network.port, NodeNetwork, transmittingIpAddress, network.port, nonce, userAgentSize, userAgent, startHeight, relay)
  }
}

object VersionMessageResponse extends Factory[VersionMessageResponse] {

  private case class VersionMessageResponseImpl(version : ProtocolVersion, services : ServiceIdentifier, timestamp : Long,
                                                addressReceiveServices : ServiceIdentifier, addressReceiveIpAddress : InetAddress,
                                                addressReceivePort : Int, addressTransServices : ServiceIdentifier,
                                                addressTransIpAddress : InetAddress, addressTransPort : Int,
                                                nonce : BigInt, userAgentSize : CompactSizeUInt, userAgent : String,
                                                startHeight : Int, relay : Boolean) extends VersionMessageResponse
  override def fromBytes(bytes : Seq[Byte]) : VersionMessageResponse = {
    val versionMessage = RawVersionMessageSerializer.read(bytes)
    VersionMessageResponseImpl(versionMessage.version, versionMessage.services, versionMessage.timestamp,
      versionMessage.addressReceiveServices, versionMessage.addressReceiveIpAddress, versionMessage.addressReceivePort,
      versionMessage.addressTransServices, versionMessage.addressTransIpAddress, versionMessage.addressTransPort,
      versionMessage.nonce, versionMessage.userAgentSize, versionMessage.userAgent, versionMessage.startHeight,
      versionMessage.relay)
  }

  def apply(version : ProtocolVersion, services : ServiceIdentifier, timestamp : Long,
            addressReceiveServices : ServiceIdentifier, addressReceiveIpAddress : InetAddress,
            addressReceivePort : Int, addressTransServices : ServiceIdentifier,
            addressTransIpAddress : InetAddress, addressTransPort : Int,
            nonce : BigInt, userAgentSize : CompactSizeUInt, userAgent : String,
            startHeight : Int, relay : Boolean) : VersionMessageResponse = {
    VersionMessageResponseImpl(version, services,timestamp, addressReceiveServices, addressReceiveIpAddress,
      addressReceivePort, addressTransServices, addressTransIpAddress, addressTransPort,
      nonce, userAgentSize, userAgent, startHeight, relay)
  }

  def apply(network : NetworkParameters, transmittingIpAddress : InetAddress) : VersionMessageResponse = {
    val receivingIpAddress = InetAddress.getLocalHost
    val nonce = 0
    val userAgent = "/" + BuildInfo.name + "/" + BuildInfo.version
    val userAgentSize = BitcoinSUtil.parseCompactSizeUInt(userAgent.map(_.toByte))
    val startHeight = 0
    val relay = false
    VersionMessageResponseImpl(ProtocolVersion70012, UnnamedService, DateTime.now.getMillis, UnnamedService, receivingIpAddress,
      network.port, NodeNetwork, transmittingIpAddress, network.port, nonce, userAgentSize, userAgent, startHeight, relay)
  }
}

