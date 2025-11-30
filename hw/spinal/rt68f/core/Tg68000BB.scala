package rt68f.core

import spinal.core._

import scala.language.postfixOps

class Tg68000BB extends BlackBox {
  // Define IO
  val io = new Bundle {
    // Clock and reset
    val clk        = in Bool()
    val reset      = in Bool()
    val clkena_in  = in Bool()

    // CPU inputs
    val data_in    = in Bits(16 bits)
    val IPL        = in Bits(3 bits) // interrupt priority level
    val dtack      = in Bool()

    // CPU outputs
    val addr       = out Bits(32 bits)
    val data_out   = out Bits(16 bits)
    val as         = out Bool()
    val uds        = out Bool()
    val lds        = out Bool()
    val rw         = out Bool()
    val drive_data = out Bool()
  }

  // Map the clock domain
  // Mapped in the wrapper
  mapClockDomain(clock = io.clk, reset = io.reset, resetActiveLevel = LOW, enable = io.clkena_in)

  setDefinitionName("TG68") // This tells SpinalHDL which Verilog module to instantiate
  addRTLPath("hw/vhdl/TG68.vhd") // Merge the file to the generated 'mergeRTL.v' file
  addRTLPath("hw/vhdl/TG68_fast.vhd") // Merge the file to the generated 'mergeRTL.v' file
  noIoPrefix() // Remove io_ prefix
}
