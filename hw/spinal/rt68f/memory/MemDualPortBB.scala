package rt68f.memory

import spinal.core.{BlackBox, Bundle, IntToBuilder, LOW, in, out}

import scala.language.postfixOps

class MemDualPortBB extends BlackBox  {
  // Define IO
  val io = new Bundle {
    // Port A
    val clka = in Bool()
    val wea = in Bits(1 bits)
    val addra = in Bits(14 bits)
    val dina = in Bits(16 bits)

    // Port B
    val clkb = in Bool()
    val addrb = in Bits(14 bits)
    val doutb = out Bits(16 bits)
  }

  // Map the clock domain
  // Mapped in the wrapper
  mapClockDomain(clock = io.clka)

  setDefinitionName("mem_dual_port") // This tells SpinalHDL which Verilog module to instantiate
  addRTLPath("hw/vhdl/mem_dual_port.vhd") // Merge the file to the generated 'mergeRTL.v' file
  noIoPrefix() // Remove io_ prefix
}
