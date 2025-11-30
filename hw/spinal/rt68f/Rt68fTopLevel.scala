package rt68f

import rt68f.core._
import rt68f.io._
import rt68f.memory._
import spinal.core._
import spinal.lib.com.uart.Uart
import spinal.lib.graphic.vga.Vga
import spinal.lib.master
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
 */

//noinspection TypeAnnotation
case class Rt68fTopLevel(romFilename: String) extends Component {
  val io = new Bundle {
    val reset = in Bool()
    val led = out Bits(4 bits)
    val key = in Bits(4 bits) // Keys disabled in UCF file due to UART conflict.
    val uart = master(Uart()) // Expose UART pins (txd, rxd), must be defined in the ucf
    val vga = master(Vga(VgaDevice.rgbConfig))
  }

  val resetCtrl = ResetCtrl()
  resetCtrl.io.button := io.reset

  val dcm = new Dcm32_25_16()

  val clk25 = ClockDomain(
    clock = dcm.io.CLK_OUT1,
    reset = resetCtrl.io.resetOut,
    frequency = FixedFrequency(25.143 MHz)
  )

  val clk16 = ClockDomain(
    clock = dcm.io.CLK_OUT2,
    reset = resetCtrl.io.resetOut,
    frequency = FixedFrequency(16 MHz)
  )

  // Clock domain area for CPU
  new ClockingArea(clk16) {
    // ----------------
    // CPU Core
    // ----------------
    val cpu = M68k()
    val cpuDataI = Bits(16 bits)
    val cpuDtack = Bool()

    // Connect CPU inputs to the aggregated signals
    cpu.io.DATAI := cpuDataI
    cpu.io.DTACK := cpuDtack

    // Default responses
    cpuDataI := B(0, 16 bits)
    cpuDtack := True

    io.vga.color.r := B(0, 4 bits).asUInt
    io.vga.color.g := B(0, 4 bits).asUInt
    io.vga.color.b := B(0, 4 bits).asUInt
    io.vga.vSync := False
    io.vga.hSync := False
    io.vga.colorEn := False

    // --------------------------------
    // ROM: 16 KB @ 0x0000 - 0x4FFFF
    // --------------------------------
    val romSizeWords = 16384 / 2 // 16 KB / 2 bytes per 16-bit word
    val rom = Mem16Bits(size = romSizeWords, readOnly = true, initFile = Some(romFilename))
    val romSel = cpu.io.ADDR < U(0x3FFF, cpu.io.ADDR.getWidth bits)

    // Connect CPU outputs to ROM inputs
    rom.io.bus.AS    := cpu.io.AS
    rom.io.bus.UDS   := cpu.io.UDS
    rom.io.bus.LDS   := cpu.io.LDS
    rom.io.bus.RW    := cpu.io.RW
    rom.io.bus.ADDR  := cpu.io.ADDR
    rom.io.bus.DATAO := cpu.io.DATAO

    rom.io.sel := romSel

    // If ROM selected, forward ROM response into CPU aggregated signals
    when(!cpu.io.AS && romSel) {
      cpuDataI := rom.io.bus.DATAI
      cpuDtack := rom.io.bus.DTACK
    }


    // --------------------------------
    // RAM: 16 KB @ 0x4000 - 0x7FFF
    // --------------------------------
    val ramSizeWords = 16384 / 2 // 16384 KB / 2 bytes per 16-bit word
    val ram = Mem16Bits(size = ramSizeWords)
    val ramSel = cpu.io.ADDR >= U(0x4000, cpu.io.ADDR.getWidth bits) && cpu.io.ADDR < U(0x7FFF, cpu.io.ADDR.getWidth bits)

    // Connect CPU outputs to ROM inputs
    ram.io.bus.AS    := cpu.io.AS
    ram.io.bus.UDS   := cpu.io.UDS
    ram.io.bus.LDS   := cpu.io.LDS
    ram.io.bus.RW    := cpu.io.RW
    ram.io.bus.ADDR  := cpu.io.ADDR
    ram.io.bus.DATAO := cpu.io.DATAO

    ram.io.sel := ramSel

    // If RAM selected, forward RAM response into CPU aggregated signals
    when(!cpu.io.AS && ramSel) {
      cpuDataI := ram.io.bus.DATAI
      cpuDtack := ram.io.bus.DTACK
    }

    /*
    // --------------------------------
    // VGA: 32 KB @ 0x8000 - 0xFFFF
    // --------------------------------
    val vga = VgaDevice()
    io.vga <> vga.io.vga

    val vgaFramebufferSel = cpu.io.ADDR >= U(0x8000, cpu.io.ADDR.getWidth bits) && cpu.io.ADDR < U(0xFFFF, cpu.io.ADDR.getWidth bits)
    val vgaPaletteSel = cpu.io.ADDR >= U(0x13000, cpu.io.ADDR.getWidth bits) && cpu.io.ADDR < U(0x13008, cpu.io.ADDR.getWidth bits)
    val vgaControlSel = cpu.io.ADDR === U(0x13100, cpu.io.ADDR.getWidth bits)

    // Connect CPU outputs to ROM inputs
    vga.io.bus.AS    := cpu.io.AS
    vga.io.bus.UDS   := cpu.io.UDS
    vga.io.bus.LDS   := cpu.io.LDS
    vga.io.bus.RW    := cpu.io.RW
    vga.io.bus.ADDR  := cpu.io.ADDR
    vga.io.bus.DATAO := cpu.io.DATAO

    vga.io.framebufferSel := vgaFramebufferSel
    vga.io.paletteSel := vgaPaletteSel
    vga.io.controlSel := vgaControlSel

    // If VGA selected, forward VGA response into CPU aggregated signals
    when(!cpu.io.AS && (vgaFramebufferSel || vgaPaletteSel || vgaControlSel)) {
      cpuDataI := vga.io.bus.DATAI
      cpuDtack := vga.io.bus.DTACK
    }
    */

    // --------------------------------
    // LED device @ 0x10000
    // --------------------------------
    val ledDev = LedDevice()
    val ledDevSel = cpu.io.ADDR === U(0x10000, cpu.io.ADDR.getWidth bits)
    io.led := ledDev.io.leds

    // Connect CPU outputs to LedDev inputs
    ledDev.io.bus.AS    := cpu.io.AS
    ledDev.io.bus.UDS   := cpu.io.UDS
    ledDev.io.bus.LDS   := cpu.io.LDS
    ledDev.io.bus.RW    := cpu.io.RW
    ledDev.io.bus.ADDR  := cpu.io.ADDR
    ledDev.io.bus.DATAO := cpu.io.DATAO

    ledDev.io.sel := ledDevSel

    // If LED selected, forward LED response into CPU aggregated signals
    when(!cpu.io.AS && ledDevSel) {
      cpuDataI := ledDev.io.bus.DATAI
      cpuDtack := ledDev.io.bus.DTACK
    }


    // --------------------------------
    // Key device @ 0x11000
    // --------------------------------
    val keyDev = KeyDevice()
    val keyDevSel = cpu.io.ADDR === U(0x11000, cpu.io.ADDR.getWidth bits)
    keyDev.io.keys := io.key

    // Connect CPU outputs to LedDev inputs
    keyDev.io.bus.AS    := cpu.io.AS
    keyDev.io.bus.UDS   := cpu.io.UDS
    keyDev.io.bus.LDS   := cpu.io.LDS
    keyDev.io.bus.RW    := cpu.io.RW
    keyDev.io.bus.ADDR  := cpu.io.ADDR
    keyDev.io.bus.DATAO := cpu.io.DATAO

    keyDev.io.sel := keyDevSel

    // If KEY selected, forward KEY response into CPU aggregated signals
    when(!cpu.io.AS && keyDevSel) {
      cpuDataI := keyDev.io.bus.DATAI
      cpuDtack := keyDev.io.bus.DTACK
    }


    // --------------------------------
    // UART device @ 0x12000
    // --------------------------------
    val uartDev = UartDevice()
    val uartDevSel = cpu.io.ADDR === U(0x12000, cpu.io.ADDR.getWidth bits) ||
      cpu.io.ADDR === U(0x12002, cpu.io.ADDR.getWidth bits)

    io.uart <> uartDev.io.uart

    // Connect CPU outputs to LedDev inputs
    uartDev.io.bus.AS    := cpu.io.AS
    uartDev.io.bus.UDS   := cpu.io.UDS
    uartDev.io.bus.LDS   := cpu.io.LDS
    uartDev.io.bus.RW    := cpu.io.RW
    uartDev.io.bus.ADDR  := cpu.io.ADDR
    uartDev.io.bus.DATAO := cpu.io.DATAO

    uartDev.io.sel := uartDevSel

    // If UART selected, forward UART response into CPU aggregated signals
    when(!cpu.io.AS && uartDevSel) {
      cpuDataI := uartDev.io.bus.DATAI
      cpuDtack := uartDev.io.bus.DTACK
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

  private val report = Config.spinal.generateVhdl(Rt68fTopLevel(romFilename))
  report.mergeRTLSource("mergeRTL") // Merge all rtl sources into mergeRTL.vhd and mergeRTL.v files
  report.printPruned()
}
