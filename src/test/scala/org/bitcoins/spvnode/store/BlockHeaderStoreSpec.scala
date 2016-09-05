package org.bitcoins.spvnode.store

import org.bitcoins.core.gen.BlockchainElementsGenerator
import org.bitcoins.core.protocol.blockchain.BlockHeader
import org.scalacheck.{Gen, Prop, Properties}

/**
  * Created by chris on 9/5/16.
  */
class BlockHeaderStoreSpec extends Properties("BlockHeaderStoreSpec") {
  val file = new java.io.File("src/test/resources/block_header_spec.dat")
  property("serialization symmetry to file") =
    Prop.forAll(Gen.listOf(BlockchainElementsGenerator.blockHeader)) { case headers : Seq[BlockHeader] =>
      BlockHeaderStore.append(headers,file)
      val headersFromFile = BlockHeaderStore.read(file)
      val result = headersFromFile == headers
      file.delete()
      result
    }
}
