package org.bitcoins.spvnode.bloom

import org.bitcoins.core.crypto.{DoubleSha256Digest, ECPrivateKey, ECPublicKey, Sha256Hash160Digest}
import org.bitcoins.core.gen.CryptoGenerators
import org.bitcoins.core.number.UInt32
import org.bitcoins.core.protocol.blockchain.Block
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

  it must "match transactions from inside of a merkle block" in {
    //mimics this test suite inside of core
    //https://github.com/bitcoin/bitcoin/blob/master/src/test/bloom_tests.cpp#L185
    val block = Block("0100000090f0a9f110702f808219ebea1173056042a714bad51b916cb6800000000000005275289558f51c9966699404ae2294730c3c9f9bda53523ce50e9b95e558da2fdb261b4d4c86041b1ab1bf930901000000010000000000000000000000000000000000000000000000000000000000000000ffffffff07044c86041b0146ffffffff0100f2052a01000000434104e18f7afbe4721580e81e8414fc8c24d7cfacf254bb5c7b949450c3e997c2dc1242487a8169507b631eb3771f2b425483fb13102c4eb5d858eef260fe70fbfae0ac00000000010000000196608ccbafa16abada902780da4dc35dafd7af05fa0da08cf833575f8cf9e836000000004a493046022100dab24889213caf43ae6adc41cf1c9396c08240c199f5225acf45416330fd7dbd022100fe37900e0644bf574493a07fc5edba06dbc07c311b947520c2d514bc5725dcb401ffffffff0100f2052a010000001976a914f15d1921f52e4007b146dfa60f369ed2fc393ce288ac000000000100000001fb766c1288458c2bafcfec81e48b24d98ec706de6b8af7c4e3c29419bfacb56d000000008c493046022100f268ba165ce0ad2e6d93f089cfcd3785de5c963bb5ea6b8c1b23f1ce3e517b9f022100da7c0f21adc6c401887f2bfd1922f11d76159cbc597fbd756a23dcbb00f4d7290141042b4e8625a96127826915a5b109852636ad0da753c9e1d5606a50480cd0c40f1f8b8d898235e571fe9357d9ec842bc4bba1827daaf4de06d71844d0057707966affffffff0280969800000000001976a9146963907531db72d0ed1a0cfb471ccb63923446f388ac80d6e34c000000001976a914f0688ba1c0d1ce182c7af6741e02658c7d4dfcd388ac000000000100000002c40297f730dd7b5a99567eb8d27b78758f607507c52292d02d4031895b52f2ff010000008b483045022100f7edfd4b0aac404e5bab4fd3889e0c6c41aa8d0e6fa122316f68eddd0a65013902205b09cc8b2d56e1cd1f7f2fafd60a129ed94504c4ac7bdc67b56fe67512658b3e014104732012cb962afa90d31b25d8fb0e32c94e513ab7a17805c14ca4c3423e18b4fb5d0e676841733cb83abaf975845c9f6f2a8097b7d04f4908b18368d6fc2d68ecffffffffca5065ff9617cbcba45eb23726df6498a9b9cafed4f54cbab9d227b0035ddefb000000008a473044022068010362a13c7f9919fa832b2dee4e788f61f6f5d344a7c2a0da6ae740605658022006d1af525b9a14a35c003b78b72bd59738cd676f845d1ff3fc25049e01003614014104732012cb962afa90d31b25d8fb0e32c94e513ab7a17805c14ca4c3423e18b4fb5d0e676841733cb83abaf975845c9f6f2a8097b7d04f4908b18368d6fc2d68ecffffffff01001ec4110200000043410469ab4181eceb28985b9b4e895c13fa5e68d85761b7eee311db5addef76fa8621865134a221bd01f28ec9999ee3e021e60766e9d1f3458c115fb28650605f11c9ac000000000100000001cdaf2f758e91c514655e2dc50633d1e4c84989f8aa90a0dbc883f0d23ed5c2fa010000008b48304502207ab51be6f12a1962ba0aaaf24a20e0b69b27a94fac5adf45aa7d2d18ffd9236102210086ae728b370e5329eead9accd880d0cb070aea0c96255fae6c4f1ddcce1fd56e014104462e76fd4067b3a0aa42070082dcb0bf2f388b6495cf33d789904f07d0f55c40fbd4b82963c69b3dc31895d0c772c812b1d5fbcade15312ef1c0e8ebbb12dcd4ffffffff02404b4c00000000001976a9142b6ba7c9d796b75eef7942fc9288edd37c32f5c388ac002d3101000000001976a9141befba0cdc1ad56529371864d9f6cb042faa06b588ac000000000100000001b4a47603e71b61bc3326efd90111bf02d2f549b067f4c4a8fa183b57a0f800cb010000008a4730440220177c37f9a505c3f1a1f0ce2da777c339bd8339ffa02c7cb41f0a5804f473c9230220585b25a2ee80eb59292e52b987dad92acb0c64eced92ed9ee105ad153cdb12d001410443bd44f683467e549dae7d20d1d79cbdb6df985c6e9c029c8d0c6cb46cc1a4d3cf7923c5021b27f7a0b562ada113bc85d5fda5a1b41e87fe6e8802817cf69996ffffffff0280651406000000001976a9145505614859643ab7b547cd7f1f5e7e2a12322d3788ac00aa0271000000001976a914ea4720a7a52fc166c55ff2298e07baf70ae67e1b88ac00000000010000000586c62cd602d219bb60edb14a3e204de0705176f9022fe49a538054fb14abb49e010000008c493046022100f2bc2aba2534becbdf062eb993853a42bbbc282083d0daf9b4b585bd401aa8c9022100b1d7fd7ee0b95600db8535bbf331b19eed8d961f7a8e54159c53675d5f69df8c014104462e76fd4067b3a0aa42070082dcb0bf2f388b6495cf33d789904f07d0f55c40fbd4b82963c69b3dc31895d0c772c812b1d5fbcade15312ef1c0e8ebbb12dcd4ffffffff03ad0e58ccdac3df9dc28a218bcf6f1997b0a93306faaa4b3a28ae83447b2179010000008b483045022100be12b2937179da88599e27bb31c3525097a07cdb52422d165b3ca2f2020ffcf702200971b51f853a53d644ebae9ec8f3512e442b1bcb6c315a5b491d119d10624c83014104462e76fd4067b3a0aa42070082dcb0bf2f388b6495cf33d789904f07d0f55c40fbd4b82963c69b3dc31895d0c772c812b1d5fbcade15312ef1c0e8ebbb12dcd4ffffffff2acfcab629bbc8685792603762c921580030ba144af553d271716a95089e107b010000008b483045022100fa579a840ac258871365dd48cd7552f96c8eea69bd00d84f05b283a0dab311e102207e3c0ee9234814cfbb1b659b83671618f45abc1326b9edcc77d552a4f2a805c0014104462e76fd4067b3a0aa42070082dcb0bf2f388b6495cf33d789904f07d0f55c40fbd4b82963c69b3dc31895d0c772c812b1d5fbcade15312ef1c0e8ebbb12dcd4ffffffffdcdc6023bbc9944a658ddc588e61eacb737ddf0a3cd24f113b5a8634c517fcd2000000008b4830450221008d6df731df5d32267954bd7d2dda2302b74c6c2a6aa5c0ca64ecbabc1af03c75022010e55c571d65da7701ae2da1956c442df81bbf076cdbac25133f99d98a9ed34c014104462e76fd4067b3a0aa42070082dcb0bf2f388b6495cf33d789904f07d0f55c40fbd4b82963c69b3dc31895d0c772c812b1d5fbcade15312ef1c0e8ebbb12dcd4ffffffffe15557cd5ce258f479dfd6dc6514edf6d7ed5b21fcfa4a038fd69f06b83ac76e010000008b483045022023b3e0ab071eb11de2eb1cc3a67261b866f86bf6867d4558165f7c8c8aca2d86022100dc6e1f53a91de3efe8f63512850811f26284b62f850c70ca73ed5de8771fb451014104462e76fd4067b3a0aa42070082dcb0bf2f388b6495cf33d789904f07d0f55c40fbd4b82963c69b3dc31895d0c772c812b1d5fbcade15312ef1c0e8ebbb12dcd4ffffffff01404b4c00000000001976a9142b6ba7c9d796b75eef7942fc9288edd37c32f5c388ac00000000010000000166d7577163c932b4f9690ca6a80b6e4eb001f0a2fa9023df5595602aae96ed8d000000008a4730440220262b42546302dfb654a229cefc86432b89628ff259dc87edd1154535b16a67e102207b4634c020a97c3e7bbd0d4d19da6aa2269ad9dded4026e896b213d73ca4b63f014104979b82d02226b3a4597523845754d44f13639e3bf2df5e82c6aab2bdc79687368b01b1ab8b19875ae3c90d661a3d0a33161dab29934edeb36aa01976be3baf8affffffff02404b4c00000000001976a9144854e695a02af0aeacb823ccbc272134561e0a1688ac40420f00000000001976a914abee93376d6b37b5c2940655a6fcaf1c8e74237988ac0000000001000000014e3f8ef2e91349a9059cb4f01e54ab2597c1387161d3da89919f7ea6acdbb371010000008c49304602210081f3183471a5ca22307c0800226f3ef9c353069e0773ac76bb580654d56aa523022100d4c56465bdc069060846f4fbf2f6b20520b2a80b08b168b31e66ddb9c694e240014104976c79848e18251612f8940875b2b08d06e6dc73b9840e8860c066b7e87432c477e9a59a453e71e6d76d5fe34058b800a098fc1740ce3012e8fc8a00c96af966ffffffff02c0e1e400000000001976a9144134e75a6fcb6042034aab5e18570cf1f844f54788ac404b4c00000000001976a9142b6ba7c9d796b75eef7942fc9288edd37c32f5c388ac00000000")
    block.blockHeader.hash.hex must be (BitcoinSUtil.flipEndianess("0000000000013b8ab2cd513b0261a14096412195a72a0c4827d229dcc7e0f7af"))

    val filter = BloomFilter(10, 0.000001, UInt32.zero, BloomUpdateAll)
    val lastTxInBlock = DoubleSha256Digest(BitcoinSUtil.flipEndianess("74d681e0e03bafa802c8aa084379aa98d9fcd632ddc2ed9782b586ec87451f20"))
    val filter1 = filter.insert(lastTxInBlock)



  }


}
