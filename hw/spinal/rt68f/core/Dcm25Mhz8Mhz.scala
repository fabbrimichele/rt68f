package rt68f.core

import spinal.core.{BlackBox, Bundle, HIGH, in, out}

//noinspection TypeAnnotation
class Dcm25Mhz8Mhz extends BlackBox {
  val io = new Bundle {
    val CLK_IN1  = in Bool()
    val CLK_OUT1  = out Bool() // 25.143 MHz
    val CLK_OUT2 = out Bool() // 8.000 MHz
    val RESET  = in Bool()
    val LOCKED = out Bool()
  }

  mapClockDomain(clock = io.CLK_IN1, reset = io.RESET, resetActiveLevel = HIGH)

  setDefinitionName("dcm32_25_8") // This tells SpinalHDL which Verilog module to instantiate
  addRTLPath("hw/vhdl/dcm32_25_8.vhd") // Merge the file to the generated 'mergeRTL.v' file
  noIoPrefix() // Remove io_ prefix
}
