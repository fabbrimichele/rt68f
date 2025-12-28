package rt68f

import rt68f.core._
import rt68f.io._
import rt68f.memory._
import spinal.core._
import spinal.lib.com.uart.Uart
import spinal.lib.graphic.vga.Vga
import spinal.lib.io.InOutWrapper
import spinal.lib.{BufferCC, ResetCtrl, master}
import vga.VgaDevice

import scala.language.postfixOps

/**
 * Hardware definition
 *
 * @param romFilename name of the file containing the ROM content
 *
 * SimpleSoC Memory Map
 *   0x00000000 - 0x00000007 : Shadowed ROM (for Reset SP and PC)
 *   0x00000008 - 0x0007FFFF : 512 KB RAM (minus 2 longs)
 *   0x00200000 - 0x0020FA00 : 64 KB Video Framebuffer (16-bit words)
 *   0x00300000 - 0x00307FFF : 16 KB ROM (16-bit words)
 *   0x00400000 - 0x00400000 : LED peripheral (lower 4 bits drive LEDs)
 *   0x00401000 - 0x00401000 : KEY peripheral (lower 4 bits reflect key inputs)
 *   0x00402000 - 0x00402010 : UART
 *   0x00403000 - 0x00403020 : VGA Palette (16 words, it'll be 256)
 *   0x00403100 - 0x00403100 : VGA Control (1 word, it might increase)
 */

//noinspection TypeAnnotation
case class Rt68fTopLevel(romFilename: String) extends Component {
  val io = new Bundle {
    val clk = in Bool()
    val reset = in Bool()
    val led = out Bits(4 bits)
    val key = in Bits(4 bits) // Keys disabled in UCF file due to UART conflict.
    val uart = master(Uart()) // Expose UART pins (txd, rxd), must be defined in the ucf
    val vga = master(Vga(VgaDevice.rgbConfig, withColorEn = false))
    val sram = master(SRamBus())
  }

  val clkCtrl = ClockCtrl()
  clkCtrl.io.clkIn := io.clk
  clkCtrl.io.reset := io.reset

  // Clock domain area for CPU
  new ClockingArea(clkCtrl.clk16) {
    // ----------------
    // CPU Core
    // ----------------
    val cpu = M68k()

    val busManager = BusManager()
    busManager.io.cpuBus <> cpu.io

    // --------------------------------
    // ROM: 1.5 KB @ 0x0000 - 0x4FFFF TODO: update range in the comment
    // --------------------------------
    val romSizeWords = 1536 / 2 // 1.5 KB / 2 bytes per 16-bit word
    val rom = Mem16Bits(size = romSizeWords, readOnly = true, initFile = Some(romFilename))

    busManager.io.romBus <> rom.io.bus
    rom.io.sel := busManager.io.romSel

    // --------------------------------
    // RAM: 16 KB @ 0x4000 - 0x7FFF
    // --------------------------------
    val ramSizeWords = 16384 / 2 // 16384 KB / 2 bytes per 16-bit word
    val ram = Mem16Bits(size = ramSizeWords)

    busManager.io.ramBus <> ram.io.bus
    ram.io.sel := busManager.io.ramSel

    // --------------------------------
    // VGA: 64000 bytes @ 0x8000 - 0xFFFF TODO: update range in the comment
    // --------------------------------
    val vga = VgaDevice(clkCtrl.clk25)
    io.vga <> vga.io.vga

    busManager.io.vgaBus <> vga.io.bus
    vga.io.framebufferSel := busManager.io.vgaFramebufferSel
    vga.io.paletteSel := busManager.io.vgaPaletteSel
    vga.io.controlSel := busManager.io.vgaControlSel

    // --------------------------------
    // LED device @ 0x10000
    // --------------------------------
    val ledDev = LedDevice()
    io.led := ledDev.io.leds

    busManager.io.ledBus <> ledDev.io.bus
    ledDev.io.sel := busManager.io.ledDevSel

    // --------------------------------
    // Key device @ 0x11000
    // --------------------------------
    val keyDev = KeyDevice()
    keyDev.io.keys := io.key

    busManager.io.keyBus <> keyDev.io.bus
    keyDev.io.sel := busManager.io.keyDevSel


    // --------------------------------
    // UART device @ 0x12000
    // 8 word registers
    // --------------------------------
    val uartDev = T16450Device()
    io.uart <> uartDev.io.uart

    busManager.io.uartBus <> uartDev.io.bus
    uartDev.io.sel := busManager.io.uartDevSel

    // --------------------------------
    // SRAM: 512 KB @ 0x100000 - 0x180000
    // --------------------------------
    val sramCtrl = SRamCtrl(clkCtrl.clk64)
    io.sram <> sramCtrl.io.sram

    busManager.io.sramBus <> sramCtrl.io.bus
    sramCtrl.io.sel := busManager.io.sramSel
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
