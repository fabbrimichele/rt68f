package rt68f.io

import spinal.core.{BlackBox, IntToBuilder, LOW, in, log2Up, out}
import spinal.lib.experimental.chisel.Bundle

import scala.language.postfixOps

// Define the Generics for the VHDL component
case class SpiMasterConfig(
  spiMode: Int = 0,
  clksPerHalfBit: Int = 2,
  maxBytesPerCs: Int = 2,
  csInactiveClks: Int = 1
)

class SpiMasterWithCs(config: SpiMasterConfig) extends BlackBox {
  // Calculate the bit width required for the counters based on maxBytesPerCs
  // We use log2Up to find the number of bits needed to represent the max value
  val countWidth = log2Up(config.maxBytesPerCs + 1) bits

  // Add Generics to the BlackBox
  addGeneric("SPI_MODE", config.spiMode)
  addGeneric("CLKS_PER_HALF_BIT", config.clksPerHalfBit)
  addGeneric("MAX_BYTES_PER_CS", config.maxBytesPerCs)
  addGeneric("CS_INACTIVE_CLKS", config.csInactiveClks)

  // Define IO
  val io = new Bundle {
    // Control/Data Signals,
    val i_Rst_L = in Bool() // FPGA Reset
    val i_Clk   = in Bool() // FPGA Clock

    // TX (MOSI) Signals
    val i_TX_Count = in UInt(countWidth) // # bytes per CS low
    val i_TX_Byte  = in Bits(8 bits)     // Byte to transmit on MOSI
    val i_TX_DV    = in Bool()           // Data Valid Pulse with i_TX_Byte
    val o_TX_Ready = out Bool()          // Transmit Ready for next byte

    // RX (MISO) Signals
    val o_RX_Count = out UInt(countWidth) // Index RX byte
    val o_RX_DV    = out Bool()           // Data Valid pulse (1 clock cycle)
    val o_RX_Byte  = out Bits(8 bits);    // Byte received on MISO

    // SPI Interface
    val o_SPI_Clk  = out Bool()
    val i_SPI_MISO = in Bool()
    val o_SPI_MOSI = out Bool()
    val o_SPI_CS_n = out Bool()
  }

  mapClockDomain(clock = io.i_Clk, reset = io.i_Rst_L, resetActiveLevel = LOW)

  // Ensure the name matches the VHDL Entity
  setDefinitionName("SPI_Master_With_Single_CS")

  // Add RTL paths, they'll be copied into the 'mergeRTL.v' file
  addRTLPath("hw/vhdl/SPI_Master.vhd")
  addRTLPath("hw/vhdl/SPI_Master_With_Single_CS.vhd")

  noIoPrefix() // Remove io_ prefix
}
