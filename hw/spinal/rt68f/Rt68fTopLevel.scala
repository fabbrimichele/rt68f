package rt68f

import rt68f.core._
import rt68f.io._
import rt68f.memory._
import spinal.core._
import spinal.lib.com.uart.Uart
import spinal.lib.graphic.vga.Vga
import spinal.lib.io.InOutWrapper
import spinal.lib.{BufferCC, master}
import vga.VgaDevice

import scala.language.postfixOps

/**
 * Hardware definition
 *
 * @param romFilename name of the file containing the ROM content
 *
 * SimpleSoC Memory Map
 *
 *   0x00000000 - 0x00003FFF : 16 KB ROM (16-bit words)
 *   0x00004000 - 0x00007FFF : 16 KB RAM (16-bit words)
 *   0x00008000 - 0x0000FFFF : 32 KB Video Framebuffer (16-bit words)
 *   0x00010000              : LED peripheral (lower 4 bits drive LEDs)
 *   0x00011000              : KEY peripheral (lower 4 bits reflect key inputs)
 *   0x00012000              : UART (base)
 *   0x00013000              ; VGA Palette (4 words, it'll be 256)
 *   0x00013100              ; VGA Control (1 word, it might increase)
 *   0x00100000 - 0x00180000 ; 512 KB SRAM
 */

