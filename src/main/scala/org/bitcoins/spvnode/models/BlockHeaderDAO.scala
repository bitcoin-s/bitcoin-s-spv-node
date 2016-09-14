package org.bitcoins.spvnode.models

import akka.actor.{ActorRef, ActorRefFactory, Props}
import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.protocol.blockchain.BlockHeader
import org.bitcoins.spvnode.constant.Constants
import org.bitcoins.spvnode.modelsd.BlockHeaderTable
import org.bitcoins.spvnode.util.BitcoinSpvNodeUtil
import slick.driver.PostgresDriver.api._
import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Created by chris on 9/8/16.
  * This actor is responsible for all database operations relating to
  * [[BlockHeader]]'s. Currently we store all block headers in a postgresql database
  */
sealed trait BlockHeaderDAO extends CRUDActor[BlockHeader,DoubleSha256Digest] {

  override val table = TableQuery[BlockHeaderTable]

  def receive = {
    case createMsg: BlockHeaderDAO.Create =>
      val createdBlockHeader = create(createMsg.blockHeader).map(BlockHeaderDAO.CreatedHeader(_))(context.dispatcher)
      sendToParent(createdBlockHeader)
    case createAllMsg: BlockHeaderDAO.CreateAll =>
      val createAllHeaders = createAll(createAllMsg.blockHeaders).map(BlockHeaderDAO.CreatedHeaders(_))(context.dispatcher)
      sendToParent(createAllHeaders)
    case readMsg: BlockHeaderDAO.Read =>
      val readHeader = read(readMsg.hash)
      sendToParent(readHeader)
    case deleteMsg: BlockHeaderDAO.Delete =>
      val deletedRowCount = delete(deleteMsg.blockHeader)
      sendToParent(deletedRowCount)
  }


  /** Sends a message to our parent actor */
  private def sendToParent(returnMsg: Future[Any]): Unit = returnMsg.onComplete {
    case Success(msg) =>
      context.parent ! msg
      //context.stop(self)
    case Failure(exception) =>
      //means the future did not complete successfully, we encountered an error somewhere
      logger.error("Exception: " + exception.toString)
      throw exception
  }(context.dispatcher)

  def create(blockHeader: BlockHeader): Future[BlockHeader] = {
    val action = (table += blockHeader).andThen(DBIO.successful(blockHeader))
    database.run(action)
  }

  def createAll(headers: Seq[BlockHeader]): Future[Seq[BlockHeader]] = {
    val actions = table ++= headers
    val bulkInsert = DBIO.seq(actions).andThen(DBIO.successful(headers))
    database.run(bulkInsert)
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
  case class CreateAll(blockHeaders: Seq[BlockHeader]) extends BlockHeaderDAOMessage
  case class Read(hash: DoubleSha256Digest) extends BlockHeaderDAOMessage
  case class Delete(blockHeader: BlockHeader) extends BlockHeaderDAOMessage

  sealed trait BlockHeaderDAOMessageReplies extends BlockHeaderDAOMessage
  case class CreatedHeader(header: BlockHeader) extends BlockHeaderDAOMessageReplies
  case class CreatedHeaders(headers: Seq[BlockHeader]) extends BlockHeaderDAOMessageReplies


  private case class BlockHeaderDAOImpl(database: Database) extends BlockHeaderDAO

  def props(database: Database): Props = Props(BlockHeaderDAOImpl(database))

  def apply(context: ActorRefFactory, database: Database): ActorRef = context.actorOf(props(database),
    BitcoinSpvNodeUtil.createActorName(BlockHeaderDAO.getClass))

  def apply(database: Database): ActorRef = BlockHeaderDAO(Constants.actorSystem,database)

  def apply: ActorRef = BlockHeaderDAO(Constants.actorSystem,Constants.database)
}
