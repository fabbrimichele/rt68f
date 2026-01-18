package rt68f.timer

import rt68f.core.M68kBus
import spinal.core._
import spinal.lib._

import scala.language.postfixOps

/**
 * # ctrlReg (8 bits - read/write)
 * Bit 0: Timer mode: 0 -> repeat, 1 -> single
 * Bit 1: Timer int : 0 -> off, 1 -> on
 * Bit 6: Timer ack : Write to acknowledge Timer A interrupt
 *
 * # initPresc (8 bits - read/write)
 * It divides the clock by the set value, 0 stop counter
 *
 * # InitCount (16 bits - write only)
 * Initial value for Counter
 *
 * # Counter (16 bits - read only)
 * Current counter value
 */
//noinspection TypeAnnotation
case class TimerDevice() extends Component {
  val io = new Bundle {
    val bus = slave(M68kBus())
    val sel = in Bool()         // chip select from decoder
    val int = out Bool()        // Timer interrupt
  }

  // -- Memory Mapped Registers --
  val ctrlReg    = Reg(Bits(8 bits))  init 0 // Read/Write

  val initPresc = Reg(UInt(8 bits))  init 0 // Read/Write
  val initCount = Reg(UInt(16 bits)) init 0 // Write only
  val counter   = Reg(UInt(16 bits)) init 0 // Read only

  // -- Non-mapped Registers and Signals --
  val prescCount = Reg(UInt(8 bits)) init 0
  val intPending = RegInit(False)
  val intEn      = ctrlReg(1)
  val intAckBit  = 6
  io.int := intPending

  // ---- 68000 Bus Interface ----
  // Default bus signals
  io.bus.DATAI := 0     // default
  io.bus.DTACK := True  // inactive (assuming active low)

  when(!io.bus.AS && io.sel) {
    io.bus.DTACK := False // acknowledge access (active low)
    val addrWord = io.bus.ADDR(3 downto 1)

    when(io.bus.RW) {
      // Read
      io.bus.DATAI := addrWord.mux(
        0 -> ctrlReg.resize(16),
        1 -> initPresc.asBits.resize(16),
        2 -> counter.asBits,
        default -> B"xFFFF"
      )
    } otherwise {
      // Write
      // TODO: manage UDS/LDS
      val dataOut = io.bus.DATAO
      switch(addrWord) {
        is(0) { ctrlReg := dataOut(7 downto 0) }
        is(1) { initPresc := dataOut(7 downto 0).asUInt }
        is(2) { initCount := dataOut.asUInt }
      }
    }
  }

  // ---- Timer Handlers ----
  // TODO: Implement single mode
  when (initPresc > 0) {
    // Prescaler
    when (prescCount === 0) {
      prescCount := initPresc
    } otherwise {
      prescCount := prescCount - 1
    }

    // Counter
    when (counter === 0) {
      counter := initCount
    } elsewhen(prescCount === 0) {
      counter := counter - 1
    }

    // Interrupt
    when (counter === 0 && intEn) {
      intPending := True
    } elsewhen (!io.bus.AS && io.sel && !io.bus.RW) {
      when(!io.bus.LDS && io.bus.DATAO(intAckBit)) {
        intPending := False
      }
    }
  }
}
