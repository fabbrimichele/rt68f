package rt68f.io

import rt68f.core.M68kBus
import spinal.core._
import spinal.lib._
import spinal.lib.fsm._

import scala.language.postfixOps

case class FlashReader() extends Component {
  val io = new Bundle {
    val bus = slave(M68kBus())
    val sel = in Bool() // chip select from decoder
    val spi = master(Spi())
    val led = out Bits(4 bits) // For debugging
  }

  // SPI
  val spiConfig = SpiMasterConfig(
    maxBytesPerCs = 8,
  )
  val spiMaster = new SpiMasterWithCs(spiConfig)
  io.spi.cs := spiMaster.io.o_SPI_CS_n
  io.spi.clk := spiMaster.io.o_SPI_Clk
  io.spi.mosi := spiMaster.io.o_SPI_MOSI
  spiMaster.io.i_SPI_MISO := io.spi.miso

  // Registers
  // bit 0: START (Self-clearing or Handshaking)
  val ctrlReg = Reg(Bits(8 bits)) init 0
  // bit 7: BUSY/VALID
  val statReg = Reg(Bits(8 bits)) init 0
  val dataReg = Reg(Bits(8 bits)) init 0
  val addrReg = Reg(Bits(24 bits)) init 0

  io.led := dataReg(3 downto 0)

  // Alias for logic
  val startCommand = ctrlReg(0)

  // -------------------
  // 68000 bus
  // -------------------
  val addr = io.bus.ADDR(2 downto 1) // Each register is 16 bit wide
  io.bus.DATAI := 0 // default
  io.bus.DTACK := True // inactive (assuming active low)

  when(!io.bus.AS && io.sel) {
    io.bus.DTACK := False // acknowledge access (active low)

    when(io.bus.RW) {
      // Read
      io.bus.DATAI := addr.mux(
        0 -> statReg.resize(16),
        1 -> dataReg.resize(16),
        2 -> addrReg(23 downto 16).resize(16), // High
        3 -> addrReg(15 downto 0), // Low
      )
    } otherwise {
      // Write
      // TODO: manage UDS/LDS
      switch(addr) {
        is(0) { ctrlReg := io.bus.DATAO(7 downto 0) }
        is(1) { dataReg := io.bus.DATAO(7 downto 0) }
        is(2) { addrReg(23 downto 16) := io.bus.DATAO(7 downto 0) }
        is(3) { addrReg(15 downto 0) := io.bus.DATAO }
      }
    }
  }

  // -------------------
  // SPI Master Bus
  // -------------------
  val fsm = new StateMachine {
    val idle = new State with EntryPoint
    val sendCmd, waitCmd = new State
    val sendAddr1, waitAddr1 = new State
    val sendAddr2, waitAddr2 = new State
    val sendAddr3, waitAddr3 = new State
    val readByte, waitByte = new State

    // Default for SPI Master
    spiMaster.io.i_TX_DV := False
    spiMaster.io.i_TX_Byte := B"8'x00"
    spiMaster.io.i_TX_Count := 5 // 1 cmd + 3 addr + 1 data = 5 bytes total

    idle.whenIsActive {
      // Update Status: Not busy
      statReg(7) := False

      when(startCommand) {
        // Clear the start bit immediately so we don't loop
        ctrlReg(0) := False
        goto(sendCmd)
      }
    }

    sendCmd.whenIsActive {
      statReg(7) := True // Mark as BUSY
      spiMaster.io.i_TX_Byte := B"8'x03"
      spiMaster.io.i_TX_DV := True
      goto(waitCmd)
    }
    waitCmd.whenIsActive {
      spiMaster.io.i_TX_DV := False // This is not strictly necessary since it is set to False in default section
      when(spiMaster.io.o_TX_Ready) { goto(sendAddr1) }
    }

    sendAddr1.whenIsActive {
      spiMaster.io.i_TX_Byte := addrReg(23 downto 16).asBits
      spiMaster.io.i_TX_DV := True
      goto(waitAddr1)
    }
    waitAddr1.whenIsActive {
      spiMaster.io.i_TX_DV := False
      when(spiMaster.io.o_TX_Ready) { goto(sendAddr2) }
    }

    sendAddr2.whenIsActive {
      spiMaster.io.i_TX_Byte := addrReg(15 downto 8).asBits
      spiMaster.io.i_TX_DV := True
      goto(waitAddr2)
    }
    waitAddr2.whenIsActive {
      spiMaster.io.i_TX_DV := False
      when(spiMaster.io.o_TX_Ready) { goto(sendAddr3) }
    }

    sendAddr3.whenIsActive {
      spiMaster.io.i_TX_Byte := addrReg(7 downto 0).asBits
      spiMaster.io.i_TX_DV := True
      goto(waitAddr3)
    }
    waitAddr3.whenIsActive {
      spiMaster.io.i_TX_DV := False
      when(spiMaster.io.o_TX_Ready) { goto(readByte) }
    }

    readByte.whenIsActive {
      spiMaster.io.i_TX_Byte := 0
      spiMaster.io.i_TX_DV := True
      goto(waitByte)
    }
    waitByte.whenIsActive {
      spiMaster.io.i_TX_Byte := 0
      spiMaster.io.i_TX_DV := False

      when(spiMaster.io.o_RX_DV) {
        dataReg := spiMaster.io.o_RX_Byte
        // Transition back to idle; statReg(7) will clear there
        goto(idle)
      }
    }
  }
}