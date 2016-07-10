package org.bitcoins.spvnode.networking

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.util.{BitcoinSLogger, BitcoinSUtil}
import org.bitcoins.spvnode.messages.BlockMessage
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpecLike, MustMatchers}
import scala.concurrent.duration.DurationInt

/**
  * Created by chris on 7/10/16.
  */
class BlockActorTest extends TestKit(ActorSystem("BlockActorTest")) with FlatSpecLike
  with MustMatchers with ImplicitSender
  with BeforeAndAfter with BeforeAndAfterAll with BitcoinSLogger  {

  val blockActor = TestActorRef(BlockActor.props,self)
  "BlockActor" must "be able to send a GetBlocksMessage then receive that block back" in {
    val blockHash = DoubleSha256Digest(BitcoinSUtil.flipEndianess("00000000b873e79784647a6c82962c70d228557d24a747ea4d1b8bbe878e1206"))

    blockActor ! blockHash

    val blockMsg = expectMsgType[BlockMessage](10.seconds)
    blockMsg.block.blockHeader.hash must be (blockHash)

  }


  override def afterAll = {
    TestKit.shutdownActorSystem(system)
  }
}
