package org.bitcoins.spvnode.util

/**
  * Created by chris on 5/31/16.
  * Encapsulated network IP address currently uses the following structure
  */
sealed trait NetworkIpAddress {

  /**
    * Added in protocol version 31402.
    * A time in Unix epoch time format. Nodes advertising their own IP address set this to the current time.
    * Nodes advertising IP addresses they’ve connected to set this to the last time they connected to that node.
    * Other nodes just relaying the IP address should not change the time.
    * Nodes can use the time field to avoid relaying old addr messages.
    * Malicious nodes may change times or even set them in the future.
    * @return
    */
  def time : Long

  /**
    * The services the node advertised in its version message.
    * @return
    */
  def services : BigInt

  /**
    * IPv6 address in big endian byte order.
    * IPv4 addresses can be provided as IPv4-mapped IPv6 addresses
    * @return
    */
  def address : String

  /**
    * Port number in big endian byte order.
    * Note that Bitcoin Core will only connect to nodes with non-standard port numbers as
    * a last resort for finding peers. This is to prevent anyone from trying to use the
    * network to disrupt non-Bitcoin services that run on other ports.
    * @return
    */
  def port : Int
}


object NetworkIpAddress {
  private case class NetworkIpAddressImpl(time : Long, services : BigInt,
                                          address : String, port : Int) extends NetworkIpAddress

  def apply(time : Long, services : BigInt, address : String, port : Int) : NetworkIpAddress = {
    NetworkIpAddressImpl(time,services,address,port)
  }
}


