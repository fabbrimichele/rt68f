package rt68f.io

import spinal.core._

import scala.language.postfixOps

case class SpiMasterConfig(
  spiMode: Int = 0,
  clksPerHalfBit: Int = 2,
)

class SpiMaster(config: SpiMasterConfig) extends BlackBox {
  // Add Generics to the BlackBox
  addGeneric("SPI_MODE", config.spiMode)
  addGeneric("CLKS_PER_HALF_BIT", config.clksPerHalfBit)

  val io = new Bundle {
    // Control/Data Signals,
    val i_Rst_L = in Bool() // FPGA Reset
    val i_Clk   = in Bool() // FPGA Clock

    // TX (MOSI) Signals
    val i_TX_Byte  = in Bits(8 bits) // Byte to transmit on MOSI
    val i_TX_DV    = in Bool()       // Data Valid Pulse with i_TX_Byte
    val o_TX_Ready = out Bool()      // Transmit Ready for next byte

    // RX (MISO) Signals
    val o_RX_DV   = out Bool()       // Data Valid pulse (1 clock cycle)
    val o_RX_Byte = out Bits(8 bits) // Byte received on MISO

    // SPI Interface
    val o_SPI_Clk  = out Bool()
    val i_SPI_MISO = in Bool()
    val o_SPI_MOSI = out Bool()
  }
  mapClockDomain(clock = io.i_Clk, reset = io.i_Rst_L, resetActiveLevel = LOW)

  setDefinitionName("SPI_Master") // This tells SpinalHDL which Verilog module to instantiate
  addRTLPath("hw/vhdl/SPI_Master.vhd") // Merge the file to the generated 'mergeRTL.v' file

  noIoPrefix() // Remove io_ prefix
}
