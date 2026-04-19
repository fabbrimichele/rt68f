package rt68f.vga

import rt68f.core._
import rt68f.vga.VgaBwDevice.Modes.M0_640X400C002
import rt68f.vga.VgaBwDevice.rgbConfig
import spinal.core._
import spinal.lib.graphic.RgbConfig
import spinal.lib.graphic.vga.Vga
import spinal.lib.{BufferCC, master, slave}

import scala.language.postfixOps

object VgaBwDevice {
  val rgbConfig = RgbConfig(4, 4, 4)

  object Modes {
    val M0_640X400C002 = 0
    val M1_640X480C002 = 1
  }
}

//noinspection TypeAnnotation
case class VgaBwDevice(clk25: ClockDomain) extends Component {
  val io = new Bundle {
    val bus             = slave(M68kBus())
    val framebufferSel  = in Bool()   // Framebuffer select from decoder
    val paletteSel      = in Bool()   // Palette select from decoder (not used by this version)
    val controlSel      = in Bool()   // Control select from decoder
    val vBlankInt       = out Bool()  // Vertical blank interrupt
    val vga             = master(Vga(rgbConfig, withColorEn = false))
  }

  // Framebuffer
  val fbWidth = 49152 / 2  // max 648x400, 1 bit color
  val framebuffer = Mem(Bits(16 bits), fbWidth)

  // Control register
  // Bits 1-0 [Screen mode] : 0 -> 640x400 2 colors, 1 -> 640x480 2 colors
  // Bit 2    [Overscan]    : 0 -> off, 1 -> on
  // Bit 3    [VBlank int]  : 0 -> off, 1 -> on
  // Bit 6    [VBlank ack]  : Write to acknowledge VBlank interrupt
  val controlReg = Reg(Bits(16 bits)) init 3 // 640X400C002, no overscan, no vBlank int

  // ------------ Interrupts ------------
  // vSync
  val vBlankIntEn = controlReg(3)
  val vBlackAckBit = 6

  val vSyncReg = BufferCC(io.vga.vSync)

  val vSyncPending = RegInit(False)
  // VSync is negative (for current resolutions)
  // TODO: consider polarity (for current resolutions)
  when(vSyncReg.fall() && vBlankIntEn) {
    vSyncPending := True
  } elsewhen (!io.bus.AS && io.controlSel && !io.bus.RW) {
    when(!io.bus.LDS && io.bus.DATAO(vBlackAckBit)) {
      vSyncPending := False
    }
  }

  io.vBlankInt := vSyncPending


  // ------------ 68000 BUS side ------------
  // Default response
  io.bus.DATAI := 0
  io.bus.DTACK := True

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

  // Simulate Palette access
  when(!io.bus.AS && io.paletteSel) {
    io.bus.DTACK := False // acknowledge access (active low)
    when(io.bus.RW) {
      // Read
      io.bus.DATAI := 0
    }
  }

  // Frame buffer read/write
  when(!io.bus.AS && io.framebufferSel) {
    io.bus.DTACK := False // active
    val wordAddr = io.bus.ADDR(log2Up(fbWidth) downto 1)

    when(io.bus.RW) {
      // ------------------------------------
      // Read Access (Byte strobes are ignored by the memory block)
      // ------------------------------------
      // NOTE: mem.readSync is fine; the M68k core internally selects D15-D8 or D7-D0 based on UDS/LDS.
      io.bus.DATAI := framebuffer.readSync(wordAddr)
    } otherwise {
      // ------------------------------------
      // Write Access (Byte strobes MUST be managed)
      // ------------------------------------
      // io.bus.UDS (D15-D8) -> mask(1)
      // io.bus.LDS (D7-D0) -> mask(0)
      val byteMask = Cat(!io.bus.UDS, !io.bus.LDS).asBits // The 2-bit byte write enable mask

      // Use writeMixedWidth to enable byte-level writing
      framebuffer.writeMixedWidth(
        address = wordAddr,
        data = io.bus.DATAO,
        mask = byteMask
      )
    }
  }

  // ------------ VGA side ------------
  new ClockingArea(clk25) {
    val controlRegCC = BufferCC(controlReg)
    val mode = controlRegCC(1 downto 0).asUInt
    val overscanEn = controlRegCC(2)


    // Configuration
    val fbLatency = 1
    val lineWidth =  640
    val numberOfLinesReg = Reg(UInt()) init 400

    val ctrl = VgaCtrl(rgbConfig)
    ctrl.io.softReset := False

    when(mode === M0_640X400C002) {
      // VGA Signal 640 x 400 @ 70 Hz
      numberOfLinesReg := 400
      ctrl.io.timings.setAs(
        hPixels = 640,
        hSync = 96,
        hFront = 16,
        hBack = 48,
        hPolarity = false,
        vPixels = 400,
        vSync = 2,
        vFront = 12,
        vBack = 35,
        vPolarity = false
      )
    } otherwise {
      numberOfLinesReg := 480
      ctrl.io.timings.setAs_h640_v480_r60
    }

    // --- Access Exposed Counters and Timings ---
    val hCount = ctrl.io.hCounter
    val vCount = ctrl.io.vCounter
    val frameStart = ctrl.io.frameStart
    val timings = ctrl.io.timings

    // -- Framebuffer read ---
    // Depending on the number of bits per pixel, the
    // address will ignore the least significant bits.
    // In other words the extra bits are used to divide
    // the address, i.e.:
    // 1-bit color -> pixelCounter(pixelCounter.high downto 4)
    // 2-bit color -> pixelCounter(pixelCounter.high downto 3)
    // 4-bit color -> pixelCounter(pixelCounter.high downto 1)
    val stretch = Reg(Bool()) init True
    val pixelCounter = Reg(UInt(log2Up(fbWidth) + 4 bits)) init 0
    val pixelStartLineCounter = Reg(UInt(log2Up(fbWidth) + 4 bits)) init 0
    val lineCounter = Reg(UInt(9 bits)) init 0
    val isVisibleVertRange = vCount >= timings.v.colorStart && vCount < (timings.v.colorStart + numberOfLinesReg)
    val isVisibleHorRange = hCount >= (timings.h.colorStart - fbLatency) && hCount < timings.h.colorEnd - fbLatency

    when (frameStart) {
      lineCounter := 0
      pixelCounter := 0
      pixelStartLineCounter := 0
    } elsewhen(isVisibleVertRange && isVisibleHorRange) {
      pixelCounter := pixelCounter + 1
    } elsewhen(isVisibleVertRange && (hCount === 0)) {
      pixelCounter := pixelStartLineCounter
      lineCounter := lineCounter + 1
      pixelStartLineCounter := pixelStartLineCounter + lineWidth
    }

    val fbReadAddr = pixelCounter(pixelCounter.high downto 4).resize(log2Up(fbWidth))
    val wordData = framebuffer.readSync(
      address = fbReadAddr,
      clockCrossing = true
    )

    // -- Shift register and output ---
    val shiftRegister = Reg(Bits(16 bits)) init 0

    val pixelX = Mux(
      hCount >= timings.h.colorStart,
      hCount - timings.h.colorStart,
      U(0, 12 bits)
    )

    val pixelBitIndex = pixelX(3 downto 0) // Load the shift register every 16 pixels (no stretch)

    when (pixelBitIndex === 0) {
      shiftRegister := wordData
    } otherwise  {
        shiftRegister := shiftRegister |<< 1
    }

    val colEn = ctrl.io.colorEn && isVisibleVertRange
    io.vga.hSync := ctrl.io.vga.hSync
    io.vga.vSync := ctrl.io.vga.vSync
    io.vga.color <> ctrl.io.vga.color

    when(colEn) {
      val pixelColor = B(4 bits, default -> !shiftRegister(15)).asUInt
      ctrl.io.rgb.r := pixelColor
      ctrl.io.rgb.g := pixelColor
      ctrl.io.rgb.b := pixelColor
    } otherwise {
      ctrl.io.rgb.clear()
    }
  }
}
