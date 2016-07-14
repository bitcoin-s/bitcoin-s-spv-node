/*
package org.bitcoins.spvnode.networking

import akka.actor.{Actor, ActorContext, ActorRef, Props}
import org.bitcoins.spvnode.util.BitcoinSpvNodeUtil

/**
  * Created by chris on 7/14/16.
  */
sealed trait AddressManager extends Actor {

  def receive: Receive = {

  }
}


object AddressManager {

  def props: Props = Props(AddressManagerImpl())

  def apply(context: ActorContext): ActorRef = context.actorOf(props, BitcoinSpvNodeUtil.createActorName(this.getClass))
}
*/
