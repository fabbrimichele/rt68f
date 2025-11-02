package rt68f.vga

import rt68f.core.M68kBus
import rt68f.vga.VgaDevice.rgbConfig
import spinal.core.{Area, Bits, Bool, Bundle, Cat, ClockDomain, ClockingArea, Component, False, IntToBuilder, Mem, Mux, Reg, RegInit, True, U, UInt, in, log2Up, when}
import spinal.lib.graphic.RgbConfig
import spinal.lib.graphic.vga.{Vga, VgaTimingsHV}
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

  val size = 32768 / 2  // 32KB = 640x400, 1 bit color
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
    val ctrl = VgaCtrl(rgbConfig)
    ctrl.io.vga <> io.vga

    ctrl.io.softReset := False
    ctrl.io.timings.setAs_h640_v480_r60

    // --- Access Exposed Counters and Timings ---
    val hCount = ctrl.io.hCounter
    val vCount = ctrl.io.vCounter
    val colorEn = ctrl.io.vga.colorEn
    val timings = ctrl.io.timings // Reuse the configured timing struct

    // --- VRAM Read Logic (Offset, Clamped, and Latency Compensated) ---
    val addressWidth = log2Up(size) // 13 bits (for 8192 words)
    val lineLength = U(640 / 16)    // 40 words per line

    // 1. Horizontal Offset: pixelX = hCount - hStart (The pixel currently being displayed)
    val hStartValue = timings.h.colorStart.resize(12 bits)
    val pixelX = Mux(
      hCount >= hStartValue,
      hCount - hStartValue,
      U(0, 12 bits)
    )

    // 2. LATENCY COMPENSATION: pixelX_command = pixelX - 1 (Command address for next cycle)
    // The command is based on the address one pixel *before* the current display position.
    val pixelX_command = Mux(
      hCount > hStartValue,
      hCount - hStartValue - 1,
      U(0, 12 bits)
    )

    // 3. Vertical Offset: pixelY_raw = vCount - vStart
    val vStartValue = timings.v.colorStart.resize(12 bits)
    val pixelY = Mux(
      vCount >= vStartValue,
      vCount - vStartValue,
      U(0, 12 bits)
    )

    // 5. Vertical Clamp: Ensure the address does not exceed VRAM height (200 lines).
    val vramHeight = U(400, pixelY.getWidth bits)
    val vramLastLine = U(399, pixelY.getWidth bits)

    val vramY = Mux(
      pixelY >= vramHeight,
      vramLastLine,
      pixelY
    )

    // 6. VRAM X Word Address: (pixelX_command) divided by 16
    val vramXWord = pixelX_command(pixelX_command.high downto 4)

    // 7. Linear Address = (Y_clamped * 40) + X_word
    val vramAddress = ((vramY * lineLength) + vramXWord).resize(addressWidth)

    // VRAM Read: mem.readSync handles the 1-cycle data delay.
    val wordData = mem.readSync(
      address = vramAddress,
      clockCrossing = true
    )

    // 8. Pixel Bit Index: lower 4 bits of the *displayed* pixel (pixelX).
    val pixelBitIndex = pixelX(3 downto 0) - 3 // - 3 is a hack
    val pixelDataBit = (wordData.asUInt >> pixelBitIndex).lsb

    ctrl.io.rgb.r := 0
    ctrl.io.rgb.g := 0
    ctrl.io.rgb.b := 0

    when(ctrl.io.vga.colorEn && pixelDataBit) {
      ctrl.io.rgb.r := 15
      ctrl.io.rgb.g := 15
      ctrl.io.rgb.b := 15
    }
  }
}
