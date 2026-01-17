package rt68f.timer

import rt68f.core.M68kBus
import spinal.core._
import spinal.lib._

import scala.language.postfixOps

/**
 * # ctrlReg (8 bits - read/write)
 * Bit 0: Timer A mode: 0 -> repeat, 1 -> single
 * Bit 1: Timer B mode: 0 -> repeat, 1 -> single
 * Bit 2: Timer A int : 0 -> off, 1 -> on
 * Bit 3: Timer B int : 0 -> off, 1 -> on
 * Bit 6: Timer A ack : Write to acknowledge Timer A interrupt
 * Bit 7: Timer B ack : Write to acknowledge Timer B interrupt
 *
 * # initPrescX (8 bits - read/write)
 * It divides the clock by the set value, 0 stop counter (A or B)
 *
 * # InitCountX (16 bits - write only)
 * Initial value for Counter (A or B)
 *
 * # CounterX (16 bits - read only)
 * Current counter value
 */
//noinspection TypeAnnotation
case class TimerDevice() extends Component {
  val io = new Bundle {
    val bus       = slave(M68kBus())
    val sel       = in Bool()         // chip select from decoder
    val timerAInt = out Bool()        // Timer A interrupt
    val timerBInt = out Bool()        // Timer B interrupt
  }

  // -- Memory Mapped Registers --
  val ctrlReg    = Reg(Bits(8 bits))  init 0 // Read/Write

  val initPrescA = Reg(UInt(8 bits))  init 0 // Read/Write
  val initCountA = Reg(UInt(16 bits)) init 0 // Write only
  val counterA   = Reg(UInt(16 bits)) init 0 // Read only

  // TODO: For some reason timer B blocks the boot loading...
  // TODO: It might be a good idea to define a TimerDevice class
  //       that has only 1 timer and then create multiple instances
  //       of the TimeDevice. This way adding a new timer is trivial
  /*
  val initPrescB = Reg(UInt(8 bits))  init 0 // Read/Write
  val initCountB = Reg(UInt(16 bits)) init 0 // Write only
  val counterB   = Reg(UInt(16 bits)) init 0 // Read only
  */

  // -- Non-mapped Registers and Signals --
  val prescCountA = Reg(UInt(8 bits)) init 0
  val intAPending = RegInit(False)
  val intAEn      = ctrlReg(2)
  val intAAckBit  = 6
  io.timerAInt := intAPending

  /*
  val prescCountB = Reg(UInt(8 bits)) init 0
  val intBPending = RegInit(False)
  val intBEn      = ctrlReg(3)
  val intBAckBit  = 7
  io.timerBInt := intBPending
  */
  io.timerBInt := False

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
        1 -> initPrescA.asBits.resize(16),
        2 -> counterA.asBits,
        /*
        3 -> initPrescB.asBits.resize(16),
        4 -> counterB.asBits,
        */
        default -> B"xFFFF"
      )
    } otherwise {
      // Write
      // TODO: manage UDS/LDS
      val dataOut = io.bus.DATAO
      switch(addrWord) {
        is(0) { ctrlReg := dataOut(7 downto 0) }
        is(1) { initPrescA := dataOut(7 downto 0).asUInt }
        is(2) { initCountA := dataOut.asUInt }
        /*
        is(3) { initPrescB := dataOut(7 downto 0).asUInt }
        is(4) { initCountB := dataOut.asUInt }
        */
      }
    }
  }

  // ---- Timer Handlers ----
  // TODO: Implement single mode
  when (initPrescA > 0) {
    // Prescaler Counter A
    when (prescCountA === 0) {
      prescCountA := initPrescA
    } otherwise {
      prescCountA := prescCountA - 1
    }

    // Counter A
    when (counterA === 0) {
      counterA := initCountA
    } elsewhen(prescCountA === 0) {
      counterA := counterA - 1
    }

    // Interrupt A
    when (counterA === 0 && intAEn) {
      intAPending := True
    } elsewhen (!io.bus.AS && io.sel && !io.bus.RW) {
      when(!io.bus.LDS && io.bus.DATAO(intAAckBit)) {
        intAPending := False
      }
    }
  }

  /*
  // TODO: Implement single mode
  when (initPrescB > 0) {
    // Prescaler Counter B
    when (prescCountB === 0) {
      prescCountB := initPrescB
    } otherwise {
      prescCountB := prescCountB - 1
    }

    // Counter B
    when (counterB === 0) {
      counterB := initCountB
    } elsewhen(prescCountB === 0) {
      counterB := counterB - 1
    }

    // Interrupt B
    when (counterB === 0 && intBEn) {
      intBPending := True
    } elsewhen (!io.bus.AS && io.sel && !io.bus.RW) {
      when(!io.bus.LDS && io.bus.DATAO(intBAckBit)) {
        intBPending := False
      }
    }
  }
  */
}
