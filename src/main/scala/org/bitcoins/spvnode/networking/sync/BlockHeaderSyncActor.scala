package org.bitcoins.spvnode.networking.sync

import akka.actor.{Actor, ActorRef, ActorRefFactory, PoisonPill, Props}
import akka.event.LoggingReceive
import org.bitcoins.core.config.{MainNet, RegTest, TestNet3}
import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.protocol.blockchain.BlockHeader
import org.bitcoins.core.util.BitcoinSLogger
import org.bitcoins.spvnode.constant.{Constants, DbConfig}
import org.bitcoins.spvnode.messages.{GetHeadersMessage, HeadersMessage}
import org.bitcoins.spvnode.messages.data.GetHeadersMessage
import org.bitcoins.spvnode.models.BlockHeaderDAO
import org.bitcoins.spvnode.networking.PeerMessageHandler
import org.bitcoins.spvnode.networking.sync.BlockHeaderSyncActor.{CheckHeaderResult, GetHeaders, StartAtLastSavedHeader}
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
    *
    * @return
    */
  private def maxHeaders = 2000

  def dbConfig: DbConfig

  private def blockHeaderDAO = BlockHeaderDAO(context, dbConfig)

  private def peerMessageHandler = PeerMessageHandler(context)

  def receive = LoggingReceive {
    case startHeader: BlockHeaderSyncActor.StartHeaders =>
      val p = peerMessageHandler
      context.become(blockHeaderSync(p,startHeader.headers.last))
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
    * @param lastHeader
    * @return
    */
  def blockHeaderSync(peerMessageHandler: ActorRef, lastHeader: BlockHeader): Receive = LoggingReceive {
    case startHeader: BlockHeaderSyncActor.StartHeaders =>
      val getHeadersMsg = GetHeadersMessage(startHeader.headers.map(_.hash))
      peerMessageHandler ! getHeadersMsg
    case headersMsg: HeadersMessage =>
      val headers = headersMsg.headers
      context.become(awaitCheckHeaders(Some(lastHeader),headers),discardOld = false)
      blockHeaderDAO ! BlockHeaderDAO.MaxHeight
    case createdHeaders: BlockHeaderDAO.CreatedHeaders =>
      //indicates that our blockHeaderDAO successfully created the block headers we sent it inside of
      //handleValidHeaders
      sender ! PoisonPill

    case checkHeaderResult: CheckHeaderResult =>
      if (checkHeaderResult.error.isDefined) {
        logger.error("We had an error syncing our blockchain: " +checkHeaderResult.error.get)
        context.parent ! checkHeaderResult.error.get
        self ! PoisonPill
      } else handleValidHeaders(checkHeaderResult.headers,peerMessageHandler)
  }

  /** This behavior is responsible for calling the [[checkHeader]] function, after evaluating
    * if the headers are valid, reverts to the context the actor previously held and sends it the
    * result of checking the headers
    *
    * The only message this context expects is the [[BlockHeaderDAO]] to send it the current
    * max height of the blockchain that it has stored right now
    * @param lastHeader
    * @param headers
    * @return
    */
  def awaitCheckHeaders(lastHeader: Option[BlockHeader], headers: Seq[BlockHeader]) = LoggingReceive {
    case maxHeight: BlockHeaderDAO.MaxHeightReply =>
      val result = checkHeaders(lastHeader,headers,maxHeight.height)
      context.unbecome()
      self ! result
  }

  /** Actor context that specifically deals with the [[BlockHeaderSyncActor.GetHeaders]] message */
  def awaitGetHeaders: Receive = LoggingReceive {
    case headersMsg: HeadersMessage =>
      val headers = headersMsg.headers
      if (headers.isEmpty) context.parent ! Nil
      else {
        context.become(awaitCheckHeaders(None, headers), discardOld = false)
        blockHeaderDAO ! BlockHeaderDAO.MaxHeight
      }
    case checkHeaderResult: CheckHeaderResult =>
      context.parent ! checkHeaderResult.error.getOrElse(checkHeaderResult.headers)
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
        context.become(blockHeaderSync(p,header))
        self ! BlockHeaderSyncActor.StartHeaders(Seq(header))
        context.parent ! BlockHeaderSyncActor.StartAtLastSavedHeaderReply(header)
      } else {
        //TODO: Need to write a test case for this inside of BlockHeaderSyncActorTest
        //means we have two (or more) competing chains, therefore we need to try and sync with both of them
        lastSavedHeader.headers.map { header =>
          val blockHeaderSyncActor = BlockHeaderSyncActor(context,dbConfig)
          blockHeaderSyncActor ! BlockHeaderSyncActor.StartHeaders(Seq(header))
          context.parent ! BlockHeaderSyncActor.StartAtLastSavedHeaderReply(header)
        }
      }
      sender ! PoisonPill
  }

  /** Checks that the given block headers all connect to each other
    * If the headers do not connect, it returns the two block header hashes that do not connec
    * @param startingHeader header we are starting our header check from
    * @param blockHeaders the set of headers we are checking the validity of
    * @param maxHeight the height of the blockchain before checking the block headers
    * */
  private def checkHeaders(startingHeader: Option[BlockHeader], blockHeaders: Seq[BlockHeader], maxHeight: Long): CheckHeaderResult = {
    @tailrec
    def loop(previousBlockHeader: BlockHeader, remainingBlockHeaders: Seq[BlockHeader]): CheckHeaderResult = {
      if (remainingBlockHeaders.isEmpty) CheckHeaderResult(None,blockHeaders)
      else {
        val header = remainingBlockHeaders.head
        if (header.previousBlockHash != previousBlockHeader.hash) {
          val error = BlockHeaderSyncActor.BlockHeadersDoNotConnect(previousBlockHeader.hash, header.hash)
          CheckHeaderResult(Some(error),blockHeaders)
        } else if (header.nBits != previousBlockHeader.nBits) {
          Constants.networkParameters match {
            case MainNet =>
              val blockHeaderHeight = (blockHeaders.size - remainingBlockHeaders.size) + maxHeight
              if ((blockHeaderHeight % 2016) == 0) loop(remainingBlockHeaders.head, remainingBlockHeaders.tail)
              else {
                val error = BlockHeaderSyncActor.BlockHeaderDifficultyFailure(previousBlockHeader,remainingBlockHeaders.head)
                CheckHeaderResult(Some(error),blockHeaders)
              }
            case RegTest | TestNet3 => ???
          }
        }
        else loop(header, remainingBlockHeaders.tail)
      }
    }
    val result = if (startingHeader.isDefined) loop(startingHeader.get,blockHeaders) else loop(blockHeaders.head, blockHeaders.tail)
    result
  }

  /** Stores the valid headers in our database, sends our actor a message to start syncing from the last
    * header we received if necessary
    *
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
      val startHeader = BlockHeaderSyncActor.StartHeaders(Seq(lastHeader))
      //need to reset the last header hash we saw on the network
      context.become(blockHeaderSync(peerMessageHandler,lastHeader))
      self ! startHeader
    } else {
      //else we we are synced up on the network, send the parent the last header we have seen
      context.parent ! lastHeader
      self ! PoisonPill
    }
  }
}

object BlockHeaderSyncActor {
  private case class BlockHeaderSyncActorImpl(dbConfig: DbConfig) extends BlockHeaderSyncActor

  def apply(context: ActorRefFactory, dbConfig: DbConfig): ActorRef = context.actorOf(props(dbConfig),
    BitcoinSpvNodeUtil.createActorName(BlockHeaderSyncActor.getClass))

  def props(dbConfig: DbConfig): Props = {
    Props(classOf[BlockHeaderSyncActorImpl], dbConfig)
  }

  sealed trait BlockHeaderSyncMessage

  sealed trait BlockHeaderSyncMessageRequest
  sealed trait BlockHeaderSycnMessageReply

  /** Indicates a set of headers to query our peer on the network to start our sync process */
  case class StartHeaders(headers: Seq[BlockHeader]) extends BlockHeaderSyncMessageRequest

  /** Retrieves the set of headers from a node on the network, this does NOT store them */
  case class GetHeaders(startHeader: DoubleSha256Digest, stopHeader: DoubleSha256Digest) extends BlockHeaderSyncMessageRequest

  /** Starts syncing our blockchain at the last header we have seen, if we haven't see any it starts at the genesis block */
  case object StartAtLastSavedHeader extends BlockHeaderSyncMessageRequest
  /** Reply for [[StartAtLastSavedHeader]] */
  case class StartAtLastSavedHeaderReply(header: BlockHeader) extends BlockHeaderSycnMessageReply

  /** Indicates an error happened during the sync of our blockchain */
  sealed trait BlockHeaderSyncError extends BlockHeaderSycnMessageReply

  /** Indicates that our block headers do not properly reference one another
    *
    * @param previousBlockHash indicates the last valid block that connected to a header
    * @param blockHash indicates the first block hash that did NOT connect to the previous valid chain
    * */
  case class BlockHeadersDoNotConnect(previousBlockHash: DoubleSha256Digest, blockHash: DoubleSha256Digest) extends BlockHeaderSyncError

  /** Indicates that our node saw a difficulty adjustment on the network when there should not have been one between the
    * two given [[BlockHeader]]s */
  case class BlockHeaderDifficultyFailure(previousBlockHeader: BlockHeader, blockHeader: BlockHeader) extends BlockHeaderSyncError

  //INTERNAL MESSAGES FOR BlockHeaderSyncActor
  case class CheckHeaderResult(error: Option[BlockHeaderSyncError], headers: Seq[BlockHeader]) extends BlockHeaderSyncMessage

}
