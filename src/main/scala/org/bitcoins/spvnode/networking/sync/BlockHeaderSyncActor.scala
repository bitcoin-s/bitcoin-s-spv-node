package org.bitcoins.spvnode.networking.sync

import akka.actor.{Actor, ActorRef, ActorRefFactory, PoisonPill, Props}
import akka.event.LoggingReceive
import org.bitcoins.core.config.{MainNet, NetworkParameters, RegTest, TestNet3}
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

  def networkParameters: NetworkParameters

  def dbConfig: DbConfig

  /** Helper function to provide a fresh instance of a [[BlockHeaderDAO]] actor */
  private def blockHeaderDAO: ActorRef = BlockHeaderDAO(context, dbConfig)

  /** Helper function to connect to a new peer on the network */
  private def peerMessageHandler: ActorRef = PeerMessageHandler(context)

  def receive = LoggingReceive {
    case startHeader: BlockHeaderSyncActor.StartHeaders =>
      val p = peerMessageHandler
      val lastHeader = startHeader.headers.last
      if (lastHeader == Constants.chainParams.genesisBlock.blockHeader) {
        //TODO: Think this causes a bug in our test because the table is being dropped
        //while the message is still being processed
        //seed the database with the genesis header
        logger.info("Switching to awaitGenesisHeaderCreateReply from receive")
        context.become(awaitGenesisHeaderCreateReply(p,Nil))
        blockHeaderDAO ! BlockHeaderDAO.Create(lastHeader)
      } else {
        logger.info("Switching to blockHeaderSync from receive")
        context.become(blockHeaderSync(p,lastHeader))
        self.forward(startHeader)
      }
    case getHeaders: GetHeaders =>
      val getHeadersMessage = GetHeadersMessage(Seq(getHeaders.startHeader), getHeaders.stopHeader)
      val p = peerMessageHandler
      p ! getHeadersMessage
      logger.info("Switching to awaitGetHeaders from receive")
      context.become(awaitGetHeaders)
    case StartAtLastSavedHeader =>
      blockHeaderDAO ! BlockHeaderDAO.LastSavedHeader
      logger.info("Switching to awaitLastSavedHeader from receive")
      context.become(awaitLastSavedHeader)
  }

  /** Main block header sync context, lastHeader is used to make sure the batch of block headers we see
    * matches connects to the last batch of block headers we saw (thus forming a blockchain)
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
      logger.debug("Headers before all of the checking: " + headers.size)
      logger.info("Switching to awaitCheckHeaders from blockHeaderSync")
      context.become(awaitCheckHeaders(Some(lastHeader),headers),discardOld = false)
      val b = blockHeaderDAO
      b ! BlockHeaderDAO.MaxHeight
    case checkHeaderResult: CheckHeaderResult =>
      logger.debug("Received check header result inside of blockHeaderSync")
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
      val result = BlockHeaderSyncActor.checkHeaders(lastHeader,headers,maxHeight.height,networkParameters)
      context.unbecome()
      sender ! PoisonPill
      self ! result
  }

  /** Actor context that specifically deals with the [[BlockHeaderSyncActor.GetHeaders]] message */
  def awaitGetHeaders: Receive = LoggingReceive {
    case headersMsg: HeadersMessage =>
      val headers = headersMsg.headers
      logger.info("Switching to awaitCheckHeaders from awaitGetHeaders")
      context.become(awaitCheckHeaders(None, headers), discardOld = false)
      blockHeaderDAO ! BlockHeaderDAO.MaxHeight
    case checkHeaderResult: CheckHeaderResult =>
      context.parent ! checkHeaderResult.error.getOrElse(BlockHeaderSyncActor.GetHeadersReply(checkHeaderResult.headers))
  }

  /** Awaits for our [[BlockHeaderDAO]] to send us the last saved header it has
    * if we do not have a last saved header, it will use the genesis block's header
    * on the network we are currently on as the last saved header */
  def awaitLastSavedHeader: Receive = {
    case lastSavedHeader: BlockHeaderDAO.LastSavedHeaderReply =>
      if (lastSavedHeader.headers.size <= 1) {
        //means we have either zero or one last saved header, if it is zero we can sync from genesis block, if one start there
        val header = lastSavedHeader.headers.headOption.getOrElse(Constants.chainParams.genesisBlock.blockHeader)
        val p = PeerMessageHandler(context)
        logger.info("Switching to blockHeaderSync from awaitLastSavedHeader")
        context.become(blockHeaderSync(p,header))
        self ! BlockHeaderSyncActor.StartHeaders(Seq(header))
        context.parent ! BlockHeaderSyncActor.StartAtLastSavedHeaderReply(header)
      } else {
        //TODO: Need to write a test case for this inside of BlockHeaderSyncActorTest
        //means we have two (or more) competing chains, therefore we need to try and sync with both of them
        lastSavedHeader.headers.map { header =>
          val syncActor = BlockHeaderSyncActor(context,dbConfig,networkParameters)
          syncActor ! BlockHeaderSyncActor.StartHeaders(Seq(header))
          context.parent ! BlockHeaderSyncActor.StartAtLastSavedHeaderReply(header)
        }
      }
      sender ! PoisonPill
  }


  /** Stores the valid headers in our database, sends our actor a message to start syncing from the last
    * header we received if necessary
    *
    * @param headers
    * @param peerMessageHandler
    */
  def handleValidHeaders(headers: Seq[BlockHeader], peerMessageHandler: ActorRef) = {
    logger.debug("Headers size to be inserted: "  + headers.size)
    val createAllMsg = BlockHeaderDAO.CreateAll(headers)
    val b = blockHeaderDAO
    logger.info("Switching to awaitCreatedAllReply from handleValidHeaders")
    context.become(awaitCreatedAllReply(peerMessageHandler,headers))
    b ! createAllMsg
  }

  /** Waits for our [[BlockHeaderDAO]] to reply with all the headers it created in persistent storage
    * @param networkHeaders the headers we received from our peer on the network. This may be a different
    *                       size than what is returned from [[BlockHeaderDAO.CreateAllReply]]. This can
    *                       be the case if our database fails to insert some of the headers.
    * */
  def awaitCreatedAllReply(peerMessageHandler: ActorRef, networkHeaders: Seq[BlockHeader]): Receive = LoggingReceive {
    case createdHeaders: BlockHeaderDAO.CreateAllReply =>
      val headers = createdHeaders.headers
      val lastHeader = createdHeaders.headers.last
      if (headers.size == maxHeaders || headers.size != networkHeaders.size) {
        //this means we either stored all the headers in the database and need to start from the last header or
        //we failed to store all the headers (due to some error in BlockHeaderDAO) and we need to start from the last
        //header we successfully stored in the database
        logger.debug("Starting next sync with this block header: " + lastHeader.hash)
        //means we need to send another GetHeaders message with the last header in this message as our starting point
        val startHeader = BlockHeaderSyncActor.StartHeaders(Seq(lastHeader))
        //need to reset the last header hash we saw on the network
        logger.info("Switching to blockHeaderSync from awaitCreatedAllReply")
        context.become(blockHeaderSync(peerMessageHandler,lastHeader))
        self ! startHeader
      } else {
        //else we we are synced up on the network, send the parent the last header we have seen
        context.parent ! BlockHeaderSyncActor.SuccessfulSyncReply(lastHeader)
        self ! PoisonPill
      }
      sender ! PoisonPill
  }

  /** This behavior is used to seed the database,
    * we cannot do anything until the genesis header is created in persisten storage */
  def awaitGenesisHeaderCreateReply(peerMessageHandler: ActorRef, queuedMsgs: Seq[Any]) : Receive = {
    case createReply: BlockHeaderDAO.CreateReply =>
      logger.info("Switching to blockHeaderSync from awaitGenesisHeaderCreateReply")
      context.become(blockHeaderSync(peerMessageHandler, createReply.blockHeader))
      self ! BlockHeaderSyncActor.StartHeaders(Seq(createReply.blockHeader))
      sender ! PoisonPill
      //now send all queued messages
      queuedMsgs.map(msg => self ! msg)
    case msg =>
      context.become(awaitGenesisHeaderCreateReply(peerMessageHandler, queuedMsgs ++ Seq(msg)))
  }
}

