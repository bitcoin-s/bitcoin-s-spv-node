package org.bitcoins.spvnode.messages

import java.net.InetAddress

import org.bitcoins.core.crypto.{DoubleSha256Digest, ECDigitalSignature}
import org.bitcoins.core.protocol.blockchain.BlockHeader
import org.bitcoins.core.protocol.transaction.Transaction
import org.bitcoins.core.protocol.{CompactSizeUInt, NetworkElement}
import org.bitcoins.spvnode.messages.control.{Alert, ServiceIdentifier}
import org.bitcoins.spvnode.messages.data.Inventory
import org.bitcoins.spvnode.serializers.control.{RawAddrMessageSerializer, RawVersionMessageSerializer}
import org.bitcoins.spvnode.serializers.messages.data._
import org.bitcoins.spvnode.util.NetworkIpAddress
import org.bitcoins.spvnode.versions.ProtocolVersion

/**
  * Created by chris on 5/31/16.
  */
sealed trait NetworkMessage extends NetworkElement

/**
  * Represents a message that is sending a request to another node on the network
  */
sealed trait NetworkRequest

/**
  * Represents a message that is response to a request that was sent
  */
sealed trait NetworkResponse
/**
  * Represents a data message inside of bitcoin core
  * https://bitcoin.org/en/developer-reference#data-messages
  */
sealed trait DataMessage extends NetworkMessage

/**
  * The block message transmits a single serialized block
  * https://bitcoin.org/en/developer-reference#block
  */
sealed trait BlockMessage extends DataMessage with NetworkResponse

/**
  * The getblocks message requests an inv message that provides block header hashes
  * starting from a particular point in the block chain.
  * It allows a peer which has been disconnected or started for the first time to get the data
  * it needs to request the blocks it hasn’t seen.
  * https://bitcoin.org/en/developer-reference#getblocks
  */
trait GetBlocksMessage extends DataMessage with NetworkRequest {
  /**
    * The protocol version number; the same as sent in the version message.
    * @return
    */
  def protocolVersion : ProtocolVersion

  /**
    * The number of header hashes provided not including the stop hash.
    * There is no limit except that the byte size of the entire message
    * must be below the MAX_SIZE limit; typically from 1 to 200 hashes are sent.
    * @return
    */
  def hashCount : CompactSizeUInt

  /**
    * One or more block header hashes (32 bytes each) in internal byte order.
    * Hashes should be provided in reverse order of block height,
    * so highest-height hashes are listed first and lowest-height hashes are listed last.
    * @return
    */
  def blockHeaderHashes : Seq[DoubleSha256Digest]

  /**
    * The header hash of the last header hash being requested;
    * set to all zeroes to request an inv message with all subsequent
    * header hashes (a maximum of 500 will be sent as a reply to this message;
    * if you need more than 500, you will need to send another getblocks message
    * with a higher-height header hash as the first entry in block header hash field).
    * @return
    */
  def stopHash : DoubleSha256Digest

  def hex : String = RawGetBlocksMessageSerializer.write(this)
}

/**
  * The getdata message requests one or more data objects from another node.
  * The objects are requested by an inventory,
  * which the requesting node typically previously received by way of an inv message.
  * https://bitcoin.org/en/developer-reference#getdata
  */
sealed trait GetDataMessage extends DataMessage with NetworkRequest

/**
  * The getheaders message requests a headers message that provides block headers starting
  * from a particular point in the block chain.
  * It allows a peer which has been disconnected or started for the first time to get the
  * headers it hasn’t seen yet.
  * https://bitcoin.org/en/developer-reference#getheaders
  */
sealed trait GetHeadersMessage extends DataMessage with NetworkRequest

/**
  * The headers message sends one or more block headers to a node
  * which previously requested certain headers with a getheaders message.
  * https://bitcoin.org/en/developer-reference#headers
  */
sealed trait HeadersMessage extends DataMessage with NetworkResponse {
  /**
    * Number of block headers up to a maximum of 2,000.
    * Note: headers-first sync assumes the sending node
    * will send the maximum number of headers whenever possible.
    * @return
    */
  def count : CompactSizeUInt

  /**
    * Block headers: each 80-byte block header is in the format described in the
    * block headers section with an additional 0x00 suffixed.
    * This 0x00 is called the transaction count, but because the headers message
    * doesn’t include any transactions, the transaction count is always zero.
    * @return
    */
  def headers : Seq[BlockHeader]
}

