package org.bitcoins.spvnode.networking.sync

import akka.actor.{Actor, ActorRef, ActorRefFactory, PoisonPill, Props}
import akka.event.LoggingReceive
import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.protocol.blockchain.BlockHeader
import org.bitcoins.core.util.BitcoinSLogger
import org.bitcoins.spvnode.constant.Constants
import org.bitcoins.spvnode.messages.{GetHeadersMessage, HeadersMessage}
import org.bitcoins.spvnode.messages.data.GetHeadersMessage
import org.bitcoins.spvnode.models.BlockHeaderDAO
import org.bitcoins.spvnode.networking.PeerMessageHandler
import org.bitcoins.spvnode.networking.sync.BlockHeaderSyncActor.{GetHeaders, StartAtLastSavedHeader}
import org.bitcoins.spvnode.store.BlockHeaderStore
import org.bitcoins.spvnode.util.BitcoinSpvNodeUtil
import slick.driver.PostgresDriver.api._

import scala.annotation.tailrec

/**
  * Created by chris on 9/5/16.
  *
  *
  */
trait BlockHeaderSyncActor extends Actor with BitcoinSLogger {

  /** This is the maximum amount of headers the bitcoin protocol will transmit
    * in one request
    * [[https://bitcoin.org/en/developer-reference#getheaders]]
    * @return
    */
  private def maxHeaders = 2000

  def receive = LoggingReceive {
    case startHeader: BlockHeaderSyncActor.StartHeaders =>
      val p = peerMessageHandler
      context.become(blockHeaderSync(p,startHeader.hashes.last))
      self.forward(startHeader)
    case getHeaders: GetHeaders =>
      val getHeadersMessage = GetHeadersMessage(Seq(getHeaders.startHeader), getHeaders.stopHeader)
      val p = peerMessageHandler
      p ! getHeadersMessage
      context.become(awaitGetHeaders)
    case StartAtLastSavedHeader =>
      blockHeaderDAO ! BlockHeaderDAO.LastSavedHeader
      context.become(awaitLastSavedHeader)
  }

  /** Main block header sync context, lastHeaderHash is used to make sure the batch of block headers we see
    * matches connects to the last batch of block headers we saw
    * @param peerMessageHandler
    * @param lastHeaderHash
    * @return
    */
  def blockHeaderSync(peerMessageHandler: ActorRef, lastHeaderHash: DoubleSha256Digest): Receive = LoggingReceive {
    case startHeader: BlockHeaderSyncActor.StartHeaders =>
      val getHeadersMsg = GetHeadersMessage(startHeader.hashes)
      peerMessageHandler ! getHeadersMsg
    case headersMsg: HeadersMessage =>
      val headers = headersMsg.headers
      val (validHeaders,lastValidHeaderHash,firstInvalidHeaderHash) = checkHeaders(lastHeaderHash,headers)
      if (!validHeaders) {
        logger.error("Our blockchain headers are not connected, disconnected at: " + lastValidHeaderHash + " and " + firstInvalidHeaderHash)
        context.parent !  BlockHeaderSyncActor.BlockHeadersDoNotConnect(lastValidHeaderHash.get,firstInvalidHeaderHash.get)
        self ! PoisonPill
      } else handleValidHeaders(headers,peerMessageHandler)
    case createdHeaders: BlockHeaderDAO.CreatedHeaders =>
      //indicates that our blockHeaderDAO successfully created the block headers we sent it inside of
      //handleValidHeaders
      sender ! PoisonPill
  }

  /** Actor context that specifically deals with the [[BlockHeaderSyncActor.GetHeaders]] message */
  def awaitGetHeaders: Receive = LoggingReceive {
    case headersMsg: HeadersMessage =>
      val headers = headersMsg.headers
      val (validHeaders,lastValidHeaderHash,firstInvalidHeaderHash) = checkHeaders(headers.head.hash,headers.tail)
      if (!validHeaders) {
        logger.error("Our blockchain headers are not connected, disconnected at: " + lastValidHeaderHash + " and " + firstInvalidHeaderHash)
        context.parent !  BlockHeaderSyncActor.BlockHeadersDoNotConnect(lastValidHeaderHash.get,firstInvalidHeaderHash.get)
        context.stop(self)
      } else context.parent ! headers
  }

