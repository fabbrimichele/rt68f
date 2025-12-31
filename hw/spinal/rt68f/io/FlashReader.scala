package rt68f.io

import rt68f.core.M68kBus
import spinal.core._
import spinal.lib._

import scala.language.postfixOps

case class FlashReader(/*spiMaster: SpiMasterWithCs*/) extends Component {
  val io = new Bundle {
    val bus = slave(M68kBus())
    val sel = in Bool() // chip select from decoder
    val spi = master(Spi())
  }

  val ctrlReg = Reg(Bits(8 bits)) init 0
  val dataReg = Reg(Bits(8 bits)) init 0
  val addrHiReg = Reg(Bits(8 bits)) init 0
  val addrLoReg = Reg(Bits(16 bits)) init 0

  val addr = io.bus.ADDR(2 downto 1) // Each register is 16 bit wide

  // Default bus signals
  io.bus.DATAI := 0     // default
  io.bus.DTACK := True  // inactive (assuming active low)

  when(!io.bus.AS && io.sel) {
    io.bus.DTACK := False // acknowledge access (active low)

    when(io.bus.RW) {
      // Read
      io.bus.DATAI := addr.mux(
        0 -> ctrlReg.resize(16),
        1 -> dataReg.resize(16),
        2 -> addrHiReg.resize(16),
        3 -> addrLoReg.resize(16),
      )
    } otherwise {
      // Write
      // TODO: manage UDS/LDS
      switch(addr) {
        is(0) { ctrlReg := io.bus.DATAO(7 downto 0) }
        is(1) { dataReg := io.bus.DATAO(7 downto 0) } // TODO: once debugged remove this
        is(2) { addrHiReg := io.bus.DATAO(7 downto 0) }
        is(3) { addrLoReg := io.bus.DATAO }
      }
    }
  }
}
