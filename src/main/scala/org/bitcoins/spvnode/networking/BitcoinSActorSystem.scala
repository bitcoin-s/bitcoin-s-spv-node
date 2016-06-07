package org.bitcoins.spvnode.networking

import akka.actor.ActorSystem
import org.bitcoins.spvnode.BuildInfo
/**
  * Created by chris on 6/6/16.
  */
trait BitcoinSActorSystem {
  implicit lazy val actorSystem : ActorSystem = ActorSystem(BuildInfo.name)
}

object BitcoinSActorSystem extends BitcoinSActorSystem
