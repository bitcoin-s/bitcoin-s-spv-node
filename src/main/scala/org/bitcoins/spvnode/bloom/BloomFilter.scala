package org.bitcoins.spvnode.bloom

import org.bitcoins.core.crypto.DoubleSha256Digest
import org.bitcoins.core.number.{UInt32, UInt64}
import org.bitcoins.core.protocol.{CompactSizeUInt, NetworkElement}
import org.bitcoins.core.util.{BitcoinSLogger, BitcoinSUtil, Factory, NumberUtil}
import org.bitcoins.spvnode.serializers.control.RawBloomFilterSerializer

import scala.annotation.tailrec
import scala.util.hashing.MurmurHash3

/**
  * Created by chris on 8/2/16.
  */
sealed trait BloomFilter extends NetworkElement with BitcoinSLogger {

  /** How large the bloom filter is, in Bytes */
  def filterSize: CompactSizeUInt

  /** The bits that are set inside of the bloom filter */
  def data: Seq[Byte]

  /** The number of hash functions used in the bloom filter */
  def hashFuncs: UInt32

  /** An arbitrary value to add to the seed value in the hash function used by the bloom filter. */
  def tweak: UInt32

  /** A set of flags that control how outpoints corresponding to a matched pubkey script are added to the filter.
    * See the 'Comparing Transaction Elements to a Bloom Filter' section in this link
    * https://bitcoin.org/en/developer-reference#filterload
    */
  def flags: BloomFlag

  /** Inserts a hash into the [[BloomFilter]] */
  def insert(hash: DoubleSha256Digest): BloomFilter = {
    //these are the bit indexes that need to be set inside of data
    val bitIndexes = (0 until hashFuncs.toInt).map(i => murmurHash(i,hash))
    @tailrec
    def loop(remainingBitIndexes: Seq[Int], accum: Seq[Byte]): Seq[Byte] = {
      if (remainingBitIndexes.isEmpty) accum
      else {
        val currentIndex = remainingBitIndexes.head
        val byteIndex = currentIndex % 8
        val byte = accum(byteIndex)
        val setBitByte: Byte = (byte | (1 << byteIndex)).toByte
        //replace old byte with new byte with bit set
        val newAccum: Seq[Byte] = accum.updated(byteIndex,setBitByte)
        loop(remainingBitIndexes.tail,newAccum)
      }
    }
    val newData = loop(bitIndexes,data)
    BloomFilter(filterSize,newData,hashFuncs,tweak,flags)
  }

  /** Checks if [[data]] contains the given hash */
  def contains(hash: DoubleSha256Digest): Boolean = {
    val bitIndexes = (0 until hashFuncs.toInt).map(i => murmurHash(i,hash))
    @tailrec
    def loop(remainingBitIndexes: Seq[Int], accum: Seq[Boolean]): Boolean = {
      if (remainingBitIndexes.isEmpty) !accum.exists(_ == false)
      else {
        val currentIndex = remainingBitIndexes.head
        val byteIndex = currentIndex >>> 3
        val byte = data(byteIndex)
        val isBitSet = (byte & (1 << byteIndex)) != 0
        loop(remainingBitIndexes.tail, isBitSet +: accum)
      }
    }
    loop(bitIndexes,Seq())
  }


  /**
    * Performs the [[MurmurHash3]] on the given hash
    * @param hashNum the nth hash function we are using
    * @param hash the hash of the data that needs to be inserted into the [[BloomFilter]]
    * @return the index of the bit inside of [[data]] that needs to be set to 1
    */
  private def murmurHash(hashNum: Int, hash: DoubleSha256Digest): Int = {
    //TODO: The call of .toInt is probably the source of a bug here, need to come back and look at this
    //since this isn't consensus critical though I'm leaving this for now
    val seed = (hashNum * murmurConstant.underlying * tweak.underlying).toInt
    val murmurHash = MurmurHash3.bytesHash(hash.bytes.toArray, seed)
    val uint32 = UInt32(BitcoinSUtil.encodeHex(murmurHash))
    val modded = uint32.underlying % (filterSize.num.toInt * 8)
    //remove sign bit
    modded.toInt
  }

  /** See BIP37 to see where this number comes from https://github.com/bitcoin/bips/blob/master/bip-0037.mediawiki#bloom-filter-format */
  private def murmurConstant = UInt32("fba4c795")

  override def hex = RawBloomFilterSerializer.write(this)
}


object BloomFilter extends Factory[BloomFilter] {

  private case class BloomFilterImpl(filterSize: CompactSizeUInt, data: Seq[Byte], hashFuncs : UInt32,
                                     tweak: UInt32, flags: BloomFlag) extends BloomFilter
  /** Max bloom filter size as per https://bitcoin.org/en/developer-reference#filterload */
  val maxSize = UInt32(36000)

  /** Max hashFunc size as per https://bitcoin.org/en/developer-reference#filterload */
  val maxHashFuncs = UInt32(50)


  def apply(numElements: Int, falsePositiveRate: Double, tweak: UInt32, flags: BloomFlag): BloomFilter = {
    import scala.math._
    //m = number of bits in the array
    //n = number of elements in the array
    //from https://github.com/bitcoin/bips/blob/master/bip-0037.mediawiki#bloom-filter-format
    val optimalFilterSize : Double = (-1 / pow(log(2),2) * numElements * log(falsePositiveRate)) / 8
    logger.debug("optimalFilterSize " + optimalFilterSize)
    //BIP37 places limitations on the filter size, namely it cannot be > 36,000 bytes
    val actualFilterSize: Int = max(1,min(optimalFilterSize, maxSize.underlying * 8)).toInt
    logger.debug("actualFilterSize: " + actualFilterSize)
    val optimalHashFuncs: Double = (actualFilterSize * 8 / numElements * log(2))
    //BIP37 places a limit on the amount of hashFuncs we can use, which is 50
    val actualHashFuncs: Int = max(1,min(optimalHashFuncs, maxHashFuncs.underlying)).toInt

    val emptyByteArray = Seq.fill(actualFilterSize)(0.toByte)
    BloomFilter(CompactSizeUInt(UInt64(actualFilterSize)), emptyByteArray, UInt32(actualHashFuncs), tweak, flags)
  }

  def apply(filterSize: CompactSizeUInt, data: Seq[Byte], hashFuncs: UInt32, tweak: UInt32, flags: BloomFlag): BloomFilter = {
    BloomFilterImpl(filterSize, data, hashFuncs, tweak, flags)
  }


  override def fromBytes(bytes: Seq[Byte]): BloomFilter = RawBloomFilterSerializer.read(bytes)

// = UInt32("fba4c795")
}
