package rt68f.core

import spinal.core._
import spinal.lib._

import scala.language.postfixOps

case class M68k() extends Component {
  val io      = master(M68kBus())
  val ipl     = in Bits(3 bits) // interrupt priority level
  //val berr    = in Bool()

  val tg68k = new Tg68kBB

  tg68k.io.HALT := True // active low
  tg68k.io.BERR := False // active high
  tg68k.io.IPL := ipl
  io.ADDR := (B"00000000" ## tg68k.io.ADDR(23 downto 0)).asUInt // Reducing the address bus size saves LUTs
  io.DATAO := tg68k.io.DATAO
  tg68k.io.DATAI := io.DATAI
  io.AS := tg68k.io.AS
  io.UDS := tg68k.io.UDS
  io.LDS := tg68k.io.LDS
  io.RW := tg68k.io.RW
  tg68k.io.DTACK := io.DTACK
  tg68k.io.VPA := True // active low

  /*
  val tg68000 = new Tg68000BB

  // clock and reset managed automatically
  // tg68000.io.clkena_in := True // Managed by mapClockDomain in Tg68000BB
  tg68000.io.IPL := ipl

  // Bus <-> Core mapping
  io.ADDR := (B"00000000" ## tg68000.io.addr(23 downto 0)).asUInt // Reducing the address bus size saves LUTs
  io.DATAO := tg68000.io.data_out
  io.RW := tg68000.io.rw
  io.AS := tg68000.io.as
  io.UDS := tg68000.io.uds
  io.LDS := tg68000.io.lds

  // master bus read from bus (driven by slaves)
  tg68000.io.data_in := io.DATAI
  tg68000.io.dtack := io.DTACK
   */
}
