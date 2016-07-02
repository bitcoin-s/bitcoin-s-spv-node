package org.bitcoins.spvnode.constant

import akka.actor.ActorSystem

/**
  * Created by chris on 7/1/16.
  */
trait Constants {
  lazy val actorSystem = ActorSystem("BitcoinSpvNode")
}

object Constants
