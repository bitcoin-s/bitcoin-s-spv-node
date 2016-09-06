package org.bitcoins.spvnode.store

import java.io.FileOutputStream

import org.bitcoins.core.protocol.blockchain.BlockHeader
import org.bitcoins.spvnode.constant.Constants

import scala.io.Source

/**
  * Created by chris on 9/5/16.
  */
trait BlockHeaderStore {

  /** Appends block headers to the given file */
  def append(headers: Seq[BlockHeader], file : java.io.File): Unit = {
    printToFile(file) { p =>
      headers.map(_.hex).foreach(p.println)
    }
  }
  /** Appends block headers to the default blockheader file */
  def append(headers: Seq[BlockHeader]): Unit = append(headers, Constants.blockHeaderFile)

  /** Reads block headers from the given file */
  def read(file: java.io.File): Seq[BlockHeader] = (for {
    line <- Source.fromFile(file).getLines()
  } yield BlockHeader(line)).toSeq

  /** Reads block headers from the default blockheader file */
  def read: Seq[BlockHeader] = read(Constants.blockHeaderFile)

  private def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
    val p = new java.io.PrintWriter(new FileOutputStream(f,true))
    try { op(p) } finally { p.close() }
  }
}

object BlockHeaderStore extends BlockHeaderStore
