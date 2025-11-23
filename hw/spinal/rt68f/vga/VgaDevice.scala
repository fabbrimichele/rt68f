package rt68f.vga

import rt68f.core.M68kBus
import rt68f.vga.VgaDevice.Modes.{M0_640X400C02, M1_640X200C04, M2_320X200C16, M3_320X200C16}
import rt68f.vga.VgaDevice.rgbConfig
import spinal.core._
import spinal.lib.graphic.{Rgb, RgbConfig}
import spinal.lib.graphic.vga.Vga
import spinal.lib.{BufferCC, master, slave}

import scala.language.postfixOps

object VgaDevice {
  val rgbConfig = RgbConfig(4, 4, 4)

  object Modes {
    val M0_640X400C02 = 0 // Mode 0: 640x400, 2 colors (1 bit per pixel)
    val M1_640X200C04 = 1 // Mode 1: 640x200, 4 colors (2 bit per pixel)
    val M2_320X200C16 = 2 // Mode 2: 320x200, 16 colors (4 bit per pixel)
    val M3_320X200C16 = 3 // Mode 3: Same a Mode 2 TODO: see if there is a mode that makes sense
  }
}


//noinspection TypeAnnotation
case class VgaDevice() extends Component {
  val io = new Bundle {
    val bus             = slave(M68kBus())
    val framebufferSel  = in Bool() // Framebuffer select from decoder
    val paletteSel      = in Bool() // Palette select from decoder
    val controlSel      = in Bool() // Control select from decoder // TODO: use it...
    val vga             = master(Vga(VgaDevice.rgbConfig))
  }

  // TODO: add a CTRL register to switch between:
  //  - Monitor resolution: 640x480 (no black bands) and 640x400

  // Framebuffer
  val size = 32768 / 2  // 32KB = 640x400, 1 bit color
  val mem = Mem(Bits(16 bits), size)

  // Palette (implemented with registers)
  // TODO: use 12 bits
  // TODO: Find TOS default palette
  val palette = Vec.fill(16)(Reg(UInt(16 bits)))
  palette(0).init(U(0x0000))  // Black (background)
  palette(1).init(U(0x0FFF))  // White (foreground)
  palette(2).init(U(0x0F00))  // Red
  palette(3).init(U(0x00F0))  // Green
  palette(4).init(U(0x0F00))  // Blue
  palette(5).init(U(0x00AA))  // Cyan
  palette(6).init(U(0x0A0A))  // Magenta
  palette(7).init(U(0x0A50))  // Brown
  palette(8).init(U(0x0AAA))  // Light Gray
  palette(9).init(U(0x0555))  // Dark Grey
  palette(10).init(U(0x0F55))  // Light Red
  palette(11).init(U(0x05F5))  // Light Green
  palette(12).init(U(0x055F))  // Light Blue
  palette(13).init(U(0x05FF))  // Light Cyan
  palette(14).init(U(0x0F5F))  // Light Magenta
  palette(15).init(U(0x0FF5))  // Yellow

  // Control register
  val controlReg = Reg(Bits(16 bits)) init 1 // Default 640x200, 4 colors

  // ------------ 68000 BUS side ------------
  // Default response
  io.bus.DATAI := 0
  io.bus.DTACK := True

  // Palette read/write
  when(!io.bus.AS && io.paletteSel) {
    io.bus.DTACK := False // acknowledge access (active low)
    val wordAddr = io.bus.ADDR(4 downto 1)

    when(io.bus.RW) {
      // Read
      io.bus.DATAI := palette(wordAddr).asBits
    } otherwise {
      // Write
      // TODO: handle UDS/LDS
      palette(wordAddr) := io.bus.DATAO.asUInt
    }
  }

  // Control read/write
  when(!io.bus.AS && io.controlSel) {
    io.bus.DTACK := False // acknowledge access (active low)

    when(io.bus.RW) {
      // Read
      io.bus.DATAI := controlReg
    } otherwise {
      // Write
      // TODO: handle UDS/LDS
      controlReg := io.bus.DATAO
    }
  }

  // Frame buffer read/write
  when(!io.bus.AS && io.framebufferSel) {
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
    // --- Synchronize mode parameters from 32MHz to 25MHz ---
    // --- CDC: Synchronize the entire 16-bit control register ---
    val syncedControlReg = BufferCC(controlReg)
    // TODO: sync palette as well?
    //  this might cause a bug, when changing to lower resolution (MODE2) the background is dark blue

    val modeSelect = syncedControlReg(1 downto 0).asUInt

    val ctrl = VgaCtrl(rgbConfig)
    ctrl.io.vga <> io.vga

    ctrl.io.softReset := False
    ctrl.io.timings.setAs_h640_v480_r60

    // --- Access Exposed Counters and Timings ---
    val hCount = ctrl.io.hCounter
    val vCount = ctrl.io.vCounter
    val timings = ctrl.io.timings // Reuse the configured timing struct

    // 1. Horizontal Offset: pixelX = hCount - hStart
    val hStartValue = timings.h.colorStart.resize(12 bits)
    val pixelX = Mux(
      hCount >= hStartValue,
      hCount - hStartValue,
      U(0, 12 bits)
    )

    // 3. Vertical Offset: pixelY = vCount - vStart
    val vStartValue = timings.v.colorStart.resize(12 bits)
    val pixelY = Mux(
      vCount >= vStartValue,
      vCount - vStartValue,
      U(0, 12 bits)
    )

    // 5. Vertical Clamp: Ensure the address does not exceed VRAM height (200 lines).
    val vramLastLine = U(399, pixelY.getWidth bits)
    val pastVramLines = pixelY > vramLastLine

    // scaledPixelY is only used to determine RAM address
    val scaledPixelY = modeSelect.mux(
      M0_640X400C02 -> pixelY,
      M1_640X200C04 -> pixelY(11 downto 1).resized,
      M2_320X200C16 -> pixelY(11 downto 1).resized,
      M3_320X200C16 -> pixelY(11 downto 1).resized,
    )

    val vramY = Mux(
      pastVramLines,
      vramLastLine,
      scaledPixelY
    )

    // 6. VRAM X Word Address: (pixelX) divided by 16
    //    RAM needs to be read one pixel earlier to
    //    compensate for the read requiring one clock.
    val vramXWord = modeSelect.mux(
      M0_640X400C02 -> (pixelX + 1)(pixelX.high downto 4).resize(10), // 16 pixels per word
      M1_640X200C04 -> (pixelX + 1)(pixelX.high downto 3).resize(10), // 8 pixels per word
      M2_320X200C16 -> (pixelX + 1)(pixelX.high downto 2),            // 4 pixels per word
      M3_320X200C16 -> (pixelX + 1)(pixelX.high downto 2),            // 4 pixels per word
    )

    // 7. Linear Address = (Y_clamped * 40) + X_word
    val lineLength = modeSelect.mux(
      M0_640X400C02 -> U(640 / 16),
      M1_640X200C04 -> U(640 / 8),
      M2_320X200C16 -> U(640 / 4),
      M3_320X200C16 -> U(640 / 4),
    )
    val addressWidth = log2Up(size) // 13 bits (for 8192 words)
    val vramAddress = ((vramY * lineLength) + vramXWord).resize(addressWidth)

    // VRAM Read: mem.readSync handles the 1-cycle data delay.
    val wordData = mem.readSync(
      address = vramAddress,
      clockCrossing = true
    )

    val shiftRegister = Reg(Bits(16 bits)) init 0

    val pixelBitIndex = modeSelect.mux(
      M0_640X400C02 -> pixelX(3 downto 0),
      M1_640X200C04 -> pixelX(2 downto 0).resized,
      M2_320X200C16 -> pixelX(1 downto 0).resized,
      M3_320X200C16 -> pixelX(1 downto 0).resized,
    )

    val bitsPerPixel = modeSelect.mux(
      M0_640X400C02 -> U(1, 3 bits),
      M1_640X200C04 -> U(2, 3 bits),
      M2_320X200C16 -> U(4, 3 bits),
      M3_320X200C16 -> U(4, 3 bits),
    )

    when (pixelBitIndex === 0) {
      shiftRegister := wordData
    } otherwise {
      shiftRegister := shiftRegister |<< bitsPerPixel
    }

    // todo: It's still not working properly
    //       the first half of the screen work properly,
    //       including going to a new line after 320
    //       but at the same time it writes in the second half
    //       I need to repeat each pixel twice
    val pixelColorIndex = modeSelect.mux(
      M0_640X400C02 -> shiftRegister.msb.asUInt.resize(4),
      M1_640X200C04 -> shiftRegister(15 downto 14).asUInt.resize(4),
      M2_320X200C16 -> shiftRegister(15 downto 12).asUInt,
      M3_320X200C16 -> shiftRegister(15 downto 12).asUInt,
    )
    val pixelColor = palette(pixelColorIndex)

    ctrl.io.rgb.clear()
    when(ctrl.io.vga.colorEn && !pastVramLines) {
      ctrl.io.rgb.r := pixelColor(11 downto 8)
      ctrl.io.rgb.g := pixelColor(7 downto 4)
      ctrl.io.rgb.b := pixelColor(3 downto 0)
    }
  }
}
