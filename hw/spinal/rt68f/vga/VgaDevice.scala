package rt68f.vga

import rt68f.core.{M68kBus, ResetCtrl}
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
    // Mode 0: 640x400, 2 colors (1 bit per pixel)
    val M0_640X400C02 = 0
    val M1_640X200C04 = 1
    val M2_320X200C16 = 2
    val M3_320X200C16 = 3 // TODO: find a useful res, e.g. 160x200x256 color
  }
}


//noinspection TypeAnnotation
case class VgaDevice() extends Component {
  val io = new Bundle {
    val bus             = slave(M68kBus())
    val framebufferSel  = in Bool() // Framebuffer select from decoder
    val paletteSel      = in Bool() // Palette select from decoder
    val controlSel      = in Bool() // Control select from decoder
    val pixelClock      = in Bool() // Pixel clock must be 25.175 Mhz
    val pixelReset      = in Bool() // Raw reset (it must not be sync with the 16Mhz clock)
    val vga             = master(Vga(VgaDevice.rgbConfig))
  }

  // TODO: add a CTRL register to switch between:
  //  - Monitor resolution: 640x480 (no black bands) and 640x400

  // Framebuffer
  val fbWidth = 32768 / 2  // 32KB = 640x400, 1 bit color
  val framebuffer = Mem(Bits(16 bits), fbWidth)

  // Palette (implemented with registers)
  // TODO: use 12 bits
  val palette = Vec.fill(16)(Reg(UInt(16 bits)))
  palette(0).init(U(0x0000))  // Black
  palette(1).init(U(0x000A))  // Blue
  palette(2).init(U(0x00A0))  // Green
  palette(3).init(U(0x00AA))  // Cyan
  palette(4).init(U(0x0A00))  // Red
  palette(5).init(U(0x0A0A))  // Magenta
  palette(6).init(U(0x0A50))  // Brown (Special Case: R=A, G=5, B=0)
  palette(7).init(U(0x0AAA))  // Light Gray
  palette(8).init(U(0x0555))  // Dark Gray
  palette(9).init(U(0x055F))  // Bright Blue
  palette(10).init(U(0x05F5)) // Bright Green
  palette(11).init(U(0x05FF)) // Bright Cyan
  palette(12).init(U(0x0F55)) // Bright Red
  palette(13).init(U(0x0F5F)) // Bright Magenta
  palette(14).init(U(0x0FF5)) // Bright Yellow
  palette(15).init(U(0x0FFF)) // Bright White (Pure White)

  // Control register
  val controlReg = Reg(Bits(16 bits)) init 2 // Default 320x200, 16 colors

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
      when (!io.bus.LDS) {
        palette(wordAddr) := Cat(palette(wordAddr)(15 downto 8), io.bus.DATAO(7 downto 0)).asUInt
      } elsewhen(!io.bus.UDS) {
        palette(wordAddr) := Cat(io.bus.DATAO(15 downto 8), palette(wordAddr)(7 downto 0)).asUInt
      } elsewhen(!io.bus.LDS && !io.bus.UDS) {
        palette(wordAddr) := io.bus.DATAO.asUInt
      }
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
  val clk25 = ClockDomain(
    clock = io.pixelClock,
    reset = io.pixelReset,
    frequency = FixedFrequency(25.143 MHz),
  )

  new ClockingArea(clk25) {
    val controlRegCC = BufferCC(controlReg)
    val paletteCC =  BufferCC(palette)
    val mode = controlRegCC(1 downto 0).asUInt

    val bitsPerPixel = mode.mux(
      M0_640X400C02 -> U(1, 3 bits),
      M1_640X200C04 -> U(2, 3 bits),
      M2_320X200C16 -> U(4, 3 bits),
      M3_320X200C16 -> U(4, 3 bits),
    )
    val bitsPerPixelReg = RegNext(bitsPerPixel)

    val lineWidth =  mode.mux(
      M0_640X400C02 -> U(640, 10 bits),
      M1_640X200C04 -> U(640, 10 bits),
      M2_320X200C16 -> U(320, 10 bits),
      M3_320X200C16 -> U(320, 10 bits),
    )
    val lineWidthReg = RegNext(lineWidth)

    // Configuration
    val latency = 1
    val lastLine = 400

    val ctrl = VgaCtrl(rgbConfig)
    ctrl.io.vga <> io.vga

    ctrl.io.softReset := False
    ctrl.io.timings.setAs_h640_v480_r60

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
    // 1-bit color -> fbReadAddress(fbReadAddress.high downto 4)
    // 2-bit color -> fbReadAddress(fbReadAddress.high downto 3)
    // 4-bit color -> fbReadAddress(fbReadAddress.high downto 1)
    val stretch = Reg(Bool()) init True
    val pixelCounter = Reg(UInt(log2Up(fbWidth) + 4 bits)) init 0
    val pixelStartLineCounter = Reg(UInt(log2Up(fbWidth) + 4 bits)) init 0
    val lineCounter = Reg(UInt(9 bits)) init 0

    val fbReadAddr = mode.mux(
      M0_640X400C02 -> pixelCounter(pixelCounter.high downto 4).resize(log2Up(fbWidth)),
      M1_640X200C04 -> pixelCounter(pixelCounter.high downto 3).resize(log2Up(fbWidth)),
      M2_320X200C16 -> pixelCounter(pixelCounter.high downto 2).resize(log2Up(fbWidth)),
      M3_320X200C16 -> pixelCounter(pixelCounter.high downto 2).resize(log2Up(fbWidth)),
    )

    when (frameStart) {
      lineCounter := 0
      pixelCounter := 0
      pixelStartLineCounter := 0
    } elsewhen(
      (vCount >= timings.v.colorStart && vCount < timings.v.colorEnd)
      && (hCount >= (timings.h.colorStart - latency) && hCount < timings.h.colorEnd - latency)
    ) {
      // Stretch horizontal resolution for modes M2 and M3
      stretch := !stretch
      when (mode === M0_640X400C02 || mode === M1_640X200C04 || stretch === False) {
        pixelCounter := pixelCounter + 1
      }
    } elsewhen(
      (vCount >= timings.v.colorStart && vCount < timings.v.colorEnd)
        && (hCount === 0)
    ) {
      pixelCounter := pixelStartLineCounter
      lineCounter := lineCounter + 1
      stretch := True
      // Stretch vertical resolution by 2 for all screen modes but M0_640X400C02
      when(mode === M0_640X400C02 || lineCounter.lsb === True) {
        // Adding of 320 (or lineWidthReg) instead of 640 increase time from 80 ns to 103 ns
        pixelStartLineCounter := pixelStartLineCounter + lineWidthReg
      }
    }

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

    val colEn = ctrl.io.vga.colorEn && ((vCount - timings.v.colorStart) < lastLine)

    val pixelBitIndex = mode.mux(
      M0_640X400C02 -> pixelX(3 downto 0),
      M1_640X200C04 -> pixelX(2 downto 0).resized,
      M2_320X200C16 -> pixelX(2 downto 1).resized,
      M3_320X200C16 -> pixelX(2 downto 1).resized,
    )

    when (pixelBitIndex === 0) {
      shiftRegister := wordData
    } otherwise  {
      when (mode === M0_640X400C02 || mode === M1_640X200C04 || stretch === False) {
        shiftRegister := shiftRegister |<< bitsPerPixelReg
      }
    }

    val pixelColorIndex = mode.mux(
      M0_640X400C02 -> shiftRegister(15 downto 15).asUInt.resized,
      M1_640X200C04 -> shiftRegister(15 downto 14).asUInt.resized,
      M2_320X200C16 -> shiftRegister(15 downto 12).asUInt.resized,
      M3_320X200C16 -> shiftRegister(15 downto 12).asUInt.resized,
    )
    val pixelColor = paletteCC(pixelColorIndex)

    when(colEn) {
      ctrl.io.rgb.r := pixelColor(11 downto 8)
      ctrl.io.rgb.g := pixelColor(7 downto 4)
      ctrl.io.rgb.b := pixelColor(3 downto 0)
    } otherwise {
      ctrl.io.rgb.clear()
    }
  }
}
