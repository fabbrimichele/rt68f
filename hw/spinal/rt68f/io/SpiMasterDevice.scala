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
    7  : RX Data valid (TODO: this doesn't work)

    Control Register (write):
    0  : SPI CS - 0 active/1 inactive
 */
case class SpiMasterDevice(config: SpiMasterConfig = SpiMasterConfig()) extends Component {
  val io = new Bundle {
    val bus = slave(M68kBus())
    val sel = in Bool() // chip select from decoder
    val spi = master(Spi()) // TODO: either extract CS from Spi or extend Spi.cs to 8 bits
  }

  val spiMaster = new SpiMasterMM
  val spiSel = !io.bus.AS && io.sel

  // 68000 bus
  spiMaster.io.addr := io.bus.ADDR(2 downto 1).asBits
  spiMaster.io.cs := spiSel
  spiMaster.io.data_in := io.bus.DATAO(7 downto 0)
  spiMaster.io.rw := io.bus.RW
  io.bus.DATAI := spiMaster.io.data_out.resized
  // TODO: spiMaster.io.irq

  // SPI bus
  spiMaster.io.spi_miso := io.spi.miso
  io.spi.mosi := spiMaster.io.spi_mosi
  io.spi.clk := spiMaster.io.spi_clk
  io.spi.cs := spiMaster.io.spi_cs_n(0)

  io.bus.DTACK := True  // inactive (assuming active low)
  when(spiSel) {
    io.bus.DTACK := False // acknowledge access (active low)
  }
}
