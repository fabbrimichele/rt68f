package rt68f.sound

import spinal.core._

import scala.language.postfixOps

class JtOpl2BB extends BlackBox {
  val io = new Bundle {
    val rst = in Bool()
    val clk = in Bool()
    val cen = in Bool()
    val din = in Bits(8 bits)
    val addr = in Bool()
    val cs_n = in Bool()
    val wr_n = in Bool()
    val dout = out Bits(8 bits)
    val irq_n = out Bool()

    // Sound output
    val snd = out SInt(16 bits)
    val sample = out Bool()
  }

  mapClockDomain(clock = io.clk, reset = io.rst, resetActiveLevel = HIGH)

  // SpinalHDL can't generate both `mergeRTL.v` and `mergeRTL.vhd`
  // at the same time, so vhdl will still be merged but each verilog
  // file has to be specified in the file: `hw/xilinx/Rt68f.prj`.
  setDefinitionName("jtopl2") // This tells SpinalHDL which Verilog module to instantiate
  noIoPrefix() // Remove io_ prefix
}
