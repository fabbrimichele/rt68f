package rt68f.io

import rt68f.core.M68kBus
import spinal.core._
import spinal.lib._

import scala.language.postfixOps

case class SpiMasterDevice(config: SpiMasterConfig = SpiMasterConfig()) extends Component {
  val io = new Bundle {
    val bus = slave(M68kBus())
    val sel = in Bool() // chip select from decoder
    val spi0 = master(Spi()) // TODO: make it generic?
    val spi1 = master(Spi())
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
  spiMaster.io.spi_miso := spiMaster.io.spi_cs_n.mux(
    B"11111110" -> io.spi0.miso, // Device 0 Active
    B"11111101" -> io.spi1.miso, // Device 1 Active
    default -> True,
  )

  io.spi0.mosi := spiMaster.io.spi_mosi
  io.spi0.clk := spiMaster.io.spi_clk
  io.spi0.cs := spiMaster.io.spi_cs_n(0)

  io.spi1.mosi := spiMaster.io.spi_mosi
  io.spi1.clk := spiMaster.io.spi_clk
  io.spi1.cs := spiMaster.io.spi_cs_n(1)

  io.bus.DTACK := True  // inactive (assuming active low)
  when(spiSel) {
    io.bus.DTACK := False // acknowledge access (active low)
  }
}
