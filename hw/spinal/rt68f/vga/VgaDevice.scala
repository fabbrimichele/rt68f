package rt68f.vga

import rt68f.core.M68kBus
import rt68f.vga.VgaDevice.rgbConfig
import spinal.core._
import spinal.lib.graphic.{Rgb, RgbConfig}
import spinal.lib.graphic.vga.Vga
import spinal.lib.{master, slave}

import scala.language.postfixOps

object VgaDevice {
  val rgbConfig = RgbConfig(4, 4, 4)
}

case class VgaDevice() extends Component {
  val io = new Bundle {
    val bus     = slave(M68kBus())
    val sel     = in Bool() // memory select from decoder
    val regSel  = in Bool() // registry select from decoder
    val vga     = master(Vga(VgaDevice.rgbConfig))
  }

  // Frame buffer
  val size = 32768 / 2  // 32KB = 640x400, 1 bit color
  val mem = Mem(Bits(16 bits), size)

  // Registers
  // ctrlReg(0): color 0 (background color)
  // ctrlReg(1): color 1 (foreground color)
  // ctrlReg(2): color 2
  // ctrlReg(3): color 3
  val regWidth = 16 bits
  val ctrlReg = Vec.fill(4)(Reg(UInt(regWidth)))
  ctrlReg(0).init(U(0x0000))  // Initialize background color to black
  ctrlReg(1).init(U(0x0FFF))  // Initialize foreground color to white
  ctrlReg(2).init(U(0x0F00))  // Initialize color 2 to red
  ctrlReg(3).init(U(0x00F0))  // Initialize color 3 to green

  // ------------ 68000 BUS side ------------
  // Default response
  io.bus.DATAI := 0
  io.bus.DTACK := True

  // Registers read/write
  when(!io.bus.AS && io.regSel) {
    io.bus.DTACK := False // acknowledge access (active low)
    val wordAddr = io.bus.ADDR(2 downto 1)

    when(io.bus.RW) {
      // Read
      io.bus.DATAI := ctrlReg(wordAddr).asBits
    } otherwise {
      // Write
      // TODO: handle UDS/LDS
      ctrlReg(wordAddr) := io.bus.DATAO.asUInt
    }
  }

  // Frame buffer read/write
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
    //val vramLastLine = U(399, pixelY.getWidth bits)
    val vramLastLine = U(199, pixelY.getWidth bits)
    val pastVramLines = pixelY > vramLastLine

    val vramY = Mux(
      pastVramLines,
      vramLastLine,
      pixelY
    )

    // 6. VRAM X Word Address: (pixelX) divided by 16
    //    RAM needs to be read one pixel earlier to
    //    compensate for the read requiring one clock.
    //val vramXWord = (pixelX + 1)(pixelX.high downto 4) // 640x400x2 = pixel/16 -> 1 bits per pixel, 1 word = 16 pixels
    val vramXWord = (pixelX + 1)(pixelX.high downto 2) // 640x200x4 = pixelX/8 -> 2 bits per pixel, 1 word = 8 pixels

    // 7. Linear Address = (Y_clamped * 40) + X_word
    //val lineLength = U(640 / 16)    // 640x400, 2 colors = 16 pixels per word = 40 words per line
    val lineLength = U(640 / 8)    // 640x200, 4 colors = 8 pixels per word = 80 words per line
    val addressWidth = log2Up(size) // 13 bits (for 8192 words)
    val vramAddress = ((vramY * lineLength) + vramXWord).resize(addressWidth)

    // VRAM Read: mem.readSync handles the 1-cycle data delay.
    val wordData = mem.readSync(
      address = vramAddress,
      clockCrossing = true
    )

    val shiftRegister = Reg(Bits(16 bits)) init(0)

    // val pixelBitIndex = pixelX(3 downto 0) // 640x400, 2 colors
    val pixelBitIndex = pixelX(1 downto 0) // 640x200, 4 colors
    when (pixelBitIndex === 0) {
      shiftRegister := wordData
    } otherwise {
      shiftRegister := shiftRegister |<< 2
    }

    val pixelDataBit = shiftRegister(15 downto 14)

    ctrl.io.rgb.clear()

    when(ctrl.io.vga.colorEn && !pastVramLines) {
      val selectedColor = ctrlReg(pixelDataBit.asUInt)
      ctrl.io.rgb.r := selectedColor(11 downto 8)
      ctrl.io.rgb.g := selectedColor(7 downto 4)
      ctrl.io.rgb.b := selectedColor(3 downto 0)
    }
  }
}
