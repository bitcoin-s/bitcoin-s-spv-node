package org.bitcoins.spvnode.messages

import org.bitcoins.core.number.UInt32
import org.bitcoins.core.protocol.NetworkElement
import org.bitcoins.core.util.Factory
import org.bitcoins.spvnode.serializers.messages.RawTypeIdentifierSerializer

/**
  * Created by chris on 5/31/16.
  * This indicates the type of the object that has been hashed for an inventory
  * https://bitcoin.org/en/developer-reference#data-messages
  */
sealed trait TypeIdentifier extends NetworkElement {
  def num : UInt32
  override def hex = RawTypeIdentifierSerializer.write(this)
}

case object MsgTx extends TypeIdentifier {
  override def num = UInt32.one
}

case object MsgBlock extends TypeIdentifier {
  override def num = UInt32(2)
}

case object MsgFilteredBlock extends TypeIdentifier {
  override def num = UInt32(3)
}

sealed trait MsgUnassigned extends TypeIdentifier

object TypeIdentifier extends Factory[TypeIdentifier] {

  private case class MsgUnassignedImpl(num : UInt32) extends MsgUnassigned

  override def fromBytes(bytes : Seq[Byte]) : TypeIdentifier = RawTypeIdentifierSerializer.read(bytes)

  def apply(num : Long) : TypeIdentifier = TypeIdentifier(UInt32(num))

  def apply(uInt32 : UInt32) : TypeIdentifier = uInt32 match {
    case UInt32.one => MsgTx
    case x if (uInt32 == UInt32(2)) => MsgBlock
    case x if (uInt32 == UInt32(3)) => MsgFilteredBlock
    case x : UInt32 => MsgUnassignedImpl(x)
  }
}
