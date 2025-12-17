package rt68f.io

import rt68f.core.M68kBus
import spinal.core.in.Bool
import spinal.core.{Bundle, Component, False, IntToBuilder, Reg, True, in, when}
import spinal.lib.com.uart.Uart
import spinal.lib.fsm.{EntryPoint, State, StateMachine}
import spinal.lib.{master, slave}

case class T16450Device() extends Component {
  val io = new Bundle {
    val bus = slave(M68kBus())
    val sel = in Bool() // chip select from decoder
    val uart = master(Uart())
  }

  val uart = new T16450BB()

  val uartSel = !io.bus.AS && io.sel

  // Default Settings (Copied from MG68)
  uart.io.RClk := uart.io.BaudOut
  uart.io.CS_n := !uartSel
  uart.io.Rd_n := !io.bus.RW
  uart.io.Wr_n := io.bus.RW
  uart.io.A := io.bus.ADDR(3 downto 1).asBits
  uart.io.D_In := io.bus.DATAO(7 downto 0)
  io.bus.DATAI := uart.io.D_Out.resized
  uart.io.SIn := io.uart.rxd
  uart.io.CTS_n := False
  uart.io.DSR_n := False
  uart.io.RI_n := False
  uart.io.DCD_n := False
  io.uart.txd := uart.io.SOut
  /*
		uart.io.RTS_n	  => open,
		uart.io.DTR_n	  => open,
		uart.io.OUT1_n  => open,
		uart.io.OUT2_n  => open,
    uart.io.Intr	  => open
  */

  io.bus.DTACK := True  // inactive (assuming active low)
  when(uartSel) {
    io.bus.DTACK := False // acknowledge access (active low)
  }

  /*
  // TODO: I'm not sure this is correct, it should keep
  //       DTACK set for the whole time uartSel is active
  //
  // It might work, try again once UART can write bytes
  // Delay DTACK assertion of 1 cycle
  val dtackReg = Reg(Bool()) init True
  io.bus.DTACK := dtackReg
  val fsm = new StateMachine {
    val idle : State = new State with EntryPoint {
      whenIsActive {
        when(uartSel) {
          goto(done)
        }
      }
    }
    val done : State = new State {
      whenIsActive(goto(idle))
      onExit(dtackReg := False)
    }
  }
   */
}