object BlockHeaderSyncActor extends BitcoinSLogger {
  private case class BlockHeaderSyncActorImpl(dbConfig: DbConfig, networkParameters: NetworkParameters) extends BlockHeaderSyncActor

  def apply(context: ActorRefFactory, dbConfig: DbConfig, networkParameters: NetworkParameters): ActorRef = {
    context.actorOf(props(dbConfig, networkParameters),
      BitcoinSpvNodeUtil.createActorName(BlockHeaderSyncActor.getClass))
  }

  def props(dbConfig: DbConfig, networkParameters: NetworkParameters): Props = {
    Props(classOf[BlockHeaderSyncActorImpl], dbConfig, networkParameters)
  }

  sealed trait BlockHeaderSyncMessage

  sealed trait BlockHeaderSyncMessageRequest
  sealed trait BlockHeaderSyncMessageReply

  /** Indicates a set of headers to query our peer on the network to start our sync process */
  case class StartHeaders(headers: Seq[BlockHeader]) extends BlockHeaderSyncMessageRequest

  /** Retrieves the set of headers from a node on the network, this does NOT store them */
  case class GetHeaders(startHeader: DoubleSha256Digest, stopHeader: DoubleSha256Digest) extends BlockHeaderSyncMessageRequest
  case class GetHeadersReply(headers: Seq[BlockHeader]) extends BlockHeaderSyncMessageReply

