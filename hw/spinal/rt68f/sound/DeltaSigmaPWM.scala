package rt68f.sound

import spinal.core._

import scala.language.postfixOps

case class DeltaSigmaPWM() extends Component {
  val io = new Bundle {
    val sound = in SInt(16 bits)  // Signed sample
    val sample = in Bool()        // Sample strobe
    val pwm = out Bool()          // 1-bit PWM audio output
  }

  // ---------------------------
  // 17-bit accumulator for delta-sigma PWM
  // ---------------------------
  val pwmAcc = Reg(UInt(17 bits)) init 0

  // Convert signed sample to unsigned delta for accumulation
  val sndUnsigned = io.sound.asUInt ^ U(0x8000)

  // ---------------------------
  // Update accumulator ONLY when a new sample is available
  // ---------------------------
  when(io.sample) {
    pwmAcc := pwmAcc(15 downto 0).resize(17) + sndUnsigned
  }

  // MSB of accumulator drives PWM output
  io.pwm := pwmAcc(16)
}
