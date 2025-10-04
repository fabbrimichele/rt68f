package rt68f.core

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.apb._
import spinal.lib.fsm._

import scala.language.postfixOps

case class M68kToApb3Bridge16(addrWidth: Int) extends Component {
  val io = new Bundle {
    val m68k = slave(M68kBus(addrWidth)) // slave view of the 68k bus (bridge drives DATAI/DTACK)
    val apb  = master(Apb3(Apb3Config(
      addressWidth = addrWidth,
      dataWidth    = 16
    )))
  }

  // ----------------------------------------------------------------------------
  // APB registered control signals (single-driver)
  // ----------------------------------------------------------------------------
  // registers to hold APB control signals and break combinational loops
  val pselReg    = Reg(Bits(1 bits)) init 0
  val penableReg = Reg(Bool()) init False
  val pwriteReg  = Reg(Bool()) init False
  val paddrReg   = Reg(UInt(addrWidth bits)) init(U(0, addrWidth bits))
  val pwdataReg  = Reg(Bits(16 bits)) init(B(0, 16 bits))

  // Connect registers to APB master outputs (single place where io.apb is driven)
  io.apb.PSEL   := pselReg
  io.apb.PENABLE:= penableReg
  io.apb.PWRITE := pwriteReg
  io.apb.PADDR  := paddrReg
  io.apb.PWDATA := pwdataReg

  // APB read data is directly forwarded to the 68k DATAI (combinationally fine)
  io.m68k.DATAI := io.apb.PRDATA

  // We'll drive DTACK from a register to avoid multiple drivers / glitches
  val dtackReg = Reg(Bool()) init(False)
  io.m68k.DTACK := dtackReg

  // ----------------------------------------------------------------------------
  // Simple FSM implementing APB setup/access handshake
  // - AS is active-low on your M68kBus
  // - RW: 1 = read, 0 = write. APB PWRITE = !RW
  // ----------------------------------------------------------------------------
  val fsm = new StateMachine {
    val idle : State = new State with EntryPoint {
      whenIsActive {
        // defaults in idle
        pselReg := 0
        penableReg := False
        dtackReg := False

        // wait for active transfer (AS is active-low)
        when(!io.m68k.AS) {
          // capture address and transfer direction/data at start of transaction
          paddrReg := io.m68k.ADDR.resized
          pwriteReg := !io.m68k.RW // RW: 1=read, so PWRITE = !RW
          pwdataReg := io.m68k.DATAO

          // assert PSEL in setup (registered)
          pselReg := 1
          penableReg := False

          goto(setup)
        }
      }
    }

    val setup : State = new State {
      whenIsActive {
        // Keep PSEL asserted in setup, PENABLE false (setup phase)
        pselReg := 1
        penableReg := False
        dtackReg := False

        // move to access on next clock (APB slaves now see PSEL and address)
        goto(access)
      }
    }

    val access : State = new State {
      whenIsActive {
        // Access phase: assert PENABLE (registered)
        pselReg := 1
        penableReg := True

        // Wait for slave to indicate ready
        when(io.apb.PREADY) {
          // Acknowledge CPU for one cycle
          dtackReg := True
          goto(done)
        } otherwise {
          // Keep waiting, DTACK remains False until PREADY
          dtackReg := False
        }
      }
    }

    val done : State = new State {
      whenIsActive {
        // Deassert control lines
        pselReg := 0
        penableReg := False

        // DTACK was asserted in previous state; here we can clear it.
        // Note: CPU might sample DTACK combinationally — this code asserts it for one clock.
        dtackReg := False

        // Wait for CPU to release AS (transaction finished)
        when(io.m68k.AS) {
          goto(idle)
        }
      }
    }
  } // end fsm
}