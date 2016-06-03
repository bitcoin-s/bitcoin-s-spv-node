package org.bitcoins.spvnode.serializers.control

import org.bitcoins.spvnode.messages.control.NodeNetwork
import org.scalatest.{FlatSpec, MustMatchers}

/**
  * Created by chris on 6/2/16.
  */
class RawNetworkIpAddressSerializerTest extends FlatSpec with MustMatchers {

  val hex = "d91f4854010000000000000000000000000000000000ffffc0000233208d"
  "RawNetworkIpAddressSerializer" must "read a network ip address from a hex string" in {
    val ipAddress = RawNetworkIpAddressSerializer.read(hex)
    ipAddress.time must be (1414012889)
    ipAddress.services must be (NodeNetwork)
    ipAddress.address.toString must be ("/192.0.2.51")
    ipAddress.port must be (8333)
  }

  it must "write a network ip address from and get its original hex back" in {
    val ipAddress = RawNetworkIpAddressSerializer.read(hex)
    RawNetworkIpAddressSerializer.write(ipAddress) must be (hex)
  }
}
