package org.bitcoins.spvnode.networking.sync

import akka.actor.{Actor, ActorRef, ActorRefFactory, Props}
import akka.event.LoggingReceive
import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.util.BitcoinSLogger
import org.bitcoins.spvnode.messages.HeadersMessage
import org.bitcoins.spvnode.messages.data.GetHeadersMessage
import org.bitcoins.spvnode.networking.PeerMessageHandler
import org.bitcoins.spvnode.networking.sync.BlockHeaderSyncActor.StartAtLastSavedHeader
import org.bitcoins.spvnode.util.BitcoinSpvNodeUtil

/**
  * Created by chris on 9/5/16.
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
      //construct a get headers message
      val getHeadersMsg = GetHeadersMessage(startHeader.hashes)
      val peerMsgHandler = PeerMessageHandler(context)
      peerMsgHandler ! getHeadersMsg
      context.become(awaitBlockHeaders(startHeader, peerMsgHandler))
    case StartAtLastSavedHeader => ???
  }

  def awaitBlockHeaders(startHeaders: BlockHeaderSyncActor.StartHeaders, peerMessageHandler: ActorRef) = LoggingReceive {
    case headersMsg: HeadersMessage =>
      val headers = headersMsg.headers
      if (headers.size == maxHeaders) {
        //means we need to send another GetHeaders message with the last header in this message as our starting point

      } else {
        //we should be synced up with all of the headers on the network.
        //store these headers in some sort a block store
      }
  }


}

object BlockHeaderSyncActor {
  private case class BlockHeaderSyncActorImpl() extends BlockHeaderSyncActor

  def props: Props = Props(classOf[BlockHeaderSyncActor])

  def apply(context: ActorRefFactory): ActorRef = context.actorOf(props,
    BitcoinSpvNodeUtil.createActorName(BlockHeaderSyncActor.getClass))


  sealed trait BlockHeaderSyncMsg
  /** Indicates a set of headers to query our peer on the network to start our sync process */
  case class StartHeaders(hashes: Seq[DoubleSha256Digest]) extends BlockHeaderSyncMsg
  //case class SyncSetOfHeaders(startHeaders: Seq[DoubleSha256Digest], stopHeaders: Seq[DoubleSha256Digest]) extends BlockHeaderSyncMsg
  case object StartAtLastSavedHeader extends BlockHeaderSyncMsg
}
