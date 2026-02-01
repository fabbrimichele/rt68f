package rt68f.ps2

import spinal.core._

import scala.language.postfixOps

class Ps2RxTxBB extends BlackBox {
  val io = new Bundle {
    val clk = in Bool()
    val reset = in Bool()
    val wr_ps2 = in Bool()
    val din = in Bits(8 bits)
    val dout = out Bits(8 bits)
    val rx_done_tick = out Bool()
    val tx_done_tick = out Bool()
    val ps2d = inout(Analog(Bool()))
    val ps2c = inout(Analog(Bool()))
  }

  mapClockDomain(clock = io.clk, reset = io.reset, resetActiveLevel = HIGH)

  setDefinitionName("ps2_rxtx") // This tells SpinalHDL which Verilog module to instantiate
  addRTLPath("hw/vhdl/ps2_rx.vhd") // Merge the file to the generated 'mergeRTL.v' file
  addRTLPath("hw/vhdl/ps2_tx.vhd") // Merge the file to the generated 'mergeRTL.v' file
  addRTLPath("hw/vhdl/ps2_rxtx.vhd") // Merge the file to the generated 'mergeRTL.v' file
  noIoPrefix() // Remove io_ prefix
}

