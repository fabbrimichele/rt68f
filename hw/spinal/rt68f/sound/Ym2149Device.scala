package rt68f.sound

import rt68f.core.M68kBus
import spinal.core._
import spinal.lib._

import scala.language.postfixOps

case class Ym2149Device() extends Component {
  val io = new Bundle {
    val bus = slave(M68kBus())
    val sel = in Bool() // chip select from decoder
    val pwmAudio = out Bool() // PWM sound
  }

  private val psgEnable = new Area {
    // 1. Create the 4MHz enable pulse from the 16MHz clock
    private val psgEn = Reg(UInt(2 bits)) init 0
    psgEn := psgEn + 1
    val tick = (psgEn === 3) // High for 1 cycle out of 4
  }

  // -- PSG --
  private val psg = new Ym2149BB()
  psg.io.en_clk_psg_i := True // TODO: set to `psgEnable.tick`, it'll require latching the bus
  psg.io.sel_n_i := False // Divide by 2 the `io._clk_psg_i` clock (in this case 16 MHz/2 = 8 MHz)
  psg.io.data_i := io.bus.DATAO(7 downto 0)
  io.bus.DATAI := psg.io.data_r_o.resized

  /*
    |SND_EN|RW (68k) |A1 (68k)|BDIR|BC1|PSG State    |
    +------+---------+--------+----+---+-------------+
    |0     |X        |X       |0   |0  |Inactive     |
    |1     |0 (Write)|0       |1   |1  |Latch Address|
    |1     |0 (Write)|1       |1   |0  |Write Data   |
    |1     |1 (Read) |0       |0   |1  |Read Data    |
    |1     |1 (Read) |1       |0   |0  |Inactive     |

    Note: It works like on a real Atari ST:
    - reading from the Address Latch offset (A1=0) is
      the standard way to read data back,
    - reading from A1=1 usually results in an inactive bus.
  */
  private val psgSel = !io.bus.AS && io.sel
  psg.io.bdir_i := psgSel && !io.bus.RW
  psg.io.bc_i := psgSel && !io.bus.ADDR(1)
  io.bus.DTACK := True  // inactive (assuming active low)

  // -- PWM --
  private val pwm = DeltaSigmaPWM(sampleSizeInBits = 14)
  pwm.io.sound := psg.io.pcm14s_o
  pwm.io.sample := True // TODO: set to `psgEnable.tick`
  io.pwmAudio := pwm.io.pwm
}
