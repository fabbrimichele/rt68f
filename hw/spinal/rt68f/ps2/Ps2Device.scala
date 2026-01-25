package rt68f.ps2

import rt68f.core.M68kBus
import spinal.core._
import spinal.lib._

import scala.language.postfixOps

// See: https://github.com/wel97459/MySpinalHardware/blob/master/PS2.scala
case class Ps2Device(Timeout: BigInt = 100) extends Component {
  val io = new Bundle {
    val bus = slave(M68kBus())
    val sel = in Bool() // chip select from decoder
    val int = out Bool() // UART interrupt
    val ps2 = master(Ps2())      // Ps2 data and clock
  }

  val ps2rx = new Ps2RxBB()

  ps2rx.io.ps2c := io.ps2.clk
  ps2rx.io.ps2d := io.ps2.dat

  // -- Memory Mapped Registers --
  val data = ps2rx.io.dout  // It's already a register in ps2rx
  val ctrlReg = ps2rx.io.rx_done_tick ## B"0000001"

  // -------------------
  // 68000 bus
  // -------------------
  io.bus.DATAI := 0 // default
  io.bus.DTACK := True // inactive (assuming active low)

  when(!io.bus.AS && io.sel) {
    io.bus.DTACK := False // acknowledge access (active low)
    val addr = io.bus.ADDR(1 downto 1) // 2 registers, Each register is 16 bit wide

    when(io.bus.RW) {
      // Read
      io.bus.DATAI := addr.mux(
        0 -> ctrlReg.resize(16),
        1 -> data.resize(16),
      )
    } otherwise {
      // Write
      // TODO: manage UDS/LDS
      switch(addr) {
        //is(0) { ctrlReg := io.bus.DATAO(7 downto 0) }
      }
    }
  }
}
