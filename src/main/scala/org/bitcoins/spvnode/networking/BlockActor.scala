package org.bitcoins.spvnode.networking

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.LoggingReceive
import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.protocol.CompactSizeUInt
import org.bitcoins.core.protocol.blockchain.BlockHeader
import org.bitcoins.core.util.BitcoinSLogger
import org.bitcoins.spvnode.NetworkMessage
import org.bitcoins.spvnode.constant.Constants
import org.bitcoins.spvnode.messages.{BlockMessage, GetBlocksMessage, InventoryMessage, MsgBlock}
import org.bitcoins.spvnode.messages.data.{GetBlocksMessage, GetDataMessage, Inventory, InventoryMessage}

/**
  * Created by chris on 7/10/16.
  */
sealed trait BlockActor extends Actor with BitcoinSLogger {


  def receive: Receive = LoggingReceive {
    case getBlocksMessage: GetBlocksMessage =>
      val peerMsgHandler = context.actorOf(PeerMessageHandler.props)
      val networkMessage = NetworkMessage(Constants.networkParameters, getBlocksMessage)
      peerMsgHandler ! networkMessage
      context.become(awaitBlockMsg)
    case hash: DoubleSha256Digest =>
      val peerMsgHandler = context.actorOf(PeerMessageHandler.props,"PeerMessageHandlerBlockActor")
      val inv = Inventory(MsgBlock,hash)
      val getDataMessage = GetDataMessage(inv)
      val networkMessage = NetworkMessage(Constants.networkParameters, getDataMessage)
      logger.debug("self: " + self)
      peerMsgHandler ! networkMessage
      context.become(awaitBlockMsg)
    case blockHeader: BlockHeader =>
      self ! blockHeader.hash
    case blockMsg: BlockMessage =>
      logger.debug("Received blockMsg in BlockActor: " + blockMsg)
  }

  def awaitBlockMsg: Receive = LoggingReceive {
    case blockMsg: BlockMessage =>
      context.parent ! blockMsg
      context.stop(self)
  }
}


object BlockActor {
  private case class BlockActorImpl() extends BlockActor
  def props = Props(classOf[BlockActorImpl])

  def apply(actorSystem: ActorSystem): ActorRef = actorSystem.actorOf(props)

}