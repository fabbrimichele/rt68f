package rt68f.ps2

import spinal.core._

import scala.language.postfixOps

class Ps2RxBB extends BlackBox {
  val io = new Bundle {
    val clk = in Bool()
    val reset = in Bool()
    val ps2d = in Bool()
    val ps2c = in Bool()
    val rx_en = in Bool()
    val rx_done_tick = out Bool()
    val dout = out Bits(8 bits)
  }

  mapClockDomain(clock = io.clk, reset = io.reset, resetActiveLevel = HIGH, enable = io.rx_en)

  setDefinitionName("ps2_rx") // This tells SpinalHDL which Verilog module to instantiate
  addRTLPath("hw/vhdl/ps2_rx.vhd") // Merge the file to the generated 'mergeRTL.v' file
  noIoPrefix() // Remove io_ prefix
}
