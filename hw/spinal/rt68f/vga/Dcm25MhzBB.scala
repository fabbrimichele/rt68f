package rt68f.vga

import spinal.core._

//noinspection TypeAnnotation
class Dcm25MhzBB extends BlackBox {
    val io = new Bundle {
        val clk  = in Bool()
        val reset  = in Bool()
        val clk25  = out Bool()
        val locked = out Bool()
    }

  mapClockDomain(clock = io.clk, reset = io.reset, resetActiveLevel = HIGH)

  setDefinitionName("Dcm25Mhz") // This tells SpinalHDL which Verilog module to instantiate
  addRTLPath("hw/vhdl/dcm25mhz.vhd") // Merge the file to the generated 'mergeRTL.v' file
  noIoPrefix() // Remove io_ prefix
}
