package org.bitcoins.spvnode.models

import akka.actor.{ActorRef, ActorRefFactory, Props}
import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.protocol.blockchain.BlockHeader
import org.bitcoins.core.util.BitcoinSUtil
import org.bitcoins.spvnode.constant.{Constants, DbConfig}
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
    case msg: BlockHeaderDAO.BlockHeaderDAORequest =>
      handleBlockHeaderDAORequest(msg)
  }

  /** Function designed to handle all [[BlockHeaderDAO.BlockHeaderDAORequest]] messages we can receive */
  private def handleBlockHeaderDAORequest(message: BlockHeaderDAO.BlockHeaderDAORequest) = message match {
    case createMsg: BlockHeaderDAO.Create =>
      val createdBlockHeader = create(createMsg.blockHeader).map(BlockHeaderDAO.CreateReply(_))(context.dispatcher)
      sendToParent(createdBlockHeader)
    case createAllMsg: BlockHeaderDAO.CreateAll =>
      val createAllHeaders = createAll(createAllMsg.blockHeaders).map(BlockHeaderDAO.CreateAllReply(_))(context.dispatcher)
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
      logger.error("RECEIVED LAST SAVED HEADER MSG")
      val reply = lastSavedHeader.map(headers => BlockHeaderDAO.LastSavedHeaderReply(headers))(context.dispatcher)
      reply.map(r => logger.error("Last saved header reply: " + r))(context.dispatcher)
      sendToParent(reply)
    case BlockHeaderDAO.GetAtHeight(height) =>
      val result = getAtHeight(height)
      val reply = result.map(h => BlockHeaderDAO.GetAtHeightReply(h._1, h._2))(context.dispatcher)
      sendToParent(reply)
    case BlockHeaderDAO.FindHeight(hash) =>
      val result = findHeight(hash)
      val reply = result.map(BlockHeaderDAO.FoundHeight(_))(context.dispatcher)
      sendToParent(reply)
    case BlockHeaderDAO.MaxHeight =>
      val result = maxHeight
      val reply = result.map(BlockHeaderDAO.MaxHeightReply(_))(context.dispatcher)
      sendToParent(reply)
  }

  /** Sends a message to our parent actor */
  private def sendToParent(returnMsg: Future[Any]): Unit = returnMsg.onComplete {
    case Success(msg) =>
      context.parent ! msg
    case Failure(exception) =>
      //means the future did not complete successfully, we encountered an error somewhere
      logger.error("Exception: " + exception.toString)
      throw exception
  }(context.dispatcher)

  def create(blockHeader: BlockHeader): Future[BlockHeader] = {
    val action = if (blockHeader == Constants.chainParams.genesisBlock.blockHeader) {
      //we need to make an exception for the genesis block, it does not have a previous hash
      //so we remove that invariant in this sql statement
      sqlu"insert into block_headers values(0, ${blockHeader.hash.hex}, ${blockHeader.version.underlying}, ${blockHeader.previousBlockHash.hex}, ${blockHeader.merkleRootHash.hex}, ${blockHeader.time.underlying}, ${blockHeader.nBits.underlying}, ${blockHeader.nonce.underlying}, ${blockHeader.hex})".andThen(DBIO.successful(blockHeader))
    } else insertStatement(blockHeader)
    database.run(action)
  }

  /** Creates all of the given [[BlockHeader]] in the database */
  def createAll(headers: Seq[BlockHeader]): Future[Seq[BlockHeader]] = {
    val actions = DBIO.sequence(headers.map(insertStatement(_)))
    database.run(actions)
  }

  /** This is the custom insert statement needed for block headers, the magic here is it
    * increments the height of the previous [[BlockHeader]] by one.
    * See this question for how the statement works
    * [[http://stackoverflow.com/questions/39628543/derive-column-value-from-another-rows-column-value-slick]]
    * @param blockHeader
    * @return
    */
  private def insertStatement(blockHeader: BlockHeader) = {
    sqlu"insert into block_headers (height, hash, version, previous_block_hash, merkle_root_hash, time,n_bits,nonce,hex) select height + 1, ${blockHeader.hash.hex}, ${blockHeader.version.underlying}, ${blockHeader.previousBlockHash.hex}, ${blockHeader.merkleRootHash.hex}, ${blockHeader.time.underlying}, ${blockHeader.nBits.underlying}, ${blockHeader.nonce.underlying}, ${blockHeader.hex}  from block_headers where hash = ${blockHeader.previousBlockHash.hex}"
        .flatMap { rowsAffected =>
          if (rowsAffected == 0) {
            val exn = new IllegalArgumentException("Failed to insert blockHeader: " + BitcoinSUtil.flipEndianness(blockHeader.hash.bytes) + " prevHash: " + BitcoinSUtil.flipEndianness(blockHeader.previousBlockHash.bytes))
            DBIO.failed(exn)
          }
          else DBIO.successful(blockHeader)
        }(context.dispatcher)
  }

  def find(blockHeader: BlockHeader): Query[Table[_],  BlockHeader, Seq] = findByPrimaryKey(blockHeader.hash)

  def findByPrimaryKey(hash : DoubleSha256Digest): Query[Table[_], BlockHeader, Seq] = {
    import ColumnMappers._
    table.filter(_.hash === hash)
  }

  /** Retrieves a [[BlockHeader]] at the given height, if none is found it returns None */
  def getAtHeight(height: Long): Future[(Long,Seq[BlockHeader])] = {
    //which would both have height x
    val query = table.filter(_.height === height).result
    database.run(query).map(h => (height,h))(context.dispatcher)
  }

  /** Finds the height of the given [[BlockHeader]]'s hash, if it exists */
  def findHeight(hash: DoubleSha256Digest): Future[Option[(Long,BlockHeader)]] = {
    import ColumnMappers._
    val query = table.filter(_.hash === hash).map(x => (x.height,x)).result
    val b: Future[Option[(Long,BlockHeader)]] = database.run(query).map(_.headOption)(context.dispatcher)
    b
  }

  /** Returns the maximum block height from our database */
  def maxHeight: Future[Long] = {
    val query = table.map(_.height).max.result
    val result = database.run(query)
    result.map(_.getOrElse(0L))(context.dispatcher)
  }

  /** Returns the last saved header in the database */
  def lastSavedHeader: Future[Seq[BlockHeader]] = {
    implicit val c = context.dispatcher
    val max = maxHeight
    max.flatMap(getAtHeight(_).map(_._2))
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
  case class CreateReply(blockHeader: BlockHeader) extends BlockHeaderDAOMessageReplies

  /** The message to create all [[BlockHeader]]s in our persistent storage */
  case class CreateAll(blockHeaders: Seq[BlockHeader]) extends BlockHeaderDAORequest
  /** The reply to the message [[CreateAll]] */
  case class CreateAllReply(headers: Seq[BlockHeader]) extends BlockHeaderDAOMessageReplies

  /** Reads a [[BlockHeader]] with the given hash from persistent storage */
  case class Read(hash: DoubleSha256Digest) extends BlockHeaderDAORequest
  /** The reply for the [[Read]] message */
  case class ReadReply(hash: Option[BlockHeader]) extends BlockHeaderDAOMessageReplies

  /** Deletes a [[BlockHeader]] from persistent storage */
  case class Delete(blockHeader: BlockHeader) extends BlockHeaderDAORequest
  /** The reply to a [[Delete]] message */
  case class DeleteReply(blockHeader: Option[BlockHeader]) extends BlockHeaderDAOMessageReplies

  /** Asks our [[BlockHeaderDAO]] to return the last saved [[BlockHeader]] */
  case object LastSavedHeader extends BlockHeaderDAORequest
  /** Returns the last saved [[BlockHeader]], this could be multiple headers if their is a fork in the chain */
  case class LastSavedHeaderReply(headers: Seq[BlockHeader]) extends BlockHeaderDAOMessageReplies

  /** Asks [[BlockHeaderDAO]] to give us the [[BlockHeader]] at the specified height */
  case class GetAtHeight(height: Long) extends BlockHeaderDAORequest
  /** Returns the [[BlockHeader]]s at the given height, note this can be multiple headers if we have a fork in the chain */
  case class GetAtHeightReply(height: Long, headers: Seq[BlockHeader]) extends BlockHeaderDAOMessageReplies

  /** Asks [[BlockHeaderDAO]] to return the height of the given [[BlockHeader]]'s hash */
  case class FindHeight(hash: DoubleSha256Digest) extends BlockHeaderDAORequest

  /** Returns the height of the [[BlockHeader]], this is a reply to the [[FindHeight]] message
    * note that this is different than [[GetAtHeightReply]] in the fact that it will only
    * return ONE [[BlockHeader]], [[GetAtHeightReply]] will return multiple if their are
    * competing chains
    */
  case class FoundHeight(headerAtHeight: Option[(Long,BlockHeader)]) extends BlockHeaderDAOMessageReplies

  /** Requests the height of the longest chain */
  case object MaxHeight extends BlockHeaderDAORequest

  /** A reply to the [[MaxHeight]] request, returns the height of the longest chain in our store */
  case class MaxHeightReply(height: Long) extends BlockHeaderDAOMessageReplies

  private case class BlockHeaderDAOImpl(dbConfig: DbConfig) extends BlockHeaderDAO

  def props(dbConfig: DbConfig): Props = Props(classOf[BlockHeaderDAOImpl],dbConfig)

  def apply(context: ActorRefFactory, dbConfig: DbConfig): ActorRef = context.actorOf(props(dbConfig),
    BitcoinSpvNodeUtil.createActorName(BlockHeaderDAO.getClass))

  def apply(dbConfig: DbConfig): ActorRef = BlockHeaderDAO(Constants.actorSystem,dbConfig)

  def apply: ActorRef = BlockHeaderDAO(Constants.actorSystem,Constants.dbConfig)
}