/**
  * The inv message (inventory message) transmits one or more inventories of objects known to the transmitting peer.
  * It can be sent unsolicited to announce new transactions or blocks,
  * or it can be sent in reply to a getblocks message or mempool message.
  * https://bitcoin.org/en/developer-reference#inv
  */
trait InventoryMessage extends DataMessage with NetworkRequest with NetworkResponse {
  /**
    * The number of inventory enteries
    * @return
    */
  def inventoryCount : CompactSizeUInt

  /**
    * One or more inventory entries up to a maximum of 50,000 entries.
    * @return
    */
  def inventories : Seq[Inventory]

  def hex = RawInventoryMessageSerializer.write(this)
}

/**
  * The mempool message requests the TXIDs of transactions that the receiving node has verified
  * as valid but which have not yet appeared in a block.
  * That is, transactions which are in the receiving node’s memory pool.
  * The response to the mempool message is one or more inv messages containing the TXIDs in the usual inventory format.
  * https://bitcoin.org/en/developer-reference#mempool
  */
case object MemPoolMessage extends DataMessage with NetworkRequest {
  def hex = ""
}

/**
  * The merkleblock message is a reply to a getdata message which requested a
  * block using the inventory type MSG_MERKLEBLOCK.
  * It is only part of the reply: if any matching transactions are found,
  * they will be sent separately as tx messages.
  * https://bitcoin.org/en/developer-reference#merkleblock
  */
trait MerkleBlockMessage extends DataMessage with NetworkResponse {

  /**
    * The block header associated with our merkle block message
    * @return
    */
  def blockHeader : BlockHeader
  /**
    * The number of transactions in the block (including ones that don’t match the filter).
    * @return
    */
  def transactionCount : Long

  /**
    * The number of hashes in the following field
    * @return
    */
  def hashCount : CompactSizeUInt

  /**
    * One or more hashes of both transactions and merkle nodes in internal byte order. Each hash is 32 bits.
    * @return
    */
  def hashes : Seq[DoubleSha256Digest]

  /**
    * The number of flag bytes in the following field.
    * @return
    */
  def flagCount : CompactSizeUInt

  /**
    * A sequence of bits packed eight in a byte with the least significant bit first.
    * May be padded to the nearest byte boundary but must not contain any more bits than that.
    * Used to assign the hashes to particular nodes in the merkle tree as described below.
    * @return
    */
  def flags : Seq[Byte]

  def hex = RawMerkleBlockMessageSerializer.write(this)

}

/**
  * The notfound message is a reply to a getdata message which requested an object the receiving
  * node does not have available for relay. (Nodes are not expected to relay historic transactions
  * which are no longer in the memory pool or relay set.
  * Nodes may also have pruned spent transactions from older blocks, making them unable to send those blocks.)
  * https://bitcoin.org/en/developer-reference#notfound
  */
trait NotFoundMessage extends DataMessage with NetworkResponse with InventoryMessage {
  override def hex = RawNotFoundMessageSerializer.write(this)
}

/**
  * The tx message transmits a single transaction in the raw transaction format.
  * It can be sent in a variety of situations;
  * https://bitcoin.org/en/developer-reference#tx
  */
trait TransactionMessage extends DataMessage with NetworkResponse {
  def transaction : Transaction
  override def hex = RawTransactionMessageSerializer.write(this)
}





/**
  * Represents a control message on this network
  * https://bitcoin.org/en/developer-reference#control-messages
  */
sealed trait ControlMessage extends NetworkMessage

/**
  * The addr (IP address) message relays connection information for peers on the network.
  * Each peer which wants to accept incoming connections creates an addr message providing its
  * connection information and then sends that message to its peers unsolicited.
  * Some of its peers send that information to their peers (also unsolicited),
  * some of which further distribute it, allowing decentralized peer discovery for
  * any program already on the network.
  * https://bitcoin.org/en/developer-reference#addr
  */
trait AddrMessage extends ControlMessage with NetworkResponse with NetworkRequest {
  def ipCount : CompactSizeUInt
  def addresses : Seq[NetworkIpAddress]
  override def hex = RawAddrMessageSerializer.write(this)
}

/**
  * The alert message warns nodes of problems that may affect them or the rest of the network.
  * Each alert message is signed using a key controlled by respected community members,
  * mostly Bitcoin Core developers.
  */
sealed trait AlertMessage extends ControlMessage with NetworkResponse {
  def alertSize : CompactSizeUInt
  def alert : Alert
  def signatureSize : CompactSizeUInt
  def signature : ECDigitalSignature
}


