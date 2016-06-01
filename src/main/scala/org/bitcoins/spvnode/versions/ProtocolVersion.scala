package org.bitcoins.spvnode.versions

/**
  * Created by chris on 6/1/16.
  * The peer to peer network has versions to allow for new operations
  * Here are the currently protocol versions in the network
  * https://bitcoin.org/en/developer-reference#protocol-versions
  */
sealed trait ProtocolVersion

/**
  * Added receive IP address fields to version message.
  * Bitcoin Core 0.1.6 (Oct 2009)
  */
case object ProtocolVersion106 extends ProtocolVersion

/**
  * Added checksum field to message headers.
  * Bitcoin Core 0.2.9 (May 2010)
  */
case object ProtocolVersion209 extends ProtocolVersion

/**
  * Added alert message
  * Bitcion Core 0.3.11 (Aug 2010)
  */
case object ProtocolVersion311 extends ProtocolVersion

/**
  * Added time field to addr message.
  * Bitcoin Core 0.3.15 (Oct 2010)
  */
case object ProtocolVersion31402 extends ProtocolVersion

/**
  * Added getheaders message and headers message.
  * Bitcoin Core 0.3.18 (Dec 2010)
  */
case object ProtocolVersion31800 extends ProtocolVersion

/**
  * BIP14: Separated protocol version from Bitcoin Core version
  * Bitcoin Core 0.6.0 (Mar 2012)
  */
case object ProtocolVersion60000 extends ProtocolVersion

/**
  * BIP31: Added nonce field to ping message, Added pong message
  * Bitcoin Core 0.6.1 (May 2012)
  */
case object ProtocolVersion60001 extends ProtocolVersion

/**
  * BIP35: Added mempool message.
  • Extended getdata message to allow download of memory pool transactions
  * Bitcoin Core 0.7.0 (Sep 2012)
  */
case object ProtocolVersion60002 extends ProtocolVersion


/**
  * Added notfound message.
  * BIP37:
  • Added filterload message.
  • Added filteradd message.
  • Added filterclear message.
  • Added merkleblock message.
  • Added relay field to version message
  • Added MSG_FILTERED_BLOCK inventory type to getdata message.
  * Bitcoin Core 0.8.0 (Feb 2013)
  */
case object ProtocolVersion70001 extends ProtocolVersion

/**
  * Send multiple inv messages in response to a mempool message if necessary
  * BIP61: Add reject message
  * Bitcoin Core 0.9.0 (Mar 2014)
  */
case object ProtocolVersion70002 extends ProtocolVersion

/**
  * BIP130: Add sendheaders message
  * Bitcoin Core 0.12.0
  */
case object ProtocolVersion70012 extends ProtocolVersion