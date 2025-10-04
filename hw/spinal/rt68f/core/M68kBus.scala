package rt68f.core

import spinal.core._
import spinal.lib.IMasterSlave

import scala.language.postfixOps

case class M68kBus(addrWidth: Int = 32, dataWidth: Int = 16) extends Bundle with IMasterSlave {
  val AS = Bool()
  val UDS = Bool()
  val LDS = Bool()
  val RW = Bool() // 1 = read, 0 = write
  val ADDR = UInt (addrWidth bits)
  val DATAI = Bits (dataWidth bits) // from CPU
  val DATAO = Bits(dataWidth bits) // to CPU
  val DTACK = Bool()

  override def asMaster(): Unit = {
    out(AS, UDS, LDS, RW, ADDR, DATAO)
    in(DATAI, DTACK)
  }
}
