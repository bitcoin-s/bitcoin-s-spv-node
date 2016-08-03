package org.bitcoins.spvnode.bloom

import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.gen.CryptoGenerators
import org.bitcoins.core.number.UInt32
import org.scalatest.{FlatSpec, MustMatchers}

/**
  * Created by chris on 8/3/16.
  */
class BloomFilterTest extends FlatSpec with MustMatchers {

  "BloomFilter" must "load one hash into the bloom filter, then check to see if the filter contains it" in {
    //test case in bitcoin core
    //https://github.com/bitcoin/bitcoin/blob/master/src/test/bloom_tests.cpp#L28
    val filter = BloomFilter(3, 0.01, UInt32.zero, BloomUpdateAll)
    val hash = DoubleSha256Digest("99108ad8ed9bb6274d3980bab5a85c048f0950c8")
    val newFilter = filter.insert(hash)
    newFilter.contains(hash) must be (true)
  }
}
