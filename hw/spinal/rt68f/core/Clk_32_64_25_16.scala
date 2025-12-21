package rt68f.core

import spinal.core.{BlackBox, Bundle, HIGH, in, out}

class Clk_32_64_25_16 extends BlackBox {
  val io = new Bundle {
    val CLK_IN32  = in Bool()   // 32.000 MHz
    val CLK_OUT64  = out Bool() // 64.000 MHz
    val CLK_OUT25 = out Bool()  // 25.143 MHz
    val CLK_OUT16 = out Bool()  // 16.000 MHz
    val RESET  = in Bool()
    val LOCKED = out Bool()
  }

  mapClockDomain(clock = io.CLK_IN32, reset = io.RESET, resetActiveLevel = HIGH)

  setDefinitionName("clk_32_64_25_16") // This tells SpinalHDL which Verilog module to instantiate
  addRTLPath("hw/vhdl/clk_32_64_25_16.vhd") // Merge the file to the generated 'mergeRTL.v' file
  noIoPrefix() // Remove io_ prefix
}