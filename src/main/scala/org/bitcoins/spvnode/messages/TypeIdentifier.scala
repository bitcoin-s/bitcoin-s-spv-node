package org.bitcoins.spvnode.messages

import org.bitcoins.core.util.{BitcoinSUtil, Factory}
import org.bitcoins.spvnode.serializers.messages.RawTypeIdentifierSerializer

/**
  * Created by chris on 5/31/16.
  * This indicates the type of the object that has been hashed for an inventory
  */
sealed trait TypeIdentifier {
  def num : Long
}

sealed trait MsgTx extends TypeIdentifier {
  override def num = 1
}

sealed trait MsgBlock extends TypeIdentifier {
  override def num = 2
}

sealed trait MsgFilteredBlock extends TypeIdentifier {
  override def num = 3
}

sealed trait MsgUnassigned extends TypeIdentifier

object TypeIdentifier extends Factory[TypeIdentifier] {

  private case object MsgTxImpl extends MsgTx

  private case object MsgBlockImpl extends MsgBlock

  private case object MsgFilteredBlockImpl extends MsgFilteredBlock

  private case class MsgUnassignedImpl(num : Long) extends MsgUnassigned

  override def fromBytes(bytes : Seq[Byte]) : TypeIdentifier = RawTypeIdentifierSerializer.read(bytes)

  def apply(hex : String) : TypeIdentifier = fromHex(hex)

  def apply(bytes : Seq[Byte]) : TypeIdentifier = fromBytes(bytes)

  def apply(num : Long) : TypeIdentifier = num match {
    case 1 => MsgTxImpl
    case 2 => MsgBlockImpl
    case 3 => MsgFilteredBlockImpl
    case x : Long => MsgUnassignedImpl(x)
  }
}
