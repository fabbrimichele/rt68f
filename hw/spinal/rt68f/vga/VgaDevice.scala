package rt68f.vga

import rt68f.core.M68kBus
import rt68f.vga.VgaDevice.Modes.{M0_640X400C02, M1_640X200C04}
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
    val vga             = master(Vga(VgaDevice.rgbConfig))
  }

  // TODO: add a CTRL register to switch between:
  //  - Monitor resolution: 640x480 (no black bands) and 640x400

  // Framebuffer
  val fbWidth = 32768 / 2  // 32KB = 640x400, 1 bit color
  val framebuffer = Mem(Bits(16 bits), fbWidth)

  // Palette (implemented with registers)
  // TODO: use 12 bits
  val palette = Vec.fill(4)(Reg(UInt(16 bits)))
  palette(0).init(U(0x0000))  // Initialize background color to black
  palette(1).init(U(0x0FFF))  // Initialize foreground color to white
  palette(2).init(U(0x0F00))  // Initialize color 2 to red
  palette(3).init(U(0x00F0))  // Initialize color 3 to green

  // Control register
  val controlReg = Reg(Bits(16 bits)) init 1 // Default 640x200, 4 colors

  // ------------ 68000 BUS side ------------
  // Default response
  io.bus.DATAI := 0
  io.bus.DTACK := True

  // Palette read/write
  when(!io.bus.AS && io.paletteSel) {
    io.bus.DTACK := False // acknowledge access (active low)
    val wordAddr = io.bus.ADDR(2 downto 1)

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
    reset = ClockDomain.current.reset,
    frequency = FixedFrequency(25.143 MHz),
  )

  new ClockingArea(clk25) {
    val controlRegCC = BufferCC(controlReg)
    val paletteCC =  BufferCC(palette)
    val mode = controlRegCC(0).asUInt

    val bitsPerPixel = mode.mux(
      M0_640X400C02 -> U(1, 2 bits),
      M1_640X200C04 -> U(2, 2 bits),
    )
    val bitsPerPixelReg = RegNext(bitsPerPixel)

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
    val pixelCounter = Reg(UInt(log2Up(fbWidth) + 4 bits)) init 0
    val pixelStartLineCounter = Reg(UInt(log2Up(fbWidth) + 4 bits)) init 0
    val lineCounter = Reg(UInt(9 bits)) init 0

    val fbReadAddr = mode.mux(
      M0_640X400C02 -> pixelCounter(pixelCounter.high downto 4).resize(log2Up(fbWidth)),
      M1_640X200C04 -> pixelCounter(pixelCounter.high downto 3).resize(log2Up(fbWidth))
    )

    when (frameStart) {
      lineCounter := 0
      pixelCounter := 0
      pixelStartLineCounter := 0
    } elsewhen(
      (vCount >= timings.v.colorStart && vCount < timings.v.colorEnd)
      && (hCount >= (timings.h.colorStart - latency) && hCount < timings.h.colorEnd - latency)
    ) {
      pixelCounter := pixelCounter + 1
    } elsewhen(
      (vCount >= timings.v.colorStart && vCount < timings.v.colorEnd)
        && (hCount === 0)
    ) {
      pixelCounter := pixelStartLineCounter
      lineCounter := lineCounter + 1
      // Stretch vertical resolution by 2 for all screen modes but M0_640X400C02
      when(mode === M0_640X400C02 || lineCounter.lsb === True) {
        pixelStartLineCounter := pixelStartLineCounter + 640
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
      M1_640X200C04 -> pixelX(2 downto 0).resized
    )

    when (pixelBitIndex === 0) {
      shiftRegister := wordData
    } otherwise  {
      shiftRegister := shiftRegister |<< bitsPerPixelReg
    }

    val pixelColorIndex = mode.mux(
      M0_640X400C02 -> shiftRegister.msb.asUInt.resized,
      M1_640X200C04 -> shiftRegister(15 downto 14).asUInt,
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
