package org.bitcoins.spvnode.store

import java.io.{BufferedWriter, File}
import java.net.{URI, URL}

import org.bitcoins.core.protocol.blockchain.BlockHeader

import scala.io.{BufferedSource, Source}

/**
  * Created by chris on 9/5/16.
  */
trait BlockHeaderStore {

  def append(headers: Seq[BlockHeader], file : java.io.File): Unit = {
    printToFile(file) { p =>
      headers.map(_.hex).foreach(p.println)
    }
  }


  def read(file: java.io.File) : Seq[BlockHeader] = (for {
    line <- Source.fromFile(file).getLines()
  } yield BlockHeader(line)).toSeq

  def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit) {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }
}

object BlockHeaderStore extends BlockHeaderStore
