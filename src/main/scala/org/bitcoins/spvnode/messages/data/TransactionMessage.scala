package org.bitcoins.spvnode.messages.data

import org.bitcoins.core.protocol.transaction.Transaction
import org.bitcoins.core.util.Factory
import org.bitcoins.spvnode.messages._
import org.bitcoins.spvnode.serializers.messages.data.RawTransactionMessageSerializer

/**
  * Created by chris on 6/2/16.
  * Companion factory object for the TransactionMessage on the p2p network
  * https://bitcoin.org/en/developer-reference#tx
  */
object TransactionMessage extends Factory[TransactionMessage] {

  private case class TransactionMessageImpl(transaction : Transaction) extends TransactionMessage

  def fromBytes(bytes : Seq[Byte]) : TransactionMessage = RawTransactionMessageSerializer.read(bytes)

  def apply(transaction: Transaction) : TransactionMessage = TransactionMessageImpl(transaction)
}