/**
  * The filteradd message tells the receiving peer to add a single element to a
  * previously-set bloom filter, such as a new public key.
  * The element is sent directly to the receiving peer; the peer then uses the parameters
  * set in the filterload message to add the element to the bloom filter.
  * https://bitcoin.org/en/developer-reference#filteradd
  */
sealed trait FilterAddMessage extends ControlMessage with NetworkResponse {

  /**
    * The number of bytes in the following element field.
    * @return
    */
  def elementSize : CompactSizeUInt

  /**
    * The element to add to the current filter.
    * Maximum of 520 bytes, which is the maximum size of an element which can be pushed
    * onto the stack in a pubkey or signature script.
    * Elements must be sent in the byte order they would use when appearing in a raw transaction;
    * for example, hashes should be sent in internal byte order.
    * @return
    */
  def element : Seq[Byte]
}


/**
  * The filterclear message tells the receiving peer to remove a previously-set bloom filter.
  * This also undoes the effect of setting the relay field in the version message to 0,
  * allowing unfiltered access to inv messages announcing new transactions.
  * https://bitcoin.org/en/developer-reference#filterclear
  */
sealed trait FilterClearMessage extends ControlMessage with NetworkResponse

/**
  * The filterload message tells the receiving peer to filter all relayed transactions and
  * requested merkle blocks through the provided filter.
  * This allows clients to receive transactions relevant to their wallet plus a configurable
  * rate of false positive transactions which can provide plausible-deniability privacy.
  * https://bitcoin.org/en/developer-reference#filterload
  */
sealed trait FilterLoadMessage extends ControlMessage with NetworkResponse {

  /**
    * Number of bytes in the following filter bit field.
    * @return
    */
  def filterBytes : Int

  /**
    * A bit field of arbitrary byte-aligned size. The maximum size is 36,000 bytes.
    * @return
    */
  def filter : Int

  /**
    * The number of hash functions to use in this filter. The maximum value allowed in this field is 50.
    * @return
    */
  def hashFuncs : Long

  /**
    * An arbitrary value to add to the seed value in the hash function used by the bloom filter.
    * @return
    */
  def tweak : Long

  /**
    * A set of flags that control how outpoints corresponding to a matched pubkey script are added to the filter.
    * See the table in the Updating A Bloom Filter subsection below.
    * @return
    */
  def flags : Int
}

/**
  * The getaddr message requests an addr message from the receiving node,
  * preferably one with lots of IP addresses of other receiving nodes.
  * The transmitting node can use those IP addresses to quickly update its
  * database of available nodes rather than waiting for unsolicited addr messages to arrive over time.
  * https://bitcoin.org/en/developer-reference#getaddr
  */
sealed trait GetAddressMessage extends ControlMessage with NetworkRequest

/**
  * The ping message helps confirm that the receiving peer is still connected.
  * If a TCP/IP error is encountered when sending the ping message (such as a connection timeout),
  * the transmitting node can assume that the receiving node is disconnected.
  * The response to a ping message is the pong message.
  * https://bitcoin.org/en/developer-reference#ping
  */
sealed trait PingMessage extends ControlMessage with NetworkRequest {
  /**
    * Random nonce assigned to this ping message.
    * The responding pong message will include this nonce
    * to identify the ping message to which it is replying.
    * @return
    */
  def nonce : BigInt
}

/**
  * The pong message replies to a ping message, proving to the pinging node that the ponging node is still alive.
  * Bitcoin Core will, by default, disconnect from any clients which have not responded
  * to a ping message within 20 minutes.
  * https://bitcoin.org/en/developer-reference#pong
  */
sealed trait PongMessage extends ControlMessage with NetworkResponse {

  /**
    * The nonce which is the nonce in the ping message the peer is responding too
    * @return
    */
  def nonce : BigInt
}

/**
  * The reject message informs the receiving node that one of its previous messages has been rejected.
  * https://bitcoin.org/en/developer-reference#reject
  */
sealed trait RejectMessage extends ControlMessage with NetworkResponse {
  /**
    * The number of bytes in the following message field.
    * @return
    */
  def messageSize : CompactSizeUInt

  /**
    * The type of message rejected as ASCII text without null padding.
    * For example: “tx”, “block”, or “version”.
    * @return
    */
  def message : String

  /**
    * The reject message code.
    * @return
    */
  def code : Char

  /**
    * The number of bytes in the following reason field.
    * May be 0x00 if a text reason isn’t provided.
    * @return
    */
  def reasonSize : CompactSizeUInt

