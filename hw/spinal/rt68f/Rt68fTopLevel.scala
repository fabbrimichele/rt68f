package rt68f

import rt68f.core._
import rt68f.io._
import rt68f.memory._
import spinal.core._
import spinal.lib.bus.amba3.apb._
import spinal.lib.bus.misc._
import spinal.lib.com.uart._
import spinal.lib.master

import scala.language.postfixOps

/**
 * Hardware definition
 * @param romFilename name of the file containing the ROM content
 *
 * SimpleSoC Memory Map
 *
 *   0x0000  - 0x0FFF  : 4 KB ROM (16-bit words)
 *   0x0800  - 0x0FFF  : 2 KB RAM (16-bit words)
 *   0x10000 - 0x13FFF : APB3 bus (16-bit)
 *       0x10000 - 0x10FFF : LED peripheral (lower 4 bits drive LEDs)
 *       0x11000 - 0x11FFF : KEY peripheral (lower 4 bits reflect key inputs)
 *       0x12000 - 0x13FFF : reserved for future APB3 devices
 *
 * Notes:
 * - ROM is read-only, currently with no init file (optionally load via initFile).
 * - APB3 bus is hardwired for 16-bit transfers.
 * - Active-low signals: AS, DTACK (handled by Mem16Bits and bridge).
 */

//noinspection TypeAnnotation
case class Rt68fTopLevel(romFilename: String) extends Component {
  val io = new Bundle {
    val reset = in Bool()
    val led = out Bits(4 bits)
    val key = in Bits(3 bits)
    val uart = master(Uart()) // Expose UART pins (txd, rxd), must be defined in the ucf
  }

  val resetCtrl = ResetCtrl()
  resetCtrl.io.button := io.reset

  val resetArea = new ResetArea(resetCtrl.io.resetOut, cumulative = false) {
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

    // ----------------
    // ROM: 2 KB @ 0x0000 - 0x0800
    // ----------------
    val romSizeWords = 2048 / 2 // 2 KB / 2 bytes per 16-bit word
    val rom = Mem16Bits(size = romSizeWords, readOnly = true, initFile = Some(romFilename))
    val romSel = cpu.io.ADDR < U(0x800, cpu.io.ADDR.getWidth bits)

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

    // ----------------
    // RAM: 2 KB @ 0x0800 - 0x1000
    // ----------------
    val ramSizeWords = 2048 / 2 // 2 KB / 2 bytes per 16-bit word
    val ram = Mem16Bits(size = ramSizeWords)
    val ramSel = cpu.io.ADDR >= U(0x800, cpu.io.ADDR.getWidth bits) && cpu.io.ADDR < U(0x1000, cpu.io.ADDR.getWidth bits)

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

    // ----------------
    // APB3 Bridge and Devices
    // ----------------
    val apbBridge = M68kToApb3Bridge16(addrWidth = 32)

    // Connect CPU outputs to bridge inputs (only single-driver assignments)
    apbBridge.io.m68k.AS    := cpu.io.AS
    apbBridge.io.m68k.UDS   := cpu.io.UDS
    apbBridge.io.m68k.LDS   := cpu.io.LDS
    apbBridge.io.m68k.RW    := cpu.io.RW
    apbBridge.io.m68k.ADDR  := cpu.io.ADDR.resized
    apbBridge.io.m68k.DATAO := cpu.io.DATAO

    // determine APB-mapped selection for simple top-level arbitration
    val apbSel = (cpu.io.ADDR >= U(0x10000)) && (cpu.io.ADDR < U(0x14000))

    // When APB-selected, forward bridge response into CPU aggregated signals
    when(!cpu.io.AS && apbSel) {
      cpuDataI := apbBridge.io.m68k.DATAI
      cpuDtack := apbBridge.io.m68k.DTACK
    }

    // LED device (16-bit APB)
    val ledDev = LedApb16(width = 4, addressWidth = 12)
    io.led := ledDev.io.leds

    // Key device (16-bit APB)
    val keyDev = KeyApb16(width = 3, addressWidth = 12)
    keyDev.io.keys := io.key

    // Serial
    // WIDTH MISMATCH (32 bits <- 16 bits) on (toplevel/resetArea_uartDev/io_apb_PWDATA : in Bits[32 bits]) := (toplevel/[Apb3Router]/io_outputs_2_PWDATA : out Bits[16 bits])
    // TODO: I need either implement a 32bit APB3 to 68000 bridge or create an adapter between 32bit APB3 and 16bit APB3
    val uartDev = new Apb3UartCtrl16(
      UartCtrlMemoryMappedConfig(
        baudrate = 9600,
        txFifoDepth = 1,
        rxFifoDepth = 1,
      )
    )
    uartDev.io.uart <> io.uart

    // TODO: add timer
    //  https://spinalhdl.github.io/SpinalDoc-RTD/master/SpinalHDL/Examples/Advanced%20ones/timer.html

    // TODO: add interrupts
    //  https://spinalhdl.github.io/SpinalDoc-RTD/master/SpinalHDL/Libraries/regIf.html
    val apbDecoder = Apb3Decoder(
      master = apbBridge.io.apb,
      slaves = Seq(
        (ledDev.io.apb,  SizeMapping(0x10000, 4 KiB)),   // LED mapped at 0x10000
        (keyDev.io.apb,  SizeMapping(0x11000, 4 KiB)),   // KEY mapped at 0x11000
        (uartDev.io.apb, SizeMapping(0x12000, 4 KiB)),  // UART mapped at 0x12000
      )
    )
  }

  // Remove io_ prefix
  noIoPrefix()
}

object Rt68fTopLevelVhdl extends App {
  //private val romFilename = "keys.hex"
  //private val romFilename = "blinker.hex"
  //private val romFilename = "led_on.hex"
  //private val romFilename = "uart.hex"
  private val romFilename = "uart_echo.hex"

  private val report = Config.spinal.generateVhdl(Rt68fTopLevel(romFilename))
  report.mergeRTLSource("mergeRTL") // Merge all rtl sources into mergeRTL.vhd and mergeRTL.v files
  report.printPruned()
}
