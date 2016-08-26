package org.bitcoins.spvnode.messages.data

import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.protocol.NetworkElement
import org.bitcoins.core.util.{BitcoinSLogger, Factory}
import org.bitcoins.spvnode.messages.TypeIdentifier
import org.bitcoins.spvnode.serializers.messages.data.RawInventorySerializer

/**
  * Created by chris on 5/31/16.
  * These are used as unique identifiers inside the peer-to-peer network
  * [[https://bitcoin.org/en/developer-reference#term-inventory]]
  */
trait Inventory extends NetworkElement with BitcoinSLogger {

  /**
    * The type of object which was hashed
    * @return
    */
  def typeIdentifier : TypeIdentifier

  /**
    * SHA256(SHA256()) hash of the object in internal byte order.
    * @return
    */
  def hash : DoubleSha256Digest

  override def hex = RawInventorySerializer.write(this)
}

object Inventory extends Factory[Inventory] {

  private case class InventoryImpl(typeIdentifier: TypeIdentifier, hash : DoubleSha256Digest) extends Inventory

  override def fromBytes(bytes : Seq[Byte]) : Inventory = RawInventorySerializer.read(bytes)

  def apply(typeIdentifier: TypeIdentifier, hash : DoubleSha256Digest) : Inventory = {
    InventoryImpl(typeIdentifier,hash)
  }
}