  /** Starts syncing our blockchain at the last header we have seen, if we haven't see any it starts at the genesis block */
  case object StartAtLastSavedHeader extends BlockHeaderSyncMessageRequest
  /** Reply for [[StartAtLastSavedHeader]] */
  case class StartAtLastSavedHeaderReply(header: BlockHeader) extends BlockHeaderSyncMessageReply


  /** Indicates that we have successfully synced our blockchain, the [[lastHeader]] represents the header at the max height on the chain */
  case class SuccessfulSyncReply(lastHeader: BlockHeader) extends BlockHeaderSyncMessageReply

  /** Indicates an error happened during the sync of our blockchain */
  sealed trait BlockHeaderSyncError extends BlockHeaderSyncMessageReply

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

  /** Checks that the given block headers all connect to each other
    * If the headers do not connect, it returns the two block header hashes that do not connec
    * @param startingHeader header we are starting our header check from, this header is not checked
    *                       if this is not defined we just start from the first header in blockHeaders
    * @param blockHeaders the set of headers we are checking the validity of
    * @param maxHeight the height of the blockchain before checking the block headers
    * */
  def checkHeaders(startingHeader: Option[BlockHeader], blockHeaders: Seq[BlockHeader],
                   maxHeight: Long, networkParameters: NetworkParameters): CheckHeaderResult = {
    @tailrec
    def loop(previousBlockHeader: BlockHeader, remainingBlockHeaders: Seq[BlockHeader]): CheckHeaderResult = {
      if (remainingBlockHeaders.isEmpty) CheckHeaderResult(None,blockHeaders)
      else {
        val header = remainingBlockHeaders.head
        if (header.previousBlockHash != previousBlockHeader.hash) {
          val error = BlockHeaderSyncActor.BlockHeadersDoNotConnect(previousBlockHeader.hash, header.hash)
          CheckHeaderResult(Some(error),blockHeaders)
        } else if (header.nBits == previousBlockHeader.nBits) {
          loop(header, remainingBlockHeaders.tail)
        } else {
          networkParameters match {
            case MainNet =>
              val blockHeaderHeight = (blockHeaders.size - remainingBlockHeaders.tail.size) + maxHeight
              logger.debug("Block header height: " + blockHeaderHeight)
              if ((blockHeaderHeight % MainNet.difficultyChangeThreshold) == 0) loop(remainingBlockHeaders.head, remainingBlockHeaders.tail)
              else {
                val error = BlockHeaderSyncActor.BlockHeaderDifficultyFailure(previousBlockHeader,remainingBlockHeaders.head)
                CheckHeaderResult(Some(error),blockHeaders)
              }
            case RegTest | TestNet3 =>
              //currently we are just ignoring checking difficulty on testnet and regtest as they vary wildly
              loop(remainingBlockHeaders.head, remainingBlockHeaders.tail)
          }
        }
      }
    }
    val result = if (startingHeader.isDefined) loop(startingHeader.get,blockHeaders)
    else loop(blockHeaders.head, blockHeaders.tail)
    result
  }

}
