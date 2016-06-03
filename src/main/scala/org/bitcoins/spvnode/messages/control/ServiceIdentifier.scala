package org.bitcoins.spvnode.messages.control

import org.bitcoins.core.protocol.NetworkElement
import org.bitcoins.core.util.Factory
import org.bitcoins.spvnode.serializers.control.RawServiceIdentifierSerializer

/**
  * Created by chris on 6/2/16.
  */
sealed trait ServiceIdentifier extends NetworkElement {
  def num : BigInt
  override def hex = RawServiceIdentifierSerializer.write(this)
}

/**
  * This node is not a full node.
  * It may not be able to provide any data except for the transactions it originates.
  */
case object UnnamedService extends ServiceIdentifier {
  def num = 0
}

/**
  * This is a full node and can be asked for full blocks.
  * It should implement all protocol features available in its self-reported protocol version.
  */
case object NodeNetwork extends ServiceIdentifier {
  def num = 1
}

/**
  * Designated type for any service that does not have value of 0 or 1
  */
sealed trait UnknownService extends ServiceIdentifier

object ServiceIdentifier extends Factory[ServiceIdentifier] {

  private case class UnknownServiceImpl(num : BigInt) extends UnknownService

  def fromBytes(bytes : Seq[Byte]) : ServiceIdentifier = RawServiceIdentifierSerializer.read(bytes)

  def apply(num : BigInt) = num match {
    case _ if num == BigInt(0) => UnnamedService
    case _ if num == BigInt(1) => NodeNetwork
    case x : BigInt => UnknownServiceImpl(x)
  }
}


