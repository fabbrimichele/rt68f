package rt68f.sound

import rt68f.core.M68kBus
import spinal.core._
import spinal.lib._

import scala.language.postfixOps

/*
  OPL2/YM3812 Device
 */
case class Opl2Device() extends Component {
  val io = new Bundle {
    val bus = slave(M68kBus())
    val sel = in Bool() // chip select from decoder
    val int = out Bool() // UART interrupt
    val pwmAudio = out Bool() // PWM sound
  }

  /*
    TODO: it seems to work I:
      - wrote a value to the timer 1 register ($02)
      - enable the timer 1 writing 1 to the timer control register ($04)
      - read from the status register which returned $C6 -> 1100 01100
        where b7 is interrupt and b6 is that timer 1 overflowed
     Now I could either:
      - Write a small program that uses the time to trigger an interrupt subroutine
      - Continue working on the HW side and generate the PWM pulse from the OPL audio sample output
    TODO: reset is not working, it should be kept low for at least 6 clock cycles
   */

  val opl2 = new JtOpl2BB()
  val deltaSigmaPWM = DeltaSigmaPWM()
  val clockEnable = ClockEnable().io.enable

  deltaSigmaPWM.io.sound := opl2.io.snd
  deltaSigmaPWM.io.sample := opl2.io.sample
  io.pwmAudio := deltaSigmaPWM.io.pwm

  // -------------------
  // 68000 bus
  // -------------------
  val pendingWrite = Reg(Bool()) init False
  val latchedAddr = Reg(Bool()) init False
  val latchedData = Reg(Bits(8 bits))

  val devSel = !io.bus.AS && io.sel

  opl2.io.cen := clockEnable
  opl2.io.addr := latchedAddr
  opl2.io.din := latchedData

  val doOplWrite = pendingWrite && clockEnable
  opl2.io.cs_n := !doOplWrite
  opl2.io.wr_n := !doOplWrite

  io.int := !opl2.io.irq_n
  io.bus.DATAI := opl2.io.dout.resized

  when(devSel && !io.bus.RW && !pendingWrite) {
    // Latch 68000 write
    pendingWrite := True
    latchedAddr := io.bus.ADDR(1)
    latchedData := io.bus.DATAO(7 downto 0)
  }

  when(doOplWrite) {
    pendingWrite := False
  }

  // We only give DTACK back to the CPU once write has been cleared
  io.bus.DTACK := True
  when(devSel) {
    when(!io.bus.RW) {
      // For writes, wait until the OPL2 has swallowed the data
      when(!pendingWrite) { io.bus.DTACK := False }
    } otherwise {
      // For reads (Status Register), we can usually DTACK immediately
      // as JT02 status is typically asynchronous.
      io.bus.DTACK := False
    }
  }
}
