package org.bitcoins.spvnode.messages

import org.bitcoins.core.protocol.NetworkElement
import org.bitcoins.core.util.{BitcoinSUtil, Factory}
import org.bitcoins.spvnode.serializers.messages.RawTypeIdentifierSerializer

/**
  * Created by chris on 5/31/16.
  * This indicates the type of the object that has been hashed for an inventory
  */
sealed trait TypeIdentifier extends NetworkElement {
  def num : Long
  override def hex = RawTypeIdentifierSerializer.write(this)
}

case object MsgTx extends TypeIdentifier {
  override def num = 1
}

case object MsgBlock extends TypeIdentifier {
  override def num = 2
}

case object MsgFilteredBlock extends TypeIdentifier {
  override def num = 3
}

sealed trait MsgUnassigned extends TypeIdentifier

object TypeIdentifier extends Factory[TypeIdentifier] {

  private case class MsgUnassignedImpl(num : Long) extends MsgUnassigned

  override def fromBytes(bytes : Seq[Byte]) : TypeIdentifier = RawTypeIdentifierSerializer.read(bytes)

  def apply(hex : String) : TypeIdentifier = fromHex(hex)

  def apply(bytes : Seq[Byte]) : TypeIdentifier = fromBytes(bytes)

  def apply(num : Long) : TypeIdentifier = num match {
    case 1 => MsgTx
    case 2 => MsgBlock
    case 3 => MsgFilteredBlock
    case x : Long => MsgUnassignedImpl(x)
  }
}
