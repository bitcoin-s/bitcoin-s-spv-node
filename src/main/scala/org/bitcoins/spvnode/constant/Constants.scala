package org.bitcoins.spvnode.constant

import akka.actor.ActorSystem
import org.bitcoins.core.config.TestNet3
import org.bitcoins.spvnode.messages.control.VersionMessage
import org.bitcoins.spvnode.versions.{ProtocolVersion70002, ProtocolVersion70012}

import scala.concurrent.duration.DurationInt

/**
  * Created by chris on 7/1/16.
  */
trait Constants {
  lazy val actorSystem = ActorSystem("BitcoinSpvNode")
  def networkParameters = TestNet3
  def version = ProtocolVersion70012
  def versionMessage = VersionMessage(networkParameters)
  def timeout = 5.seconds

  def userAgent = "/bitcoins-spv-node/0.0.1"

  /** This is the file where our block headers are stored */
  def blockHeaderFile = new java.io.File("src/main/resources/block_headers.dat")
}

object Constants extends Constants
