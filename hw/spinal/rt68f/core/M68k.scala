package rt68f.core

import spinal.core._
import spinal.lib._

import scala.language.postfixOps

case class M68k() extends Component {
  val io = master(M68kBus())
  val tg68000 = new Tg68000BB

  // clock and reset managed automatically
  tg68000.io.clkena_in := True
  tg68000.io.IPL := 0b111 // TODO: move interrupts into the CpuBus?

  // Bus <-> Core mapping
  io.ADDR := tg68000.io.addr.asUInt
  io.DATAO := tg68000.io.data_out
  io.RW := tg68000.io.rw
  io.AS := tg68000.io.as
  io.UDS := tg68000.io.uds
  io.LDS := tg68000.io.lds

  // master bus read from bus (driven by slaves)
  tg68000.io.data_in := io.DATAI
  tg68000.io.dtack := io.DTACK
}
