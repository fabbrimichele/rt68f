package rt68f.sound

import rt68f.core.M68kBus
import spinal.core._
import spinal.lib._

import scala.language.postfixOps

/*
  OPL2/YM3812 Device
 */
case class Opl2Device() extends Component {
  val io = new Bundle {
    val bus = slave(M68kBus())
    val sel = in Bool() // chip select from decoder
    val int = out Bool() // UART interrupt
    // TODO: add sound output
  }

  val opl2 = new JtOpl2BB()

  // -------------------
  // 68000 bus
  // -------------------
  val clockEnable = new Area {
    // 16 Mhz -> ~3.58 MHz, 114 / 256 = 0.4453125
    private val acc = Reg(UInt(8 bits)) init 0
    acc := acc + 114
    val enable = acc.msb
  }

  val devSel = !io.bus.AS && io.sel

  opl2.io.cs_n := !devSel
  opl2.io.wr_n := io.bus.RW
  opl2.io.addr := io.bus.ADDR(1)
  opl2.io.din := io.bus.DATAO(7 downto 0)
  opl2.io.cen := clockEnable.enable
  io.int := !opl2.io.irq_n
  io.bus.DATAI := opl2.io.dout.resized

  io.bus.DTACK := True // inactive (assuming active low)
  when(devSel) {
    io.bus.DTACK := False // acknowledge access (active low)
  }

  // -------------------
  // Sound
  // -------------------
  // TODO
}
