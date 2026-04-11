package rt68f.vga

import rt68f.core._
import rt68f.vga.VgaDevice.rgbConfig
import spinal.core._
import spinal.lib.graphic.vga.Vga
import spinal.lib.{BufferCC, Delay, master, slave}

import scala.language.postfixOps

//noinspection TypeAnnotation
case class VgaPlanarDevice(clk25: ClockDomain) extends Component {
  val io = new Bundle {
    val bus             = slave(M68kBus())
    val framebufferSel  = in Bool()   // Framebuffer select from decoder
    val paletteSel      = in Bool()   // Palette select from decoder
    val controlSel      = in Bool()   // Control select from decoder
    val vBlankInt       = out Bool()  // Vertical blank interrupt
    val vga             = master(Vga(VgaDevice.rgbConfig, withColorEn = false))
  }

  // Framebuffer
  val fbWidth = 32000 / 2
  val framebuffer = Mem(Bits(16 bits), fbWidth)

  // Palette
  val palette = Mem(Bits(12 bits), InitialPalette.colors)

  // Control register
  // Bits 1-0 [Screen mode] : 0 -> 640x400 2 colors, 1 -> 640x200 4 colors, 2 -> 320x200 16 colors, 3 -> 640x400 2 colors
  // Bit 2    [Overscan]    : 0 -> off, 1 -> on
  // Bit 3    [VBlank int]  : 0 -> off, 1 -> on
  // Bit 6    [VBlank ack]  : Write to acknowledge VBlank interrupt
  val controlReg = Reg(Bits(16 bits)) init 3 // 640X400C002, no overscan, no vBlank int
  // TODO: it seems 640X400C002 is not working, it skips one byte
  //       write a program to test it

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

  // Palette read/write
  when(!io.bus.AS && io.paletteSel) {
    io.bus.DTACK := False // acknowledge access (active low)
    val wordAddr = io.bus.ADDR(4 downto 1)

    when(io.bus.RW) {
      // Read
      io.bus.DATAI := Cat(B(0, 4 bits), palette.readSync(wordAddr))
    } otherwise {
      // ------------------------------------
      // Write Access (Byte strobes MUST be managed)
      // ------------------------------------
      // writeMixedWidth() works only with bytes and can't be used
      val upperMask = B(4 bits, default -> !io.bus.UDS)
      val lowerMask = B(8 bits, default -> !io.bus.LDS)

      // Use writeMixedWidth to enable byte-level writing
      palette.write(
        address = wordAddr,
        data = io.bus.DATAO(11 downto 0),
        mask = upperMask ## lowerMask
      )
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
    val lineWidth = U(640, 10 bits)

    // Configuration
    val fbLatency = 1
    val numberOfLines = 400
    val vertOffset = 40

    val vertOffsetReg = Reg(UInt()) init 0

    val ctrl = VgaCtrl(rgbConfig)
    ctrl.io.softReset := False

    when(overscanEn) {
      // VGA Signal 640 x 400 @ 70 Hz
      vertOffsetReg := 0
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
      vertOffsetReg := vertOffset
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

    val fbReadAddr = pixelCounter(pixelCounter.high downto 4).resize(log2Up(fbWidth) - 1)

    val isVisibleVertRange = vCount >= (timings.v.colorStart + vertOffsetReg) && vCount < (timings.v.colorStart + numberOfLines + vertOffsetReg)
    val isVisibleHorRange = hCount >= (timings.h.colorStart - fbLatency) && hCount < timings.h.colorEnd - fbLatency

    when (frameStart) {
      lineCounter := 0
      pixelCounter := 0
      pixelStartLineCounter := 0
    } elsewhen(isVisibleVertRange && isVisibleHorRange) {
      // Stretch horizontal resolution for modes M2
      stretch := !stretch
      pixelCounter := pixelCounter + 1
    } elsewhen(isVisibleVertRange && (hCount === 0)) {
      pixelCounter := pixelStartLineCounter
      lineCounter := lineCounter + 1
      stretch := True
      // Stretch vertical resolution by 2 for all screen modes but M0_640X400Cxx
      pixelStartLineCounter := pixelStartLineCounter + lineWidth
    }

    // TODO:
    //  These reads are sequential, but it doesn't seem to affect the result.
    //  Another option is to split the framebuffer into banks but it makes the
    //  68000 bus side a bit more complex and adding planes requires additional
    //  splits.

    val wordDataHigh = framebuffer.readSync(
      address = fbReadAddr << 1,
      clockCrossing = true
    )

    val wordDataLow = framebuffer.readSync(
      address = (fbReadAddr << 1) | 1,
      clockCrossing = true
    )

    // -- Shift register and output ---
    val shiftRegisterHigh = Reg(Bits(16 bits)) init 0
    val shiftRegisterLow = Reg(Bits(16 bits)) init 0

    val pixelX = Mux(
      hCount >= timings.h.colorStart,
      hCount - timings.h.colorStart,
      U(0, 12 bits)
    )
    val loadShiftRegister = pixelX(2 downto 0) === 0

    when (loadShiftRegister) {
      shiftRegisterHigh := wordDataHigh
      shiftRegisterLow := wordDataLow
    } otherwise  {
      shiftRegisterHigh := shiftRegisterHigh |<< 1
      shiftRegisterLow := shiftRegisterLow |<< 1
    }

    val pixelColorIndex = shiftRegisterHigh(15) ## shiftRegisterLow(15)

    val pixelColor = palette.readSync(
      address = pixelColorIndex.resize(4).asUInt,
      clockCrossing = true
    )

    // --- LATENCY MANAGEMENT ---
    // The pixelColor is ready 1 cycle AFTER colEn and Sync signals from VgaCtrl.
    // We must delay the VgaCtrl output signals by 1 cycle to match.
    val paletteLatency = 1

    val colEn = Delay(ctrl.io.colorEn && isVisibleVertRange, paletteLatency)
    io.vga.hSync := Delay(ctrl.io.vga.hSync, paletteLatency)
    io.vga.vSync := Delay(ctrl.io.vga.vSync, paletteLatency)
    io.vga.color <> ctrl.io.vga.color

    when(colEn) {
      ctrl.io.rgb.r := pixelColor(11 downto 8).asUInt
      ctrl.io.rgb.g := pixelColor(7 downto 4).asUInt
      ctrl.io.rgb.b := pixelColor(3 downto 0).asUInt
    } otherwise {
      ctrl.io.rgb.clear()
    }
  }
}
