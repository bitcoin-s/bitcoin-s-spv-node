package org.bitcoins.spvnode.gen

import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.gen.{BlockchainElementsGenerator, CryptoGenerators}
import org.bitcoins.spvnode.block.{MerkleBlock, PartialMerkleTree}
import org.scalacheck.Gen

/**
  * Created by chris on 8/12/16.
  */
trait MerkleGenerator {

  /** Returns a [[MerkleBlock]] including the sequence of hashes inserted in to the bloom filter */
  def merkleBlockWithInsertedTxIds: Gen[(MerkleBlock,Seq[DoubleSha256Digest])] = for {
    block <- BlockchainElementsGenerator.block
    //choose some random txs in the block to put in the bloom filter
    txIds <- Gen.someOf(block.transactions.map(_.txId))
    filter <- BloomFilterGenerator.bloomFilter(txIds.map(_.bytes))
    (merkleBlock, _) = MerkleBlock(block,filter)
  } yield (merkleBlock,txIds)

  /** Generates a partial merkle tree with a sequence of txids and a flag indicating if the txid was matched */
  def partialMerkleTree: Gen[(PartialMerkleTree, Seq[(Boolean,DoubleSha256Digest)])] = for {
    randomNum <- Gen.choose(1,25)
    txMatches <- txIdsWithMatchIndication(randomNum)
  } yield (PartialMerkleTree(txMatches),txMatches)

  /** Generates a transaction ids with a boolean indicator if they match the bloom filter or not
    * this is useful for testing partial merkle trees as this is how they are built.
    * @return
    */
  private def txIdWithMatchIndication: Gen[(Boolean,DoubleSha256Digest)] = for {
    hash <- CryptoGenerators.doubleSha256Digest
    bool <- Gen.choose(0,1)
  } yield (bool == 1, hash)

  /** Generates a list of txids with a boolean indicator signifying if it matched the bloom filter or not */
  def txIdsWithMatchIndication(num: Int): Gen[Seq[(Boolean,DoubleSha256Digest)]] = Gen.listOfN(num,txIdWithMatchIndication)
}

object MerkleGenerator extends MerkleGenerator