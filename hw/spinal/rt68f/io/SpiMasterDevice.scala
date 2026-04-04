package rt68f.io

import rt68f.core.M68kBus
import spinal.core._
import spinal.lib._

import scala.language.postfixOps

case class SpiMasterConfig(portCount: Int)

case class SpiMasterDevice(config: SpiMasterConfig) extends Component {
  val io = new Bundle {
    val bus = slave(M68kBus())
    val sel = in Bool() // chip select from decoder
    val spis = Vec(master(Spi()), config.portCount)
  }

  val spiMaster = new SpiMasterBB
  val spiSel = !io.bus.AS && io.sel

  // 68000 bus
  spiMaster.io.addr := io.bus.ADDR(2 downto 1).asBits
  spiMaster.io.cs := spiSel
  spiMaster.io.data_in := io.bus.DATAO(7 downto 0)
  spiMaster.io.rw := io.bus.RW
  io.bus.DATAI := spiMaster.io.data_out.resized
  // spiMaster.io.irq not used

  // SPI bus
  spiMaster.io.spi_miso := spiMaster.io.spi_cs_n.muxList(
    defaultValue = True,
    // spi_cs_n is asserted low, need false
    // Patterns = 11111110, 11111101, 11111011, etc
    mappings = for(i <- 0 until config.portCount) yield {
      val pattern = B(8 bits, default -> true) // pattern = 11111111
      pattern(i) := False                      // e.g. i = 1 -> pattern = 11111101
      pattern -> io.spis(i).miso
    },
  )

  // SPIs outputs
  for (i <- 0 until config.portCount) {
    io.spis(i).mosi := spiMaster.io.spi_mosi
    io.spis(i).clk := spiMaster.io.spi_clk
    io.spis(i).cs := spiMaster.io.spi_cs_n(i)
  }

  io.bus.DTACK := True  // inactive (assuming active low)
  when(spiSel) {
    io.bus.DTACK := False // acknowledge access (active low)
  }
}
