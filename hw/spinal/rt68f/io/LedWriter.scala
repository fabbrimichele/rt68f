package rt68f.io

import spinal.core.{Bundle, Component, False, True, in, out}
import spinal.lib.bus.amba3.apb.Apb3
import spinal.lib.com.uart.Apb3UartCtrl
import spinal.lib.fsm.{EntryPoint, State, StateDelay, StateMachine}
import spinal.lib.master

case class LedWriter() extends Component {
  val io = new Bundle {
    val apb = master(Apb3(Apb3UartCtrl.getApb3Config))
  }

  // Default assignments (avoid latches)
  io.apb.PSEL    := False.asBits
  io.apb.PENABLE := False
  io.apb.PWRITE  := False
  io.apb.PADDR   := 0x0
  io.apb.PWDATA  := 0x0

  val fsm = new StateMachine {
    val idle: State = new State with EntryPoint {
      whenIsActive {
        goto(write)
      }
    }

    val write: State = new State { // Wait one second
      whenIsActive {
        // Drive APB3 signals
        apbWrite(0x00, 0x41) // 0x00 = write FIFO, 0x41 = 'A'
        goto(done)
      }
    }

    val done: State = new StateDelay(cyclesCount = 32_000_000) {
      whenCompleted {
        goto(idle)
      }
    }
  }

  // Helper to write to APB3
  def apbWrite(addr: Int, data: Int): Unit = {
    io.apb.PSEL    := True.asBits
    io.apb.PENABLE := True
    io.apb.PWRITE  := True
    io.apb.PADDR   := addr
    io.apb.PWDATA  := data
  }
}
