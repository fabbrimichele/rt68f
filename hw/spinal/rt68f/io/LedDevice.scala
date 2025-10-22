package rt68f.io

import rt68f.core.M68kBus
import spinal.core._
import spinal.lib.slave

import scala.language.postfixOps

case class LedDevice(width: Int = 4) extends Component {
  val io = new Bundle {
    val bus = slave(M68kBus())
    val sel = in Bool() // chip select from decoder
    val leds = out Bits (width bits)
  }

  val ledReg = Reg(Bits(16 bits)) init 0
  io.leds := ledReg(3 downto 0)

  // Default bus signals
  io.bus.DATAI := 0     // default
  io.bus.DTACK := True  // inactive (assuming active low)

  when(!io.bus.AS && io.sel) {
    io.bus.DTACK := False // acknowledge access (active low)

    when(io.bus.RW) {
      // Read
      io.bus.DATAI := ledReg
    } otherwise {
      // Write
      ledReg := io.bus.DATAO
    }
  }
}
