package org.bitcoins.spvnode.bloom

import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.gen.CryptoGenerators
import org.bitcoins.core.number.UInt32
import org.bitcoins.core.util.BitcoinSLogger
import org.scalatest.{FlatSpec, MustMatchers}

/**
  * Created by chris on 8/3/16.
  */
class BloomFilterTest extends FlatSpec with MustMatchers with BitcoinSLogger {

  "BloomFilter" must "load one hash into the bloom filter, then check to see if the filter contains it" in {
    //test case in bitcoin core
    //https://github.com/bitcoin/bitcoin/blob/master/src/test/bloom_tests.cpp#L28
    val filter = BloomFilter(3, 0.01, UInt32.zero, BloomUpdateAll)
    //hex is from bitcoin core
    filter.hex must be ("03000000050000000000000001")
    val hash = DoubleSha256Digest("99108ad8ed9bb6274d3980bab5a85c048f0950c8")
    val newFilter = filter.insert(hash)
    //hex from bitcoin core
    newFilter.hex must be ("03010098050000000000000001")
    newFilter.contains(hash) must be (true)

    val hash1BitDifferent = DoubleSha256Digest("19108ad8ed9bb6274d3980bab5a85c048f0950c8")
    newFilter.contains(hash1BitDifferent) must be (false)

    val hash1 = DoubleSha256Digest("b5a2c786d9ef4658287ced5914b37a1b4aa32eee")
    val filter2 = newFilter.insert(hash1)

    filter2.contains(hash1) must be (true)

    val hash2 = DoubleSha256Digest("b9300670b4c5366e95b2699e8b18bc75e5f729c5")
    val filter3 = filter2.insert(hash2)
    filter3.contains(hash2) must be (true)

    filter3.hex must be ("03614e9b050000000000000001")
    
  }
}
