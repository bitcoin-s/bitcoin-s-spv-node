package org.bitcoins.spvnode.gen

import java.net.{InetAddress, InetSocketAddress}

import org.bitcoins.core.gen.{NumberGenerator, StringGenerators}
import org.bitcoins.core.number.UInt32
import org.bitcoins.spvnode.messages.control._
import org.bitcoins.spvnode.messages.{FilterLoadMessage, PingMessage, PongMessage, VersionMessage}
import org.bitcoins.spvnode.versions.ProtocolVersion
import org.scalacheck.Gen

/**
  * Created by chris on 6/27/16.
  */
trait ControlMessageGenerator {

  def versionMessage : Gen[VersionMessage] = for {
    version <- protocolVersion
    identifier <- serviceIdentifier
    timestamp <- NumberGenerator.int64s
    addressReceiveServices <- serviceIdentifier
    addressReceiveIpAddress <- inetAddress
    addressReceivePort <- portNumber
    addressTransServices <- serviceIdentifier
    addressTransIpAddress <- inetAddress
    addressTransPort <- portNumber
    nonce <- NumberGenerator.uInt64s
    userAgent <- StringGenerators.genString
    startHeight <- NumberGenerator.int32s
    relay = scala.util.Random.nextInt() % 2 == 0
  } yield VersionMessage(version, identifier, timestamp, addressReceiveServices, addressReceiveIpAddress, addressReceivePort,
    addressTransServices, addressTransIpAddress, addressTransPort, nonce, userAgent, startHeight, relay)



  def pingMessage: Gen[PingMessage] = for {
    uInt64 <- NumberGenerator.uInt64s
  } yield PingMessage(uInt64)

  def pongMessage: Gen[PongMessage] = for {
    uInt64 <- NumberGenerator.uInt64s
  } yield PongMessage(uInt64)

  def protocolVersion : Gen[ProtocolVersion] = for {
    randomNum <- Gen.choose(0,ProtocolVersion.versions.length-1)
  } yield ProtocolVersion.versions(randomNum)

  def serviceIdentifier: Gen[ServiceIdentifier] = for {
    //service identifiers can only be NodeNetwork or UnnamedService
    randomNum <- Gen.choose(0,1)
  } yield ServiceIdentifier(randomNum)


  def inetAddress : Gen[InetAddress] = for {
    socketAddress <- inetSocketAddress
  } yield socketAddress.getAddress


  def inetSocketAddress : Gen[InetSocketAddress] = for {
    p <- portNumber
  } yield new InetSocketAddress(p)

  def portNumber : Gen[Int] = Gen.choose(0,65535)

  def filterLoadMessage: Gen[FilterLoadMessage] = for {
    filter <- NumberGenerator.bytes
    hashFuncs <- Gen.choose(0,50)
    tweak <- NumberGenerator.uInt32s
    flags <- BloomFilterGenerator.bloomFlag
  } yield FilterLoadMessage(filter,UInt32(hashFuncs), tweak, flags)

}

object ControlMessageGenerator extends ControlMessageGenerator



