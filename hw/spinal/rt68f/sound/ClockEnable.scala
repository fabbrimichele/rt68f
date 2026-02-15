package rt68f.sound

import spinal.core._

import scala.language.postfixOps

case class ClockEnable() extends Component {
  val io = new Bundle {
    val enable = out Bool()
  }

  // 3.58 / 16 = 0.22375
  // 0.22375 * 256 = ~57.28 -> Use 57
  private val acc = Reg(UInt(8 bits)) init 0
  private val newAcc = acc + 57
  acc := newAcc

  // The 'enable' is true ONLY on the cycle where the addition overflows
  io.enable := newAcc < acc
}
