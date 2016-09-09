package org.bitcoins.spvnode.models

import akka.actor.{ActorRef, ActorRefFactory, Props}
import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.protocol.blockchain.BlockHeader
import org.bitcoins.spvnode.constant.Constants
import org.bitcoins.spvnode.modelsd.BlockHeaderTable
import org.bitcoins.spvnode.util.BitcoinSpvNodeUtil
import slick.driver.PostgresDriver.api._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

/**
  * Created by chris on 9/8/16.
  */
sealed trait BlockHeaderDAO extends CRUDActor[BlockHeader,DoubleSha256Digest] {

  def receive = {
    case createMsg: BlockHeaderDAO.Create =>
      val createdBlockHeader = create(createMsg.blockHeader)
      createdBlockHeader.map(blockHeader => context.parent ! blockHeader)
    case readMsg: BlockHeaderDAO.Read =>
      val readHeader = read(readMsg.hash)
      readHeader.map(headerOpt => context.parent ! headerOpt)
    case deleteMsg: BlockHeaderDAO.Delete =>
      val deletedRowCount = delete(deleteMsg.blockHeader)
      deletedRowCount.map(rowCount => context.parent ! rowCount)
    case _ => throw new IllegalArgumentException
  }

  override val table = TableQuery[BlockHeaderTable]

  def create(blockHeader: BlockHeader): Future[BlockHeader] = {
    val action = (table += blockHeader).andThen(DBIO.successful(blockHeader))
    database.run(action)
  }

  def find(blockHeader: BlockHeader): Query[Table[_],  BlockHeader, Seq] = findByPrimaryKey(blockHeader.hash)

  def findByPrimaryKey(hash : DoubleSha256Digest): Query[Table[_], BlockHeader, Seq] = {
    import ColumnMappers._
    table.filter(_.hash === hash)
  }
}


object BlockHeaderDAO {
  sealed trait BlockHeaderDAOMessage
  case class Create(blockHeader: BlockHeader) extends BlockHeaderDAOMessage
  case class Read(hash: DoubleSha256Digest) extends BlockHeaderDAOMessage
  case class Update(blockHeader: BlockHeader) extends BlockHeaderDAOMessage
  case class Upsert(blockHeader: BlockHeader) extends BlockHeaderDAOMessage
  case class Delete(blockHeader: BlockHeader) extends BlockHeaderDAOMessage

  private case class BlockHeaderDAOImpl() extends BlockHeaderDAO

  def props = Props(BlockHeaderDAOImpl())

  def apply(context: ActorRefFactory): ActorRef = context.actorOf(props,
    BitcoinSpvNodeUtil.createActorName(BlockHeaderDAO.getClass))

  def apply: ActorRef = BlockHeaderDAO(Constants.actorSystem)
}
