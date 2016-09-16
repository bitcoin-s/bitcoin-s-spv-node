package org.bitcoins.spvnode.models

import akka.actor.{ActorRef, ActorRefFactory, Props}
import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.protocol.blockchain.BlockHeader
import org.bitcoins.spvnode.constant.Constants
import org.bitcoins.spvnode.models.BlockHeaderDAO.BlockHeaderDAOMessage
import org.bitcoins.spvnode.modelsd.BlockHeaderTable
import org.bitcoins.spvnode.util.BitcoinSpvNodeUtil
import slick.driver.PostgresDriver.api._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
  * Created by chris on 9/8/16.
  * This actor is responsible for all database operations relating to
  * [[BlockHeader]]'s. Currently we store all block headers in a postgresql database
  */
sealed trait BlockHeaderDAO extends CRUDActor[BlockHeader,DoubleSha256Digest] {

  override val table = TableQuery[BlockHeaderTable]

  def receive = {
    case msg: BlockHeaderDAOMessage =>
      handleBlockHeaderDAOMsg(msg)
  }

  private def handleBlockHeaderDAOMsg(message: BlockHeaderDAO.BlockHeaderDAOMessage) = message match {
    case createMsg: BlockHeaderDAO.Create =>
      val createdBlockHeader = create(createMsg.blockHeader).map(BlockHeaderDAO.CreatedHeader(_))(context.dispatcher)
      sendToParent(createdBlockHeader)
    case createAllMsg: BlockHeaderDAO.CreateAll =>
      val createAllHeaders = createAll(createAllMsg.blockHeaders).map(BlockHeaderDAO.CreatedHeaders(_))(context.dispatcher)
      sendToParent(createAllHeaders)
    case readMsg: BlockHeaderDAO.Read =>
      val readHeader = read(readMsg.hash)
      val reply = readHeader.map(BlockHeaderDAO.ReadReply(_))(context.dispatcher)
      sendToParent(reply)
    case deleteMsg: BlockHeaderDAO.Delete =>
      val deletedRowCount = delete(deleteMsg.blockHeader)
      val reply = deletedRowCount.map {
        case 0 => BlockHeaderDAO.DeleteReply(None)
        case _ => BlockHeaderDAO.DeleteReply(Some(deleteMsg.blockHeader))
      }(context.dispatcher)
      sendToParent(reply)
    case BlockHeaderDAO.LastSavedHeader =>
      val maxHeight: Rep[Option[Long]] = table.map(_.height).max
      val query : Query[BlockHeaderTable, BlockHeader, Seq] = table.filter(_.height === maxHeight)
      val result: Future[Seq[BlockHeader]] = database.run(query.result)
      //TODO: This is a bug here, shouldn't be picking just one header, maybe should return all of them
      val reply = result.map(headers => BlockHeaderDAO.LastSavedHeaderReply(headers.headOption))(context.dispatcher)
      sendToParent(reply)
    case BlockHeaderDAO.GetAtHeight(height) =>
      val result = getAtHeight(height)
      val reply = result.map(h => BlockHeaderDAO.BlockHeaderAtHeight(h._1, h._2))(context.dispatcher)
      sendToParent(reply)
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

  /** Creates all of the given [[BlockHeader]] in the database */
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

  /** Retrieves a [[BlockHeader]] at the given height, if none is found it returns None */
  def getAtHeight(height: Long): Future[(Long,Option[BlockHeader])] = {
    //TODO: Have to consider the case of reorgs here, what if their are two competing chains
    //which would both have height x
    val query = table.filter(_.height === height).result
    database.run(query).map(h => (height,h.headOption))(context.dispatcher)
  }
}


object BlockHeaderDAO {
  /** A message that the [[BlockHeaderDAO]] can send or receive */
  sealed trait BlockHeaderDAOMessage

  /** A message you can send to the [[BlockHeaderDAO]] */
  sealed trait BlockHeaderDAORequest extends BlockHeaderDAOMessage

  /** A message the [[BlockHeaderDAO]] can send to your Actor */
  sealed trait BlockHeaderDAOMessageReplies extends BlockHeaderDAOMessage

  /** The message to create a [[BlockHeader]] */
  case class Create(blockHeader: BlockHeader) extends BlockHeaderDAORequest
  /** The message that is sent as a reply to [[Create]] */
  case class CreatedHeader(header: BlockHeader) extends BlockHeaderDAOMessageReplies

  /** The message to create all [[BlockHeader]]s in our persistent storage */
  case class CreateAll(blockHeaders: Seq[BlockHeader]) extends BlockHeaderDAORequest
  /** The reply to the message [[CreateAll]] */
  case class CreatedHeaders(headers: Seq[BlockHeader]) extends BlockHeaderDAOMessageReplies

  /** Reads a [[BlockHeader]] with the given hash from persistent storage */
  case class Read(hash: DoubleSha256Digest) extends BlockHeaderDAORequest
  /** The reply for the [[Read]] message */
  case class ReadReply(hash: Option[BlockHeader]) extends BlockHeaderDAOMessageReplies

  /** Deletes a [[BlockHeader]] from persistent storage */
  case class Delete(blockHeader: BlockHeader) extends BlockHeaderDAORequest
  /** The reply to a [[Delete]] message */
  case class DeleteReply(blockHeader: Option[BlockHeader]) extends BlockHeaderDAOMessageReplies

  case object LastSavedHeader extends BlockHeaderDAORequest
  case class LastSavedHeaderReply(header: Option[BlockHeader]) extends BlockHeaderDAOMessageReplies

  case class GetAtHeight(height: Long) extends BlockHeaderDAORequest
  case class BlockHeaderAtHeight(height: Long, header: Option[BlockHeader]) extends BlockHeaderDAOMessageReplies

  private case class BlockHeaderDAOImpl(database: Database) extends BlockHeaderDAO

  def props(database: Database): Props = Props(BlockHeaderDAOImpl(database))

  def apply(context: ActorRefFactory, database: Database): ActorRef = context.actorOf(props(database),
    BitcoinSpvNodeUtil.createActorName(BlockHeaderDAO.getClass))

  def apply(database: Database): ActorRef = BlockHeaderDAO(Constants.actorSystem,database)

  def apply: ActorRef = BlockHeaderDAO(Constants.actorSystem,Constants.database)
}
