package org.bitcoins.spvnode.networking

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.LoggingReceive
import akka.io.{IO, Tcp}
import akka.util.{ByteString, CompactByteString}
import org.bitcoins.core.config.{NetworkParameters, TestNet3}
import org.bitcoins.core.util.{BitcoinSLogger, BitcoinSUtil}
import org.bitcoins.spvnode.NetworkMessage
import org.bitcoins.spvnode.headers.NetworkHeader
import org.bitcoins.spvnode.messages._
import org.bitcoins.spvnode.util.BitcoinSpvNodeUtil
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

  /**
    * The manager is an actor that handles the underlying low level I/O resources (selectors, channels)
    * and instantiates workers for specific tasks, such as listening to incoming connections.
    */
  def manager : ActorRef = IO(Tcp)(context.system)

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
  private def awaitNetworkRequest(peer: ActorRef) = LoggingReceive {

    case networkRequest: NetworkRequest =>
      handleNetworkRequest(networkRequest,peer)

    case peerRequest: PeerRequest =>

    case networkResponse: NetworkResponse =>
      logger.error("Client cannot receive network responses, PeerMessageHandler must receive them, received: " + networkResponse)
      throw new IllegalArgumentException("Client cannot receive network responses, PeerMessageHandler must receive them")
    case unknownMessage =>
      logger.error("Client recieved an unknown network message: "  + unknownMessage)
      throw new IllegalArgumentException("Unknown message for client: " + unknownMessage)
  }

  def receive  = LoggingReceive {
    case message : Tcp.Message => message match {
      case event : Tcp.Event =>
        logger.debug("Event: " + event)
        handleEvent(event)
      case command : Tcp.Command =>
        logger.debug("Command: " + command)
        handleCommand(command)
    }
    case unknownMessage =>
      logger.error("Client.receive recieved an unknown network message: "  + unknownMessage)
      throw new IllegalArgumentException("Unknown message for client: " + unknownMessage)
  }

  /**
    * This function is responsible for handling a [[Tcp.Event]] algebraic data type
    * @param event
    */
  private def handleEvent(event : Tcp.Event) = event match {
    case Tcp.Bound(localAddress) =>
      logger.debug("Actor is now bound to the local address: " + localAddress)
      listener ! Tcp.Bound(localAddress)
    case Tcp.CommandFailed(w: Tcp.Write) =>
      logger.debug("Client write command failed: " + Tcp.CommandFailed(w))
      logger.debug("O/S buffer was full")
      // O/S buffer was full
      //listener ! "write failed"
    case Tcp.CommandFailed(command) =>
      logger.debug("Client Command failed:" + command)
    case Tcp.Received(data) =>
      logger.debug("Received data from our peer on the network: " + BitcoinSUtil.encodeHex(data.toArray))
      //listener ! data
    case Tcp.Connected(remote, local) =>
      logger.debug("Tcp connection to: " + remote)
      logger.debug("Local: " + local)
      sender ! Tcp.Register(listener)
      listener ! Tcp.Connected(remote,local)
      context.become(awaitNetworkRequest(sender))
    case Tcp.ConfirmedClosed =>
      logger.debug("Client received confirmed closed msg: " + Tcp.ConfirmedClosed)
      context stop self
  }


  /**
    * This function is responsible for handling a [[Tcp.Command]] algebraic data type
    * @param command
    */
  private def handleCommand(command : Tcp.Command) = command match {
    case x => throw new IllegalArgumentException("Unknown command: " + x)
  }

  /**
    * Sends a network request to our peer on the network
    * @param request
    * @return
    */
  private def handleNetworkRequest(request : NetworkRequest, x: ActorRef) = {
    val header = NetworkHeader(TestNet3, request)
    val message = NetworkMessage(header,request)
    val byteMessage = BitcoinSpvNodeUtil.buildByteString(message.bytes)
    logger.debug("Network request: " + request)
    x ! Tcp.Write(byteMessage)
  }

}




object Client {
  private case class ClientImpl(remote: InetSocketAddress, network : NetworkParameters,
                                listener: ActorRef, actorSystem : ActorSystem) extends Client {

    manager ! Tcp.Bind(listener, new InetSocketAddress(network.port))
    //this eagerly connects the client with our peer on the network as soon
    //as the case class is instantiated
    manager ! Tcp.Connect(remote)

  }

  def props(remote : InetSocketAddress, network : NetworkParameters, listener : ActorRef)(implicit actorSystem: ActorSystem) : Props = {
    Props(classOf[ClientImpl], remote, network, listener, actorSystem)
  }

  def apply(remote : InetSocketAddress, network : NetworkParameters, listener : ActorRef)(implicit actorSystem: ActorSystem) : ActorRef = {
   actorSystem.actorOf(props(remote, network, listener))
  }

  def apply(network : NetworkParameters, listener : ActorRef)(implicit actorSystem: ActorSystem) : ActorRef = {
    //val randomSeed = ((Math.random() * 10) % network.dnsSeeds.size).toInt
    val remote = new InetSocketAddress(network.dnsSeeds(0), network.port)
    Client(remote, network, listener)
  }

}

