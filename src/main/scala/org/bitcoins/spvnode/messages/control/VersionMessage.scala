package org.bitcoins.spvnode.messages.control

import java.net.InetAddress

import org.bitcoins.core.config.NetworkParameters
import org.bitcoins.core.number.{Int32, Int64, UInt64}
import org.bitcoins.core.protocol.CompactSizeUInt
import org.bitcoins.core.util.Factory
import org.bitcoins.spvnode.messages._
import org.bitcoins.spvnode.serializers.messages.control.RawVersionMessageSerializer
import org.bitcoins.spvnode.versions.{ProtocolVersion}
import org.bitcoins.spvnode.constant.Constants
import org.joda.time.DateTime



/**
  * Created by chris on 6/3/16.
  * Companion object responsible for creating VersionMessages on the p2p network
  * https://bitcoin.org/en/developer-reference#version
  */
object VersionMessage extends Factory[VersionMessage] {

  private case class VersionMessageImpl(version : ProtocolVersion, services : ServiceIdentifier, timestamp : Int64,
                                               addressReceiveServices : ServiceIdentifier, addressReceiveIpAddress : InetAddress,
                                               addressReceivePort : Int, addressTransServices : ServiceIdentifier,
                                               addressTransIpAddress : InetAddress, addressTransPort : Int,
                                               nonce : UInt64, userAgentSize : CompactSizeUInt, userAgent : String,
                                               startHeight : Int32, relay : Boolean) extends VersionMessage

  override def fromBytes(bytes : Seq[Byte]) : VersionMessage = RawVersionMessageSerializer.read(bytes)

  def apply(version : ProtocolVersion, services : ServiceIdentifier, timestamp : Int64,
            addressReceiveServices : ServiceIdentifier, addressReceiveIpAddress : InetAddress,
            addressReceivePort : Int, addressTransServices : ServiceIdentifier,
            addressTransIpAddress : InetAddress, addressTransPort : Int,
            nonce : UInt64,  userAgent : String,
            startHeight : Int32, relay : Boolean) : VersionMessage = {
    val userAgentSize : CompactSizeUInt = CompactSizeUInt.calculateCompactSizeUInt(userAgent.getBytes)
    VersionMessageImpl(version, services, timestamp, addressReceiveServices, addressReceiveIpAddress,
      addressReceivePort, addressTransServices, addressTransIpAddress, addressTransPort,
      nonce, userAgentSize, userAgent, startHeight, relay)
  }

  def apply(network : NetworkParameters, receivingIpAddress : InetAddress) : VersionMessage = {
    val transmittingIpAddress = InetAddress.getLocalHost
    VersionMessage(network,receivingIpAddress,transmittingIpAddress)
  }

  def apply(network : NetworkParameters, receivingIpAddress : InetAddress, transmittingIpAddress : InetAddress) : VersionMessage = {
    val nonce = UInt64.zero
    val userAgent = Constants.userAgent
    val startHeight = Int32.zero
    val relay = false
    VersionMessage(Constants.version, UnnamedService, Int64(DateTime.now.getMillis), UnnamedService, receivingIpAddress,
      network.port, NodeNetwork, transmittingIpAddress, network.port, nonce, userAgent, startHeight, relay)
  }

  def apply(network: NetworkParameters): VersionMessage = {
    val transmittingIpAddress = InetAddress.getByName(network.dnsSeeds(0))
    VersionMessage(network,transmittingIpAddress)
  }
}