  /**
    * The reason for the rejection in ASCII text.
    * This should not be displayed to the user; it is only for debugging purposes.
    * @return
    */
  def reason : String

  /**
    * Optional additional data provided with the rejection.
    * For example, most rejections of tx messages or block messages include
    * the hash of the rejected transaction or block header. See the code table below.
    * @return
    */
  def extra : String
}

/**
  * The sendheaders message tells the receiving peer to send new block announcements
  * using a headers message rather than an inv message.
  * There is no payload in a sendheaders message. See the message header section for an example
  * of a message without a payload.
  * https://bitcoin.org/en/developer-reference#sendheaders
  */
sealed trait SendHeadersMessage extends ControlMessage with NetworkResponse


/**
  * The verack message acknowledges a previously-received version message,
  * informing the connecting node that it can begin to send other messages.
  * The verack message has no payload; for an example of a message with no payload,
  * see the message headers section.
  * https://bitcoin.org/en/developer-reference#verack
  */
sealed trait VerAckMessage extends ControlMessage with NetworkResponse


/**
  * The version message provides information about the transmitting node to the
  * receiving node at the beginning of a connection.
  * Until both peers have exchanged version messages, no other messages will be accepted.
  * If a version message is accepted, the receiving node should send a verack message—but
  * no node should send a verack message before initializing its half of the connection
  * by first sending a version message.
  * https://bitcoin.org/en/developer-reference#version
  */
trait VersionMessage extends ControlMessage with NetworkResponse {

  /**
    * The highest protocol version understood by the transmitting node. See the protocol version section.
    * @return
    */
  def version : ProtocolVersion

  /**
    * The services supported by the transmitting node encoded as a bitfield. See the list of service codes below.
    * @return
    */
  def services : ServiceIdentifier

  /**
    * The current Unix epoch time according to the transmitting node’s clock.
    * Because nodes will reject blocks with timestamps more than two hours in the future,
    * this field can help other nodes to determine that their clock is wrong.
    * @return
    */
  def timestamp : Long

  /**
    * The services supported by the receiving node as perceived by the transmitting node.
    * Same format as the ‘services’ field above.
    * Bitcoin Core will attempt to provide accurate information. BitcoinJ will, by default, always send 0.
    * @return
    */
  def addressReceiveServices : ServiceIdentifier

  /**
    * The IPv6 address of the receiving node as perceived by the transmitting node in big endian byte order.
    * IPv4 addresses can be provided as IPv4-mapped IPv6 addresses.
    * Bitcoin Core will attempt to provide accurate information
    * BitcoinJ will, by default, always return ::ffff:127.0.0.1
    */
  def addressReceiveIpAddress : InetAddress

  /**
    * The port number of the receiving node as perceived by the transmitting node in big endian byte order.
    * @return
    */
  def addressReceivePort : Int

  /**
    * The services supported by the transmitting node. Should be identical to the ‘services’ field above.
    * @return
    */
  def addressTransServices : ServiceIdentifier

  /**
    * The IPv6 address of the transmitting node in big endian byte order.
    * IPv4 addresses can be provided as IPv4-mapped IPv6 addresses.
    * Set to ::ffff:127.0.0.1 if unknown.
    * @return
    */
  def addressTransIpAddress : InetAddress

  /**
    * The port number of the transmitting node in big endian byte order.
    * @return
    */
  def addressTransPort : Int

  /**
    * A random nonce which can help a node detect a connection to itself.
    * If the nonce is 0, the nonce field is ignored.
    * If the nonce is anything else, a node should terminate the connection on receipt
    * of a version message with a nonce it previously sent.
    * @return
    */
  def nonce : BigInt

  /**
    * Number of bytes in following user_agent field. If 0x00, no user agent field is sent.
    * @return
    */
  def userAgentSize : CompactSizeUInt

  /**
    * User agent as defined by BIP14. Pkqreviously called subVer.
    * @return
    */
  def userAgent : String

  /**
    * The height of the transmitting node’s best block chain or,
    * in the case of an SPV client, best block header chain.
    * @return
    */
  def startHeight : Int

  /**
    * Transaction relay flag. If 0x00, no inv messages or tx messages announcing new transactions
    * should be sent to this client until it sends a filterload message or filterclear message.
    * If 0x01, this node wants inv messages and tx messages announcing new transactions.
    * @return
    */
  def relay : Boolean

  override def hex = RawVersionMessageSerializer.write(this)
}