package org.bitcoins.spvnode.gen

import org.bitcoins.core.gen.CryptoGenerators
import org.bitcoins.spvnode.messages.GetHeadersMessage
import org.bitcoins.spvnode.messages.data.GetHeadersMessage
import org.scalacheck.Gen

/**
  * Created by chris on 6/29/16.
  */
trait DataMessageGenerator {


  def getHeaderMessages: Gen[GetHeadersMessage] = for {
    version <- ControlMessageGenerator.protocolVersion
    numHashes <- Gen.choose(0,2000)
    hashes <- CryptoGenerators.doubleSha256DigestSeq(numHashes)
    hashStop <- CryptoGenerators.doubleSha256Digest
  } yield GetHeadersMessage(version,hashes,hashStop)
}

object DataMessageGenerator extends DataMessageGenerator
