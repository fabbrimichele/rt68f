package rt68f.io

import rt68f.core.M68kBus
import spinal.core._
import spinal.lib._

import scala.language.postfixOps

/*
    Memory mapped SPI device:
    $00: Status/Control Register (read/write)
    $01: RX/TS Data Register (read/write)

    Status Register (read):
    4-0: Unused
    5  : Card Detect
    6  : TX Ready
    7  : RX Data valid

    Control Register (write):
    0  : SPI CS - 0 active/1 inactive
    // 1  : TX Data Valid - should be a pulse: TODO: remove this line
 */
case class SpiDevice(config: SpiMasterConfig = SpiMasterConfig()) extends Component {
  val io = new Bundle {
    val bus = slave(M68kBus())
    val sel = in Bool() // chip select from decoder
    val spi = master(Spi())
    val cd = in Bool() // card detect
  }

  val spiMaster = new SpiMaster(config)

  // ---------------
  // Registers
  // ---------------
  // TODO: Interrupts

  // TODO: besides the configuration this component is generic enough
  //       for any SPI device, including the Flash ROM, I could rename
  //       it and use it for an SdCardReader AND create a new Flash ROM reader.

  // ctrlReg (write)
  private val ctrlReg = Reg(Bits(8 bits)) init B"00000001" // CS is inactive high
  private val spiCsN = ctrlReg(0)
  //private val txDataValid = ctrlReg(1)

  // statReg (read)
  private val statReg = spiMaster.io.o_RX_DV ## spiMaster.io.o_TX_Ready ## io.cd ## B"00000"

  // TX/RX register
  spiMaster.io.i_TX_Byte := B"8'x00"
  private val txData = spiMaster.io.i_TX_Byte
  private val rxData = spiMaster.io.o_RX_Byte

  // ---------------
  // SPI Mapping
  // ---------------
  io.spi.cs := spiCsN
  io.spi.clk := spiMaster.io.o_SPI_Clk
  io.spi.mosi := spiMaster.io.o_SPI_MOSI
  spiMaster.io.i_SPI_MISO := io.spi.miso
  spiMaster.io.i_TX_DV := False

  // -------------------
  // 68000 bus
  // -------------------
  io.bus.DATAI := 0 // default
  io.bus.DTACK := True // inactive (assuming active low)
  private val addr = io.bus.ADDR(1 downto 1) // 2 registers, Each register is 16 bit wide

  when(!io.bus.AS && io.sel) {
    io.bus.DTACK := False // acknowledge access (active low)

    when(io.bus.RW) {
      // Read
      io.bus.DATAI := addr.mux(
        0 -> statReg.resize(16),
        1 -> rxData.resize(16),
      )
    } otherwise {
      // Write
      // TODO: manage UDS/LDS
      switch(addr) {
        is(0) {
          ctrlReg := io.bus.DATAO(7 downto 0)
        }
        is(1) {
          txData := io.bus.DATAO(7 downto 0)
          spiMaster.io.i_TX_DV := True // TODO: is this pulse long enough?
        }
      }
    }
  }
}
