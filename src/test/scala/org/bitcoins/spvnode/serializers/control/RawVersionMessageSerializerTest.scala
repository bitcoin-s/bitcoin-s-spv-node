package org.bitcoins.spvnode.serializers.control

import org.bitcoins.core.number.{Int32, Int64}
import org.bitcoins.core.protocol.CompactSizeUInt
import org.bitcoins.core.util.BitcoinSUtil
import org.bitcoins.spvnode.messages.control.NodeNetwork
import org.bitcoins.spvnode.util.BitcoinSpvNodeUtil
import org.bitcoins.spvnode.versions.{ProtocolVersion, ProtocolVersion70002, ProtocolVersion70012}
import org.scalatest.{FlatSpec, MustMatchers}

/**
  * Created by chris on 6/3/16.
  */
class RawVersionMessageSerializerTest extends FlatSpec with MustMatchers {

  //take from the bitcoin developer reference underneath this seciton
  //https://bitcoin.org/en/developer-reference#version

  val protocolVersion = "72110100"
  val services = "0100000000000000"
  val timestamp = "bc8f5e5400000000"

  val receivingNodesServices = "0100000000000000"
  val receivingNodeIpAddress = "00000000000000000000ffffc61b6409"
  val receivingNodePort = "208d"

  val transNodeServices = "0100000000000000"
  val transNodeIpAddress = "00000000000000000000ffffcb0071c0"
  val transNodePort = "208d"
  val nonce = "128035cbc97953f8"

  val userAgentSize = "0f"
  val userAgent = "2f5361746f7368693a302e392e332f"
  val startHeight = "cf050500"
  val relay = "01"
  val hex = protocolVersion + services + timestamp + receivingNodesServices + receivingNodeIpAddress +
  receivingNodePort + transNodeServices + transNodeIpAddress + transNodePort + nonce +
  userAgentSize + userAgent + startHeight + relay
  "RawVersionMessageSerializer" must "read a raw version message from the p2p network" in {
    val versionMessage = RawVersionMessageSerializer.read(hex)
    versionMessage.version must be (ProtocolVersion(protocolVersion))
    versionMessage.services must be (NodeNetwork)
    versionMessage.timestamp must be (Int64(1415483324))

    versionMessage.addressReceiveServices must be (NodeNetwork)
    BitcoinSpvNodeUtil.writeAddress(versionMessage.addressReceiveIpAddress) must be (receivingNodeIpAddress)
    versionMessage.addressReceivePort must be (8333)

    versionMessage.addressTransServices must be (NodeNetwork)
    BitcoinSpvNodeUtil.writeAddress(versionMessage.addressTransIpAddress) must be (transNodeIpAddress)
    versionMessage.addressTransPort must be (8333)

    versionMessage.nonce.underlying must be (BigInt(BitcoinSUtil.decodeHex(nonce).toArray))

    versionMessage.userAgentSize must be (CompactSizeUInt(15,1))
    versionMessage.userAgent must be ("/Satoshi:0.9.3/")

    versionMessage.startHeight must be (Int32(329167))
    versionMessage.relay must be (true)


  }

  it must "write a VersionMessage to its original hex format" in {
    val versionMessage = RawVersionMessageSerializer.read(hex)
    RawVersionMessageSerializer.write(versionMessage) must be (hex)
  }


  it must "read a VersionMessage that bitcoins created" in {
    //random version message bitcoins created when connecting to a testnet seed
    //and sending it a version message
    val hex = "7c1101000000000000000000d805833655010000000000000000000000000000000000000000ffff0a940106479d010000000000000000000000000000000000ffff739259bb479d0000000000000000182f626974636f696e732d7370762d6e6f64652f302e302e310000000000"
    val versionMessage = RawVersionMessageSerializer.read(hex)
    RawVersionMessageSerializer.write(versionMessage) must be (hex)
  }

  it must "read a version message from a full node on the network" in {
    val hex = "721101000100000000000000e0165b5700000000010000000000000000000000000000000000ffffad1f27a8479d010000000000000000000000000000000000ffff00000000479d68dc32a9948d149b102f5361746f7368693a302e31312e322f7f440d0001"
    val versionMessage = RawVersionMessageSerializer.read(hex)
    versionMessage.version must be (ProtocolVersion70002)

    versionMessage.userAgent must be ("/Satoshi:0.11.2/")
    versionMessage.relay must be (true)
  }
}
