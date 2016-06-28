package org.bitcoins.spvnode.messages.control

import org.bitcoins.core.util.BitcoinSLogger
import org.bitcoins.spvnode.gen.ControlMessageGenerator
import org.scalacheck.{Prop, Properties}

/**
  * Created by chris on 6/27/16.
  */
class VersionMessageSpec extends Properties("VersionMessageSpec") with BitcoinSLogger {

  property("Serialization symmetry") =
    Prop.forAll(ControlMessageGenerator.versionMessage) { versionMessage =>
      logger.info("Version message: " + versionMessage)
      logger.info("Version message hex: " + versionMessage.hex)
      VersionMessage(versionMessage.hex) == versionMessage

    }

}
