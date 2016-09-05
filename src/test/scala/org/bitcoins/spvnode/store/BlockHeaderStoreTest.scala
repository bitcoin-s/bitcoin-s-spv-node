package org.bitcoins.spvnode.store

import org.bitcoins.core.gen.BlockchainElementsGenerator
import org.scalatest.{BeforeAndAfter, FlatSpec, MustMatchers}

/**
  * Created by chris on 9/5/16.
  */
class BlockHeaderStoreTest extends FlatSpec with MustMatchers with BeforeAndAfter {
  val file = new java.io.File("src/test/resources/block_header.dat")
  "BlockHeaderStore" must "write and then read a block header from the database" in {
    val blockHeader = BlockchainElementsGenerator.blockHeader.sample.get
    BlockHeaderStore.append(Seq(blockHeader),file)
    val headersFromFile = BlockHeaderStore.read(file)

    headersFromFile must be (Seq(blockHeader))
  }


  after {
    file.delete()
  }

}