  /** Awaits for our [[BlockHeaderDAO]] to send us the last saved header it has
    * if we do not have a last saved header, it will use the genesis block's header
    * on the network we are currently on as the last saved header */
  def awaitLastSavedHeader: Receive = {
    case lastSavedHeader: BlockHeaderDAO.LastSavedHeaderReply =>
      if (lastSavedHeader.headers.size <= 1) {
        //means we either have no saved headers at all, so we need to sync from the  genesis block
        //or we have one last saved header, so we can start syncing from that
        val header = lastSavedHeader.headers.headOption.getOrElse(Constants.chainParams.genesisBlock.blockHeader)
        val p = PeerMessageHandler(context)
        context.become(blockHeaderSync(p,header.hash))
        self ! BlockHeaderSyncActor.StartHeaders(Seq(header.hash))
        context.parent ! BlockHeaderSyncActor.StartAtLastSavedHeaderReply(header)
      } else {
        //TODO: Need to write a test case for this inside of BlockHeaderSyncActorTest
        //means we have two (or more) competing chains, therefore we need to try and sync with both of them
        lastSavedHeader.headers.map { header =>
          val blockHeaderSyncActor = BlockHeaderSyncActor(context)
          blockHeaderSyncActor ! BlockHeaderSyncActor.StartHeaders(Seq(header.hash))
          context.parent ! BlockHeaderSyncActor.StartAtLastSavedHeaderReply(header)
        }
      }
      sender ! PoisonPill


  }
  /** The database that our [[BlockHeaderDAO]] connects to */
  def database: Database

  private def blockHeaderDAO = BlockHeaderDAO(context, database)

  private def peerMessageHandler = PeerMessageHandler(context)

  /** Checks that the given block headers all connect to each other
    * If the headers do not connect, it returns the two block header hashes that do not connect */
  private def checkHeaders(firstHeaderHash: DoubleSha256Digest, blockHeaders: Seq[BlockHeader]): (Boolean, Option[DoubleSha256Digest], Option[DoubleSha256Digest]) = {
    @tailrec
    def loop(previousBlockHash: DoubleSha256Digest, remainingBlockHeaders: Seq[BlockHeader]): (Boolean, Option[DoubleSha256Digest], Option[DoubleSha256Digest]) = {
      if (remainingBlockHeaders.isEmpty) (true,None,None)
      else {
        val header = remainingBlockHeaders.head
        if (header.previousBlockHash != previousBlockHash) (false,Some(previousBlockHash),Some(header.hash))
        else loop(header.hash, remainingBlockHeaders.tail)
      }
    }
    loop(firstHeaderHash,blockHeaders)
  }

  /** Stores the valid headers in our database, sends our actor a message to start syncing from the last
    * header we received if necessary
    * @param headers
    * @param peerMessageHandler
    */
  def handleValidHeaders(headers: Seq[BlockHeader], peerMessageHandler: ActorRef) = {
    val lastHeader = headers.last
    val createAllMsg = BlockHeaderDAO.CreateAll(headers)
    val b = blockHeaderDAO
    b ! createAllMsg
    if (headers.size == maxHeaders) {
      //means we need to send another GetHeaders message with the last header in this message as our starting point
      val startHeader = BlockHeaderSyncActor.StartHeaders(Seq(lastHeader.hash))
      //need to reset the last header hash we saw on the network
      context.become(blockHeaderSync(peerMessageHandler,lastHeader.hash))
      self ! startHeader
    } else {
      //else we we are synced up on the network, send the parent the last header we have seen
      context.parent ! lastHeader
      context.stop(self)
    }
  }
}

object BlockHeaderSyncActor {
  private case class BlockHeaderSyncActorImpl(database: Database) extends BlockHeaderSyncActor

  def props: Props = props(Constants.database)

  def props(database: Database): Props = Props(classOf[BlockHeaderSyncActorImpl],database)

  def apply(context: ActorRefFactory): ActorRef = context.actorOf(props,
    BitcoinSpvNodeUtil.createActorName(BlockHeaderSyncActor.getClass))


  sealed trait BlockHeaderSyncMessage
  /** Indicates a set of headers to query our peer on the network to start our sync process */
  case class StartHeaders(hashes: Seq[DoubleSha256Digest]) extends BlockHeaderSyncMessage
  /** Retrieves the set of headers from a node on the network, this does NOT store them */
  case class GetHeaders(startHeader: DoubleSha256Digest, stopHeader: DoubleSha256Digest) extends BlockHeaderSyncMessage
  /** Starts syncing our blockchain at the last header we have seen, if we haven't see any it starts at the genesis block */
  case object StartAtLastSavedHeader extends BlockHeaderSyncMessage
  /** Reply for [[StartAtLastSavedHeader]] */
  case class StartAtLastSavedHeaderReply(header: BlockHeader) extends BlockHeaderSyncMessage
  /** Indicates an error happened during the sync of our blockchain */
  sealed trait BlockHeaderSyncError extends BlockHeaderSyncMessage

  /** Indicates that our block headers do not properly reference one another
    * @param previousBlockHash indicates the last valid block that connected to a header
    * @param blockHash indicates the first block hash that did NOT connect to the previous valid chain
    * */
  case class BlockHeadersDoNotConnect(previousBlockHash: DoubleSha256Digest, blockHash: DoubleSha256Digest) extends BlockHeaderSyncError

}
