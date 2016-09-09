package org.bitcoins.spvnode.modelsd

import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.number.UInt32
import org.bitcoins.core.protocol.blockchain.BlockHeader
import org.bitcoins.spvnode.models.ColumnMappers
import slick.driver.PostgresDriver.api._
/**
  * Created by chris on 9/7/16.
  */
class BlockHeaderTable(tag: Tag) extends Table[BlockHeader](tag,"block_headers")  {
  import ColumnMappers._
  def hash = column[DoubleSha256Digest]("hash", O.PrimaryKey)

  def version = column[UInt32]("version")

  def previousBlockHash = column[DoubleSha256Digest]("previous_block_hash")

  def merkleRootHash = column[DoubleSha256Digest]("merkle_root_hash")

  def time = column[UInt32]("time")

  def nBits = column[UInt32]("n_bits")

  def nonce = column[UInt32]("nonce")

  def * = (hash, version, previousBlockHash, merkleRootHash, time, nBits, nonce).<>[BlockHeader,
    (DoubleSha256Digest, UInt32, DoubleSha256Digest, DoubleSha256Digest, UInt32, UInt32, UInt32)](blockHeaderApply,blockHeaderUnapply)

  /** Creates a block header from a tuple */
  private val blockHeaderApply : ((DoubleSha256Digest, UInt32, DoubleSha256Digest, DoubleSha256Digest, UInt32, UInt32, UInt32)) => BlockHeader = {
    case (hash: DoubleSha256Digest, version: UInt32, previousBlockHash: DoubleSha256Digest, merkleRootHash: DoubleSha256Digest, time: UInt32,
      nBits: UInt32, nonce: UInt32) =>
      val header = BlockHeader(version,previousBlockHash,merkleRootHash, time,nBits,nonce)
      require(header.hash == hash, "Block header is not giving us the same hash that was stored in the database, " +
        "got: " + header.hash + " expected: " + hash)
      header
  }

  /** Destructs a block header to a tuple */
  private val blockHeaderUnapply: BlockHeader => Option[(DoubleSha256Digest, UInt32, DoubleSha256Digest, DoubleSha256Digest, UInt32, UInt32, UInt32)] = {
    blockHeader: BlockHeader =>
      Some((blockHeader.hash, blockHeader.version, blockHeader.previousBlockHash, blockHeader.merkleRootHash,
        blockHeader.time,blockHeader.nBits,blockHeader.nonce))
  }


}
