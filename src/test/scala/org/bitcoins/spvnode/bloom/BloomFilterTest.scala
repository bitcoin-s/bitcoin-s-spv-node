package org.bitcoins.spvnode.bloom

import org.bitcoins.core.crypto.{DoubleSha256Digest, ECPrivateKey, ECPublicKey, Sha256Hash160Digest}
import org.bitcoins.core.gen.CryptoGenerators
import org.bitcoins.core.number.UInt32
import org.bitcoins.core.protocol.transaction.{Transaction, TransactionOutPoint}
import org.bitcoins.core.util.{BitcoinSLogger, BitcoinSUtil, CryptoUtil}
import org.scalatest.{FlatSpec, MustMatchers}

/**
  * Created by chris on 8/3/16.
  */
class BloomFilterTest extends FlatSpec with MustMatchers with BitcoinSLogger {

  "BloomFilter" must "create a bloom filter, insert a few elements, then serialize it" in {
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

  it must "create a bloom filter with a tweak then insert elements and serialize it" in {
    //mimics this test case from core https://github.com/bitcoin/bitcoin/blob/master/src/test/bloom_tests.cpp#L59
    val filter = BloomFilter(3,0.01, UInt32(2147483649L), BloomUpdateAll)

    val hash1 = DoubleSha256Digest("99108ad8ed9bb6274d3980bab5a85c048f0950c8")
    val filter1 = filter.insert(hash1)
    filter1.contains(hash1) must be (true)
    //one bit different
    filter1.contains(DoubleSha256Digest("19108ad8ed9bb6274d3980bab5a85c048f0950c8")) must be (false)

    val hash2 = DoubleSha256Digest("b5a2c786d9ef4658287ced5914b37a1b4aa32eee")
    val filter2 = filter1.insert(hash2)
    filter2.contains(hash2) must be (true)

    val hash3 = DoubleSha256Digest("b9300670b4c5366e95b2699e8b18bc75e5f729c5")
    val filter3 = filter2.insert(hash3)
    filter3.contains(hash3) must be (true)

    filter3.hex must be ("03ce4299050000000100008001")
  }

  it must "insert a key & it's address into our bloom filter and the check to make sure it contains them" in {
    val filter = BloomFilter(2,0.001, UInt32.zero, BloomUpdateAll)
    val privKey = ECPrivateKey.fromWIFToPrivateKey("5Kg1gnAjaLfKiwhhPpGS3QfRg2m6awQvaj98JCZBZQ5SuS2F15C")
    logger.debug("PrivKey: " + privKey.hex)
    require(privKey.hex == "f49addfd726a59abde172c86452f5f73038a02f4415878dc14934175e8418aff")
    val pubKey = privKey.publicKey
    logger.debug("PubKey being inserted into filter: " + pubKey.hex)
    val filter1 = filter.insert(pubKey.bytes)
    //hex is from bitcoin core
    filter1.hex must be ("0302c12b080000000000000001")
    val keyId = CryptoUtil.sha256Hash160(pubKey.bytes)
    val filter2 = filter1.insert(keyId.bytes)

    filter2.hex must be ("038fc16b080000000000000001")
  }

  it must "test the isRelevant part of isRelevantAndUpdate inside of core" in {
    //mimics this test case in core
    //https://github.com/bitcoin/bitcoin/blob/master/src/test/bloom_tests.cpp#L114
    val creditingTx = Transaction("01000000010b26e9b7735eb6aabdf358bab62f9816a21ba9ebdb719d5299e88607d722c190000000008b4830450220070aca44506c5cef3a16ed519d7c3c39f8aab192c4e1c90d065f37b8a4af6141022100a8e160b856c2d43d27d8fba71e5aef6405b8643ac4cb7cb3c462aced7f14711a0141046d11fee51b0e60666d5049a9101a72741df480b96ee26488a4d3466b95c9a40ac5eeef87e10a5cd336c19a84565f80fa6c547957b7700ff4dfbdefe76036c339ffffffff021bff3d11000000001976a91404943fdd508053c75000106d3bc6e2754dbcff1988ac2f15de00000000001976a914a266436d2965547608b9e15d9032a7b9d64fa43188ac00000000")

    val spendingTx = Transaction("01000000016bff7fcd4f8565ef406dd5d63d4ff94f318fe82027fd4dc451b04474019f74b4000000008c493046022100da0dc6aecefe1e06efdf05773757deb168820930e3b0d03f46f5fcf150bf990c022100d25b5c87040076e4f253f8262e763e2dd51e7ff0be157727c4bc42807f17bd39014104e6c26ef67dc610d2cd192484789a6cf9aea9930b944b7e2db5342b9d9e5b9ff79aff9a2ee1978dd7fd01dfc522ee02283d3b06a9d03acf8096968d7dbb0f9178ffffffff028ba7940e000000001976a914badeecfdef0507247fc8f74241d73bc039972d7b88ac4094a802000000001976a914c10932483fec93ed51f5fe95e72559f2cc7043f988ac00000000")

    val filter = BloomFilter(10, 0.000001, UInt32.zero, BloomUpdateAll)

    val filter1 = filter.insert(creditingTx.txId)

    filter1.isRelevant(creditingTx) must be (true)

    val filter2 = BloomFilter(10, 0.000001, UInt32.zero, BloomUpdateAll)

    //byte reversed tx hash
    val filter3 = filter2.insert(DoubleSha256Digest("6bff7fcd4f8565ef406dd5d63d4ff94f318fe82027fd4dc451b04474019f74b4"))
    filter3.isRelevant(creditingTx) must be (true)

    val filter4 = BloomFilter(10, 0.000001, UInt32.zero, BloomUpdateAll)
    //insert a digital signature in our bloom filter for the spendingTx
    val filter5 = filter4.insert(BitcoinSUtil.decodeHex("30450220070aca44506c5cef3a16ed519d7c3c39f8aab192c4e1c90d065f37b8a4af6141022100a8e160b856c2d43d27d8fba71e5aef6405b8643ac4cb7cb3c462aced7f14711a01"))
    filter5.isRelevant(creditingTx) must be (true)

    val filter6 = BloomFilter(10, 0.000001, UInt32.zero, BloomUpdateAll)
    //insert the pubkey of spendingTx in the bloom filter
    val pubKey = ECPublicKey("046d11fee51b0e60666d5049a9101a72741df480b96ee26488a4d3466b95c9a40ac5eeef87e10a5cd336c19a84565f80fa6c547957b7700ff4dfbdefe76036c339")
    val filter7 = filter6.insert(pubKey.bytes)
    filter7.isRelevant(creditingTx) must be (true)




    val filter8 = BloomFilter(10, 0.000001, UInt32.zero, BloomUpdateAll)
    val filter9 = filter8.insert(Sha256Hash160Digest("04943fdd508053c75000106d3bc6e2754dbcff19"))
    filter9.isRelevant(creditingTx) must be (true)
    //update the bloom filter to add the outputs inside of the crediting tx
    //this is what the core test case really does, but since we separated the isRelevant and update parts, we need
    //to call update explicitly
    val filter10 = filter9.update(creditingTx)
    filter10.isRelevant(spendingTx) must be (true)

    val filter11 = BloomFilter(10, 0.000001, UInt32.zero, BloomUpdateAll)
    val filter12 = filter11.insert(Sha256Hash160Digest("a266436d2965547608b9e15d9032a7b9d64fa431"))
    filter12.isRelevant(creditingTx) must be (true)


    val filter13 = BloomFilter(10, 0.000001, UInt32.zero, BloomUpdateAll)
    val hash = Sha256Hash160Digest("a266436d2965547608b9e15d9032a7b9d64fa431")
    val filter14 = filter13.insert(hash)
    filter14.contains(hash) must be (true)

    val filter15 = BloomFilter(10, 0.000001, UInt32.zero, BloomUpdateAll)
    val outPoint = TransactionOutPoint(DoubleSha256Digest(BitcoinSUtil.flipEndianess("90c122d70786e899529d71dbeba91ba216982fb6ba58f3bdaab65e73b7e9260b")), UInt32.zero)
    val filter16 = filter15.insert(outPoint)
    filter16.hex must be ("230008000000000100000000200040304001000020000000100800050801000400800024130000000000000001")
    filter16.isRelevant(creditingTx) must be (true)

    val filter17 = BloomFilter(10, 0.000001, UInt32.zero, BloomUpdateAll)
    //random tx hash
    val filter18 = filter17.insert(DoubleSha256Digest("00000009e784f32f62ef849763d4f45b98e07ba658647343b915ff832b110436"))
    filter18.isRelevant(creditingTx) must be (false)

    val filter19 = BloomFilter(10, 0.000001, UInt32.zero, BloomUpdateAll)
    //makes sure filter does not match a random outpoint
    val randomOutPoint = TransactionOutPoint(DoubleSha256Digest("90c122d70786e899529d71dbeba91ba216982fb6ba58f3bdaab65e73b7e9260b"), UInt32.one)
    val filter20 = filter19.insert(randomOutPoint)
    filter20.isRelevant(creditingTx) must be (false)

    val filter21 = BloomFilter(10, 0.000001, UInt32.zero, BloomUpdateAll)
    val secondRandomOutPoint = TransactionOutPoint(DoubleSha256Digest(BitcoinSUtil.flipEndianess("000000d70786e899529d71dbeba91ba216982fb6ba58f3bdaab65e73b7e9260b")), UInt32.zero)
    val filter22 = filter21.insert(secondRandomOutPoint)
    filter22.hex must be ("230090f00000004000000005040000000004000400000000100101000000008002040000130000000000000001")
    filter22.isRelevant(creditingTx) must be (false)
  }



}
