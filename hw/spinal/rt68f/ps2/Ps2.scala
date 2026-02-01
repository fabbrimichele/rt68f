package rt68f.ps2

import spinal.core._
import spinal.lib.IMasterSlave


case class Ps2() extends Bundle with IMasterSlave {
  val dat = Analog(Bool())
  val clk = Analog(Bool())

  override def asMaster(): Unit = {
    inout(dat, clk)
  }
}
