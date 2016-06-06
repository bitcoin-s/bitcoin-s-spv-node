package org.bitcoins.spvnode.messages.control

import java.net.InetAddress

import org.bitcoins.core.config.MainNet
import org.joda.time.DateTime
import org.bitcoins.spvnode.BuildInfo
import org.scalatest.{FlatSpec, MustMatchers}

/**
  * Created by chris on 6/6/16.
  */
class VersionMessageTest extends FlatSpec with MustMatchers {

  "VersionMessage" must "create a new version message to be sent to another node on the network" in {
    val versionMessage = VersionMessage(MainNet, InetAddress.getLocalHost)
    versionMessage.addressReceiveServices must be (UnnamedService)
    versionMessage.addressReceiveIpAddress must be (InetAddress.getLocalHost)
    versionMessage.addressReceivePort must be (MainNet.port)

    versionMessage.addressTransServices must be (NodeNetwork)
    versionMessage.addressTransIpAddress must be (InetAddress.getLocalHost)
    versionMessage.addressTransPort must be (MainNet.port)

    versionMessage.nonce must be (0)
    versionMessage.startHeight must be (0)
    versionMessage.timestamp must be (DateTime.now.getMillis +- 1000)
    versionMessage.userAgent must be ("/" + BuildInfo.name + "/" + BuildInfo.version)
  }
}
