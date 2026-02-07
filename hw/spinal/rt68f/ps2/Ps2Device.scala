package rt68f.ps2

import rt68f.core.M68kBus
import spinal.core._
import spinal.lib._

import scala.language.postfixOps

/**
 * # ctrlReg (8 bits - read/write)
 * Bit 0: unused
 * Bit 1: PS/2 int  : 0 -> off, 1 -> on
 * Bit 2: unused
 * Bit 3: unused
 * Bit 4: RX done   : read only
 * Bit 5: TX done   : read only
 * Bit 6: PS/2 ack  : Write to acknowledge Timer A interrupt
 * Bit 7: unused
*/
case class Ps2Device(Timeout: BigInt = 100) extends Component {
  val io = new Bundle {
    val bus = slave(M68kBus())
    val sel = in Bool() // chip select from decoder
    val int = out Bool() // UART interrupt
    val ps2 = master(Ps2())      // Ps2 data and clock
  }

  val ps2rx = new Ps2RxTxBB()

  ps2rx.io.ps2c := io.ps2.clk
  ps2rx.io.ps2d := io.ps2.dat

  // -- Memory Mapped Registers --

  val ctrlReg = Reg(Bits(8 bits)) init 0 // Write
  val datain  = Reg(Bits(8 bits)) init 0 // Write
  val dataout = ps2rx.io.dout  // It's already a register in ps2rxtx
  ps2rx.io.din := datain
  ctrlReg(4) := ps2rx.io.rx_done_tick
  ctrlReg(5) := ps2rx.io.tx_done_tick

  // -- Non-mapped Registers and Signals --
  val intEn      = ctrlReg(1)
  val intPending = Reg(Bool()) init False
  val wrPulse    = Reg(Bool()) init False
  val intAckBit  = 6

  io.int          := intPending
  ps2rx.io.wr_ps2 := wrPulse


  // -------------------
  // 68000 bus
  // -------------------
  io.bus.DATAI := 0 // default
  io.bus.DTACK := True // inactive (assuming active low)
  val addr = io.bus.ADDR(1 downto 1) // 2 registers, Each register is 16 bit wide

  when(!io.bus.AS && io.sel) {
    io.bus.DTACK := False // acknowledge access (active low)

    when(io.bus.RW) {
      // Read
      io.bus.DATAI := addr.mux(
        0 -> ctrlReg.resize(16),
        1 -> dataout.resize(16),
      )
    } otherwise {
      // Write
      // TODO: manage UDS/LDS
      switch(addr) {
        is(0) {
          // Skip -> Bit 6: PS/2 ack, Bit 5: TX done, Bit 4: RX done
          ctrlReg(3 downto 0) := io.bus.DATAO(3 downto 0)
          ctrlReg(7 downto 7) := io.bus.DATAO(7 downto 7)
        }
        is(1) {
          datain :=  io.bus.DATAO(7 downto 0)
        }
      }
    }
  }

  when(ps2rx.io.tx_done_tick) {
    // Write completed disable write pulse
    wrPulse := False
  } elsewhen(!io.bus.AS && io.sel && !io.bus.RW && addr === 1) {
    // Write data, enable write pulse
    wrPulse := True
  }

  // Interrupt
  when ((ps2rx.io.rx_done_tick || ps2rx.io.tx_done_tick) && intEn) {
    intPending := True
  } elsewhen (!io.bus.AS && io.sel && !io.bus.RW) {
    when(!io.bus.LDS && io.bus.DATAO(intAckBit)) {
      intPending := False
    }
  }
}
