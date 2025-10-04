package rt68f.core

import spinal.core._

import scala.language.postfixOps

case class ResetCtrl(resetHoldCycles: Int = 32) extends Component {
  val io = new Bundle {
    val button = in Bool() // Active high
    val resetOut = out Bool()
  }

  val counter = Reg(UInt(log2Up(resetHoldCycles) bits)) init 0
  val powerOnReset = Reg(Bool()) init True

  when(powerOnReset) {
    counter := counter + 1
    when (counter === (resetHoldCycles - 1)) {
      powerOnReset := False
    }
  }

  // Button forces reset back on
  when(io.button) {
    powerOnReset := True
    counter := 0
  }

  io.resetOut := powerOnReset
}
