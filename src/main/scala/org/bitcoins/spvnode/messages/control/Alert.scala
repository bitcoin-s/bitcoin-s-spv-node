package org.bitcoins.spvnode.messages.control

import org.bitcoins.core.protocol.CompactSizeUInt

/**
  * Created by chris on 5/31/16.
  * Represents the alert object on the peer to peer network
  * https://bitcoin.org/en/developer-reference#alert
  */
trait Alert {

  /**
    * Alert format version. Version 1 from protocol version 311 through at least protocol version 70002.
    *
    * @return
    */
  def version : Int

  /**
    * The time beyond which nodes should stop relaying this alert. Unix epoch time format.
    *
    * @return
    */
  def relayUntil : Long

  /**
    * The time beyond which this alert is no longer in effect and should be ignored. Unix epoch time format.
    *
    * @return
    */
  def expiration : Long

  /**
    * A unique ID number for this alert.
    *
    * @return
    */
  def id : Long

  /**
    * All alerts with an ID number less than or equal to this number
    * should be canceled: deleted and not accepted in the future.
    *
    * @return
    */
  def cancel : Long


  /**
    * The number of IDs in the following setCancel field. May be zero.
    *
    * @return
    */
  def setCancelCount : CompactSizeUInt

  /**
    * Alert IDs which should be canceled. Each alert ID is a separate int32_t number.
    * @return
    */
  def setCancel : Int


  /**
    * This alert only applies to protocol versions greater than or equal to this version.
    * Nodes running other protocol versions should still relay it.
    * @return
    */
  def minVer : Int

  /**
    * This alert only applies to protocol versions less than or equal to this version.
    * Nodes running other protocol versions should still relay it.
    * @return
    */
  def maxVer : Int

  /**
    * If this field is empty, it has no effect on the alert.
    * If there is at least one entry is this field, this alert only applies to programs
    * with a user agent that exactly matches one of the strings in this field.
    * Each entry in this field is a compactSize uint followed by a string—the uint indicates
    * how many bytes are in the following string. This field was originally called setSubVer;
    * since BIP14, it applies to user agent strings as defined in the version message.
    * @return
    */
  def setUserAgent : CompactSizeUInt

  /**
    * Relative priority compared to other alerts.
    * @return
    */
  def priority : Int


  /**
    * The number of bytes in the following comment field. May be zero.
    * @return
    */
  def commentBytes : CompactSizeUInt

  /**
    * A comment on the alert that is not displayed.
    * @return
    */
  def comment : String

  /**
    * The number of bytes in the following statusBar field. May be zero.
    * @return
    */
  def statusBarBytes : CompactSizeUInt

  /**
    * The alert message that is displayed to the user.
    * @return
    */
  def statusBar : String

  /**
    * The number of bytes in the following reserved field. May be zero.
    * @return
    */
  def reservedBytes : CompactSizeUInt

  /**
    * Reserved for future use. Originally called RPC Error.
    * @return
    */
  def reserved : String
}
