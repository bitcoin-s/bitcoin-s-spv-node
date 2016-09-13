[![Build Status](https://travis-ci.org/bitcoin-s/bitcoin-s-spv-node.svg?branch=master)](https://travis-ci.org/bitcoin-s/bitcoin-s-spv-node) [![Coverage Status](https://coveralls.io/repos/github/bitcoin-s/bitcoin-s-spv-node/badge.svg?branch=master)](https://coveralls.io/github/bitcoin-s/bitcoin-s-spv-node?branch=master)

This is an implementation of an SPV node on the Bitcoin network using Scala & [Bitcoin-S-Core](https://github.com/bitcoin-s/bitcoin-s-core). 

Our implementation relies heavily on [Akka](http://akka.io/), which has an implementation of the [Actor model](https://en.wikipedia.org/wiki/Actor_model) in Scala. If you want to read more about Akka and what it is/how it is used, it is best to start reading [here](http://doc.akka.io/docs/akka/2.4/scala.html).
# Examples

Look inside of [Main.scala](https://github.com/Christewart/bitcoin-s-spv-node/blob/networking/src/main/scala/org/bitcoins/spvnode/Main.scala) for example of creating a [`PaymentActor`](https://github.com/Christewart/bitcoin-s-spv-node/blob/networking/src/main/scala/org/bitcoins/spvnode/networking/PaymentActor.scala), that montiors an address. Once a transaction that pays to the address is included in a block, it sends a message back to your actor saying a payment was successful. 

```scala
package org.bitcoins.spvnode

import org.bitcoins.core.config.TestNet3
import org.bitcoins.core.crypto.Sha256Hash160Digest
import org.bitcoins.core.protocol.P2PKHAddress
import org.bitcoins.spvnode.constant.Constants
import org.bitcoins.spvnode.networking.PaymentActor

/**
  * Created by chris on 8/29/16.
  */
object Main extends App {


  override def main(args : Array[String]) = {
    //note this needs to be a TestNet3 address, unless you modify the network inside of 
    //https://github.com/Christewart/bitcoin-s-spv-node/blob/networking/src/main/scala/org/bitcoins/spvnode/constant/Constants.scala#L15
    val address = BitcoinAddress("mmUW4R8SKtRA2uEKhiw5m3DsUYV76bsMZ9")
    val paymentActor = PaymentActor(Constants.actorSystem)
    paymentActor ! address
  }
}
```

If you want to see more logging for the networking stuff, adjust your [logback.xml](https://github.com/Christewart/bitcoin-s-spv-node/blob/networking/src/main/resources/logback.xml#L18) file to DEBUG.

After that, you are ready to fire up your spv node with this command:

```bash
chris@chris-870Z5E-880Z5E-680Z5E:~/dev/bitcoins-spv-node$ sbt run
```

After firing up the node, and seeing some logging output, you can make a payment to the address you specified above. You should see logging output indicating your transaction was seen on the network, and receive a [PaymentActor.SuccessfulPayment](https://github.com/Christewart/bitcoin-s-spv-node/blob/networking/src/main/scala/org/bitcoins/spvnode/networking/PaymentActor.scala#L145) when the transaction paying to your address is included in a block. 

NOTE: Block times are HIGHLY variable on testnet, it can take 30 minutes for a block to be mined -- which means you would not see a `PaymentActor.SuccessfulPayment` message for 30 minutes.


