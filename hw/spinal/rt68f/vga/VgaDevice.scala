package rt68f.vga

import rt68f.core.M68kBus
import rt68f.vga.VgaDevice.rgbConfig
import spinal.core.{Bits, Bundle, Cat, ClockDomain, ClockingArea, Component, False, IntToBuilder, Mem, True, U, in, log2Up, when}
import spinal.lib.graphic.RgbConfig
import spinal.lib.graphic.vga.{Vga, VgaCtrl}
import spinal.lib.{master, slave}

import scala.language.postfixOps

object VgaDevice {
  val rgbConfig = RgbConfig(4, 4, 4)
}

case class VgaDevice() extends Component {
  val io = new Bundle {
    val bus   = slave(M68kBus())
    val sel   = in Bool() // chip select from decoder
    val vga = master(Vga(VgaDevice.rgbConfig))
  }

  val size = 16384 / 2  // 16KB = 640x200, 1 bit color
  val mem = Mem(Bits(16 bits), size)

  // ------------ 68000 BUS side ------------
  // Default response
  io.bus.DATAI := 0
  io.bus.DTACK := True

  when(!io.bus.AS && io.sel) {
    io.bus.DTACK := False // active
    val wordAddr = io.bus.ADDR(log2Up(size) downto 1)

    when(io.bus.RW) {
      // ------------------------------------
      // Read Access (Byte strobes are ignored by the memory block)
      // ------------------------------------
      // NOTE: mem.readSync is fine; the M68k core internally selects D15-D8 or D7-D0 based on UDS/LDS.
      io.bus.DATAI := mem.readSync(wordAddr)
    } otherwise {
      // ------------------------------------
      // Write Access (Byte strobes MUST be managed)
      // ------------------------------------
      // io.bus.UDS (D15-D8) -> mask(1)
      // io.bus.LDS (D7-D0) -> mask(0)
      val byteMask = Cat(!io.bus.UDS, !io.bus.LDS).asBits // The 2-bit byte write enable mask

      // Use writeMixedWidth to enable byte-level writing
      mem.writeMixedWidth(
        address = wordAddr,
        data = io.bus.DATAO,
        mask = byteMask
      )
    }
  }

  // ------------ VGA side ------------
  val dcm = new Dcm25MhzBB()

  val pixelClock = ClockDomain(
    clock = dcm.io.clk25,
    reset = ~dcm.io.locked
  )

  // Clock domain area for VGA timing logic
  new ClockingArea(pixelClock) {
    val ctrl = new VgaCtrl(rgbConfig)
    ctrl.io.vga <> io.vga

    ctrl.io.softReset := False
    ctrl.io.timings.setAs_h640_v480_r60
    ctrl.io.pixels.valid := True

    val addressWidth = log2Up(size)
    ctrl.io.pixels.r := 0
    ctrl.io.pixels.g := 0
    ctrl.io.pixels.b := 0

    val buffer = mem.readSync(U(0, addressWidth bits), clockCrossing = true)

    when(ctrl.io.vga.colorEn) {
      ctrl.io.pixels.r := buffer(11 downto 8).asUInt
      ctrl.io.pixels.g := buffer(7 downto 4).asUInt
      ctrl.io.pixels.b := buffer(3 downto 0).asUInt
    }
  }
}
