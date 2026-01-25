package rt68f.ps2

import spinal.core.{Bundle, in, out}
import spinal.core.in.Bool
import spinal.lib.IMasterSlave


case class Ps2() extends Bundle with IMasterSlave {
  val dat = Bool()
  val clk = Bool()

  override def asMaster(): Unit = {
    in(dat, clk)
  }
}
