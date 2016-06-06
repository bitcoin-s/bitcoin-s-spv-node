package org.bitcoins.spvnode.messages.control

import java.net.InetAddress

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

  private case class VersionMessageImpl(version : ProtocolVersion, services : ServiceIdentifier, timestamp : Long,
                                        addressReceiveServices : ServiceIdentifier, addressReceiveIpAddress : InetAddress,
                                        addressReceivePort : Int, addressTransServices : ServiceIdentifier,
                                        addressTransIpAddress : InetAddress, addressTransPort : Int,
                                        nonce : BigInt, userAgentSize : CompactSizeUInt, userAgent : String,
                                        startHeight : Int, relay : Boolean) extends VersionMessage

  override def fromBytes(bytes : Seq[Byte]) : VersionMessage = RawVersionMessageSerializer.read(bytes)

  def apply(version : ProtocolVersion, services : ServiceIdentifier, timestamp : Long,
            addressReceiveServices : ServiceIdentifier, addressReceiveIpAddress : InetAddress,
            addressReceivePort : Int, addressTransServices : ServiceIdentifier,
            addressTransIpAddress : InetAddress, addressTransPort : Int,
            nonce : BigInt, userAgentSize : CompactSizeUInt, userAgent : String,
            startHeight : Int, relay : Boolean) : VersionMessage = {
    VersionMessageImpl(version, services,timestamp, addressReceiveServices, addressReceiveIpAddress,
      addressReceivePort, addressTransServices, addressTransIpAddress, addressTransPort,
      nonce, userAgentSize, userAgent, startHeight, relay)
  }

  def apply(network : NetworkParameters, transmittingIpAddress : InetAddress) : VersionMessage = {

    val receivingIpAddress = InetAddress.getLocalHost
    val nonce = 0
    val userAgent = "/bitcoin-s/" + BuildInfo.version
    val userAgentSize = BitcoinSUtil.parseCompactSizeUInt(userAgent.map(_.toByte))
    val startHeight = 0
    val relay = false
    VersionMessageImpl(ProtocolVersion70012, UnnamedService, DateTime.now.getMillis, UnnamedService, receivingIpAddress,
      network.port, UnnamedService, transmittingIpAddress, network.port, nonce, userAgentSize, userAgent, startHeight, relay)
  }
}

