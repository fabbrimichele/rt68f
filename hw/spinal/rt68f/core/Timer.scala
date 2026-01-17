package rt68f.core

import spinal.core._
import spinal.lib._

import scala.language.postfixOps

/**
 * # Control Register (8 bits)
 * Bit 0: Timer A mode: 0 -> repeat, 1 -> single
 * Bit 1: Timer B mode: 0 -> repeat, 1 -> single
 * Bit 2: Timer A int : 0 -> off, 1 -> on
 * Bit 3: Timer B int : 0 -> off, 1 -> on
 * Bit 6: Timer A ack : Write to acknowledge Timer A interrupt
 * Bit 7: Timer B ack : Write to acknowledge Timer B interrupt
 *
 * # Prescaler (8 bits)
 * It divides the clock by the set value, 0 stop counter
 *
 * # Init
 */
//noinspection TypeAnnotation
case class Timer() extends Component {
  val io = new Bundle {
    val bus       = slave(M68kBus())
    val sel       = in Bool()         // chip select from decoder
    val timerAInt = out Bool()        // Timer A interrupt
    val timerBInt = out Bool()        // Timer B interrupt
  }

  // -- Memory Mapped Registers --
  val ctrlReg    = Reg(Bits(8 bits))  init 0 // Read/Write
  val initPrescA = Reg(Bits(8 bits))  init 0 // Read/Write
  val initValueA = Reg(Bits(16 bits)) init 0 // Write only
  val counterA   = Reg(Bits(16 bits)) init 0 // Read only
  val initPrescB = Reg(Bits(8 bits))  init 0 // Read/Write
  val initValueB = Reg(Bits(16 bits)) init 0 // Write only
  val counterB   = Reg(Bits(16 bits)) init 0 // Read only

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
        1 -> initPrescA.resize(16),
        2 -> counterA,
        3 -> initPrescB.resize(16),
        4 -> counterB,
        default -> B"xFFFF"
      )
    } otherwise {
      // Write
      // TODO: manage UDS/LDS
      val dataOut = io.bus.DATAO
      switch(addrWord) {
        is(0) { ctrlReg := dataOut(7 downto 0) }
        is(1) { initPrescA := dataOut(7 downto 0) }
        is(2) { initValueA := dataOut }
        is(3) { initPrescB := dataOut(7 downto 0) }
        is(4) { initValueB := dataOut }
      }
    }
  }
}
