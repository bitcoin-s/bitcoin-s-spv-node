package org.bitcoins.spvnode.util

import org.bitcoins.core.protocol.transaction.Transaction
import org.bitcoins.spvnode.NetworkMessage
import org.bitcoins.spvnode.messages.control.VersionMessage

/**
  * Created by chris on 6/2/16.
  */
trait TestUtil {

  //txid on testnet 44e504f5b7649d215be05ad9f09026dee95201244a3b218013c504a6a49a26ff
  //this tx has multiple inputs and outputs
  def rawTransaction = "01000000" +
    "02df80e3e6eba7dcd4650281d3c13f140dafbb823a7227a78eb6ee9f6cedd040011b0000006a473044022040f91c48f4011bf2e2edb6621bfa8fb802241de939cb86f1872c99c580ef0fe402204fc27388bc525e1b655b5f5b35f9d601d28602432dd5672f29e0a47f5b8bbb26012102c114f376c98d12a0540c3a81ab99bb1c5234245c05e8239d09f48229f9ebf011ffffffff" +
    "df80e3e6eba7dcd4650281d3c13f140dafbb823a7227a78eb6ee9f6cedd04001340000006b483045022100cf317c320d078c5b884c44e7488825dab5bcdf3f88c66314ac925770cd8773a7022033fde60d33cc2842ea73fce5d9cf4f8da6fadf414a75b7085efdcd300407f438012102605c23537b27b80157c770cd23e066cd11db3800d3066a38b9b592fc08ae9c70ffffffff" +
    "02c02b00000000000017a914b0b06365c482eb4eabe6e0630029fb8328ea098487e81c0000000000001976a914938da2b50fd6d8acdfa20e30df0e7d8092f0bc7588ac00000000"
  def transaction = Transaction(rawTransaction)

  /**
    * Random version message bitcoins created when connecting to a testnet seed
    * This does not include the header
    * @return
    */
  def rawVersionMessage = "7c1101000000000000000000d805833655010000000000000000000000000000000000000000ffff0a940106479d010000000000000000000000000000000000ffff739259bb479d0000000000000000182f626974636f696e732d7370762d6e6f64652f302e302e310000000000"
  def versionMessage = VersionMessage(rawVersionMessage)

  /**
    * This is a raw network message indicating the version a node is using on the p2p network
    * This has BOTH the header and the payload
    * @return
    */
  def rawNetworkMessage = "0b11090776657273696f6e0000000000660000002f6743da721101000100000000000000e0165b5700000000010000000000000000000000000000000000ffffad1f27a8479d010000000000000000000000000000000000ffff00000000479d68dc32a9948d149b102f5361746f7368693a302e31312e322f7f440d0001"
  def networkMessage = NetworkMessage(rawNetworkMessage)
}

object TestUtil extends TestUtil