package org.bitcoins.spvnode.networking

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.io.{IO, Tcp}
import akka.util.{ByteString, CompactByteString}
import org.bitcoins.core.config.NetworkParameters
import org.bitcoins.core.util.{BitcoinSLogger, BitcoinSUtil}
import org.bitcoins.spvnode.messages._
import org.bitcoins.spvnode.util.NetworkIpAddress

import scala.concurrent.Future
/**
  * Created by chris on 6/6/16.
  */
sealed trait Client extends Actor with BitcoinSLogger {

  /**
    * The address of the peer we are attempting to connect to
    * on the p2p network
    * @return
    */
  def remote: InetSocketAddress

  /**
    * The actor that is listening to all communications between the
    * client and its peer on the network
    * @return
    */
  def listener : ActorRef


  def actorSystem : ActorSystem
  /**
    * The manager is an actor that handles the underlying low level I/O resources (selectors, channels)
    * and instantiates workers for specific tasks, such as listening to incoming connections.
    */
  def manager : ActorRef = IO(Tcp)(actorSystem)

  /**
    * The parameters for the network we are connected to
    * i.e. [[org.bitcoins.core.config.MainNet]] or [[org.bitcoins.core.config.TestNet3]]
    * @return
    */
  def network : NetworkParameters

  /**
    * This actor signifies the node we are connected to on the p2p network
    * This is set when we received a [[Tcp.Connected]] message
    */
  private var peer : Option[ActorRef] = None

  def receive = {
    case message : Tcp.Message => message match {
      case event : Tcp.Event => handleEvent(event)
      case command : Tcp.Command => handleCommand(command)
    }
    case networkMessage : NetworkPayload =>
      logger.debug("Recieved network message inside of Client: " + networkMessage)
      handleNetworkPayload(networkMessage)
    case unknownMessage => throw new IllegalArgumentException("Unknown message for client: " + unknownMessage)

/*    case data: ByteString =>
      logger.debug("Sending this data to our peer on the network: " + data)
      connection.get ! Tcp.Write(data)*/

  }
  def sendMessage(msg : NetworkRequest, peer : NetworkIpAddress) : Future[NetworkResponse] = ???

  /**
    * This function is responsible for handling a [[Tcp.Event]] algebraic data type
    * @param event
    */
  private def handleEvent(event : Tcp.Event) = event match {
    case Tcp.CommandFailed(w: Tcp.Write) =>
      logger.debug("Client write command failed: " + Tcp.CommandFailed(w))
      logger.debug("O/S buffer was full")
      // O/S buffer was full
      listener ! "write failed"
    case Tcp.CommandFailed(command) =>
      logger.debug("Client Command failed:" + command)
    case Tcp.Received(data) =>
      logger.debug("Received data from our peer on the network: " + Tcp.Received(data))
      listener ! data
    case Tcp.Connected(remote, local) =>
      logger.debug("Tcp connection to: " + remote)
      logger.debug("Local: " + local)
      listener ! Tcp.Connected(remote,local)
      peer = Some(sender)
      peer.get ! Tcp.Register(listener)
    case Tcp.ConfirmedClosed =>
      logger.debug("Client received confirmed closed msg: " + Tcp.ConfirmedClosed)
      peer = None
      context stop self
    case payload : NetworkPayload => handleNetworkPayload(payload)
  }
  /**
    * This function is responsible for handling a [[Tcp.Command]] algebraic data type
    * @param command
    */
  private def handleCommand(command : Tcp.Command) = command match {
    case Tcp.ConfirmedClose =>
      logger.debug("Client received connection closed msg: " + Tcp.ConfirmedClose)
      listener ! Tcp.ConfirmedClose
      peer.get ! Tcp.ConfirmedClose
  }
  /**
    * Function to handle all of our [[NetworkPayload]] on the p2p network.
    * @param message
    * @return
    */
  private def handleNetworkPayload(message : NetworkPayload) = message match {
    case request : NetworkRequest => handleNetworkRequest(request)
    case response : NetworkResponse => handleNetworkResponse(response)
  }

  /**
    * Sends a network request to our peer on the network
    * @param request
    * @return
    */
  private def handleNetworkRequest(request : NetworkRequest) = {
    val byteMessage = buildByteString(request.bytes)
    logger.debug("Network request: " + request)
    logger.debug("Byte message: " + BitcoinSUtil.encodeHex(request.bytes))
    logger.debug("Peer: " + peer)
    peer.map(p => p ! Tcp.Write(byteMessage))
  }

  /**
    * Sends a network response to the actor that is listening to our communications with
    * the peer we are connected to on the network
    * @param response
    */
  private def handleNetworkResponse(response : NetworkResponse) = listener ! response


  /**
    * Wraps our Seq[Byte] into an akka [[ByteString]] object
    * @param bytes
    * @return
    */
  private def buildByteString(bytes: Seq[Byte]) : ByteString = {
    CompactByteString(bytes.toArray)
  }
}


object Client {
  private case class ClientImpl(remote: InetSocketAddress, network : NetworkParameters,
                                listener: ActorRef, actorSystem : ActorSystem) extends Client {
    //this eagerly connects the client with our peer on the network as soon
    //as the case class is instantiated
    manager ! Tcp.Connect(remote, Some(new InetSocketAddress(remote.getPort)))
  }

  def props(remote : InetSocketAddress, network : NetworkParameters, listener : ActorRef, actorSystem : ActorSystem) : Props = {
    Props(classOf[ClientImpl], remote, network, listener, actorSystem)
  }

  def apply(remote : InetSocketAddress, network : NetworkParameters, listener : ActorRef, actorSystem : ActorSystem) : Actor = {
    ClientImpl(remote, network, listener, actorSystem)
  }

  def apply(network : NetworkParameters, listener : ActorRef, actorSystem : ActorSystem) : Actor = {
    //val randomSeed = ((Math.random() * 10) % network.dnsSeeds.size).toInt
    val remote = new InetSocketAddress(network.dnsSeeds(0), network.port)
    apply(remote, network, listener, actorSystem)
  }
}

