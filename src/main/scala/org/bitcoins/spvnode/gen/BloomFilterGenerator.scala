package org.bitcoins.spvnode.gen

import org.bitcoins.core.gen.{CryptoGenerators, NumberGenerator}
import org.bitcoins.core.number.UInt32
import org.bitcoins.spvnode.bloom._
import org.scalacheck.Gen

/**
  * Created by chris on 8/7/16.
  */
trait BloomFilterGenerator {

  def bloomFilter: Gen[BloomFilter] = for {
    size <- Gen.choose(1,100)
    falsePositiveRate <- Gen.choose(0.00001, 0.99999)
    tweak <- NumberGenerator.uInt32s
    flags <- bloomFlag
  } yield BloomFilter(size,falsePositiveRate, tweak, flags)

  def loadedBloomFilter: Gen[(BloomFilter,Seq[Seq[Byte]])] = for {
    filter <- bloomFilter
    randomNum <- Gen.choose(0,filter.filterSize.num.toInt)
    hashes <- CryptoGenerators.doubleSha256DigestSeq(randomNum)
    loaded = filter.insertHashes(hashes)
  } yield (loaded,hashes.map(_.bytes))

  def bloomFlag: Gen[BloomFlag] = for {
    randomNum <- Gen.choose(0,2)
  } yield {
    if (randomNum == 0) BloomUpdateNone
    else if (randomNum == 1) BloomUpdateAll
    else BloomUpdateP2PubKeyOnly
  }
}

object BloomFilterGenerator extends BloomFilterGenerator
