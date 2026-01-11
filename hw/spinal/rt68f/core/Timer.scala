package rt68f.core

import spinal.core._
import spinal.lib._

import scala.language.postfixOps

case class Timer() extends Component {
  val io = new Bundle {
    val bus       = slave(M68kBus())
    val sel       = in Bool()         // chip select from decoder
    val timerAInt = out Bool()        // Timer A interrupt
    val timerBInt = out Bool()        // Timer B interrupt
  }

  // --------- Registers ---------
  val ctrlReg    = Reg(Bits(8 bits))  init 0
  val prescalerA = Reg(Bits(16 bits)) init 0
  val counterA   = Reg(Bits(16 bits)) init 0
  val prescalerB = Reg(Bits(16 bits)) init 0
  val counterB   = Reg(Bits(16 bits)) init 0

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
        1 -> prescalerA,
        2 -> counterA,
        3 -> prescalerB,
        4 -> counterB,
        default -> B"xFFFF"
      )
    } otherwise {
      // Write
      // TODO: manage UDS/LDS
      val dataOut = io.bus.DATAO
      switch(addrWord) {
        is(0) { ctrlReg := dataOut(7 downto 0) }
        is(1) { prescalerA := dataOut }
        is(2) { counterA := dataOut }
        is(3) { prescalerB := dataOut }
        is(4) { counterB := dataOut }
      }
    }
  }

  // TODO: implement the counters and interrupts
}