//noinspection TypeAnnotation
case class Rt68fTopLevel(romFilename: String) extends Component {
  val io = new Bundle {
    val reset = in Bool()
    val led = out Bits(4 bits)
    val key = in Bits(4 bits) // Keys disabled in UCF file due to UART conflict.
    val uart = master(Uart()) // Expose UART pins (txd, rxd), must be defined in the ucf
    val vga = master(Vga(VgaDevice.rgbConfig, withColorEn = false))
    val sram = master(SRamBus())
  }

  // Clock needs to be synchronized
  // TODO: Try to debounce it syncReset.
  //  I could move the BufferCC and debounce logic inside the `ResetCtrl`.
  val syncReset = BufferCC(io.reset)
  val clk32 = ClockDomain(
    clock = ClockDomain.current.clock,
    reset = syncReset,
    frequency = FixedFrequency(32 MHz),
    config = ClockDomainConfig(
      resetKind = SYNC
    )
  )

  new ClockingArea(clk32) {

    val dcm = new Dcm32_25_16()

    val clk16 = ClockDomain(
      clock = dcm.io.CLK_OUT2,
      reset = !dcm.io.LOCKED,
      frequency = FixedFrequency(16 MHz),
    )

    // Clock domain area for CPU
    new ClockingArea(clk16) {
      // ----------------
      // CPU Core
      // ----------------
      val cpu = M68k()

      val busManager = BusManager()
      busManager.io.masterBus <> cpu.io

      // --------------------------------
      // ROM: 16 KB @ 0x0000 - 0x4FFFF
      // --------------------------------
      val romSizeWords = 16384 / 2 // 16 KB / 2 bytes per 16-bit word
      val rom = Mem16Bits(size = romSizeWords, readOnly = true, initFile = Some(romFilename))

      // Connect CPU outputs to ROM inputs
      rom.io.bus.AS := cpu.io.AS
      rom.io.bus.UDS := cpu.io.UDS
      rom.io.bus.LDS := cpu.io.LDS
      rom.io.bus.RW := cpu.io.RW
      rom.io.bus.ADDR := cpu.io.ADDR
      rom.io.bus.DATAO := cpu.io.DATAO
      rom.io.sel := busManager.io.romSel
      busManager.io.romBus.DATAI := rom.io.bus.DATAI
      busManager.io.romBus.DTACK := rom.io.bus.DTACK

      // --------------------------------
      // RAM: 16 KB @ 0x4000 - 0x7FFF
      // --------------------------------
      val ramSizeWords = 16384 / 2 // 16384 KB / 2 bytes per 16-bit word
      val ram = Mem16Bits(size = ramSizeWords)

      // Connect CPU outputs to ROM inputs
      ram.io.bus.AS := cpu.io.AS
      ram.io.bus.UDS := cpu.io.UDS
      ram.io.bus.LDS := cpu.io.LDS
      ram.io.bus.RW := cpu.io.RW
      ram.io.bus.ADDR := cpu.io.ADDR
      ram.io.bus.DATAO := cpu.io.DATAO
      ram.io.sel := busManager.io.ramSel
      busManager.io.ramBus.DATAI := ram.io.bus.DATAI
      busManager.io.ramBus.DTACK := ram.io.bus.DTACK

      // --------------------------------
      // VGA: 32 KB @ 0x8000 - 0xFFFF
      // --------------------------------
      val vga = VgaDevice()
      io.vga <> vga.io.vga

      // Connect CPU outputs to ROM inputs
      vga.io.bus.AS := cpu.io.AS
      vga.io.bus.UDS := cpu.io.UDS
      vga.io.bus.LDS := cpu.io.LDS
      vga.io.bus.RW := cpu.io.RW
      vga.io.bus.ADDR := cpu.io.ADDR
      vga.io.bus.DATAO := cpu.io.DATAO
      busManager.io.vgaBus.DATAI := vga.io.bus.DATAI
      busManager.io.vgaBus.DTACK := vga.io.bus.DTACK

      vga.io.framebufferSel := busManager.io.vgaFramebufferSel
      vga.io.paletteSel := busManager.io.vgaPaletteSel
      vga.io.controlSel := busManager.io.vgaControlSel
      vga.io.pixelClock := dcm.io.CLK_OUT1 // 25.175 MHz
      vga.io.pixelReset := !dcm.io.LOCKED

      // --------------------------------
      // LED device @ 0x10000
      // --------------------------------
      val ledDev = LedDevice()
      io.led := ledDev.io.leds

      // Connect CPU outputs to LedDev inputs
      ledDev.io.bus.AS := cpu.io.AS
      ledDev.io.bus.UDS := cpu.io.UDS
      ledDev.io.bus.LDS := cpu.io.LDS
      ledDev.io.bus.RW := cpu.io.RW
      ledDev.io.bus.ADDR := cpu.io.ADDR
      ledDev.io.bus.DATAO := cpu.io.DATAO
      ledDev.io.sel := busManager.io.ledDevSel
      busManager.io.ledBus.DATAI := ledDev.io.bus.DATAI
      busManager.io.ledBus.DTACK := ledDev.io.bus.DTACK

      // --------------------------------
      // Key device @ 0x11000
      // --------------------------------
      val keyDev = KeyDevice()
      keyDev.io.keys := io.key

      // Connect CPU outputs to LedDev inputs
      keyDev.io.bus.AS := cpu.io.AS
      keyDev.io.bus.UDS := cpu.io.UDS
      keyDev.io.bus.LDS := cpu.io.LDS
      keyDev.io.bus.RW := cpu.io.RW
      keyDev.io.bus.ADDR := cpu.io.ADDR
      keyDev.io.bus.DATAO := cpu.io.DATAO
      keyDev.io.sel := busManager.io.keyDevSel
      busManager.io.keyBus.DATAI := keyDev.io.bus.DATAI
      busManager.io.keyBus.DTACK := keyDev.io.bus.DTACK


      // --------------------------------
      // UART device @ 0x12000
      // 8 word registers
      // --------------------------------
      val uartDev = T16450Device()
      io.uart <> uartDev.io.uart

      // Connect CPU outputs to LedDev inputs
      uartDev.io.bus.AS := cpu.io.AS
      uartDev.io.bus.UDS := cpu.io.UDS
      uartDev.io.bus.LDS := cpu.io.LDS
      uartDev.io.bus.RW := cpu.io.RW
      uartDev.io.bus.ADDR := cpu.io.ADDR
      uartDev.io.bus.DATAO := cpu.io.DATAO
      uartDev.io.sel := busManager.io.uartDevSel
      busManager.io.uartBus.DATAI := uartDev.io.bus.DATAI
      busManager.io.uartBus.DTACK := uartDev.io.bus.DTACK

      // --------------------------------
      // SRAM: 512 KB @ 0x100000 - 0x180000
      // --------------------------------
      val sramCtrl = SRamCtrl()
      sramCtrl.io.bus.AS := cpu.io.AS
      sramCtrl.io.bus.UDS := cpu.io.UDS
      sramCtrl.io.bus.LDS := cpu.io.LDS
      sramCtrl.io.bus.RW := cpu.io.RW
      sramCtrl.io.bus.ADDR := cpu.io.ADDR
      sramCtrl.io.bus.DATAO := cpu.io.DATAO
      sramCtrl.io.sel := busManager.io.sramSel
      busManager.io.sramBus.DATAI := sramCtrl.io.bus.DATAI
      busManager.io.sramBus.DTACK := sramCtrl.io.bus.DTACK
      io.sram <> sramCtrl.io.sram

    }
  }

  // Remove io_ prefix
  noIoPrefix()
}

object Rt68fTopLevelVhdl extends App {
  //private val romFilename = "keys.hex"
  //private val romFilename = "blinker.hex"
  //private val romFilename = "led_on.hex"
  //private val romFilename = "uart_tx_byte.hex"
  //private val romFilename = "uart_hello.hex"
  //private val romFilename = "uart_echo.hex"
  //private val romFilename = "mem_test.hex"
  private val romFilename = "monitor.hex"
  //private val romFilename = "uart16450_echo.hex"

  private val report = Config.spinal.generateVhdl(InOutWrapper(Rt68fTopLevel(romFilename)))
  report.mergeRTLSource("mergeRTL") // Merge all rtl sources into mergeRTL.vhd and mergeRTL.v files
  report.printPruned()
}
