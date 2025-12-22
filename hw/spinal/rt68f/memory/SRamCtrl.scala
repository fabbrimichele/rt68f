package rt68f.memory

import rt68f.core.M68kBus
import spinal.core.in.Bool
import spinal.core.{B, Bits, Bundle, Cat, ClockDomain, ClockingArea, Component, False, IntToBuilder, LiteralBuilder, Reg, True, in, out, when}
import spinal.lib.fsm.{EntryPoint, State, StateMachine}
import spinal.lib.io.TriState
import spinal.lib.{IMasterSlave, master, slave}

import scala.language.postfixOps

case class SRamBus(addrWidth: Int = 19, dataWidth: Int = 8) extends Bundle with IMasterSlave {
  val addr  = Bits(addrWidth bits)
  val data  = TriState(Bits(dataWidth bits))
  val ce    = Bool()
  val we    = Bool()
  val oe    = Bool()

  override def asMaster(): Unit = {
    out(addr, ce, we, oe)
    master(data)
  }
}

/*
  The Ram controller has a 64MHz clock, 4 times the CPU clock which is 16Mhz.
  DTACK is asserted after 4 cycles (state machine) + 1 cycle (dtack reg) for
  both read and write, this corresponds to the 68000 state S2 that is before
  the 68000 expects DTACK to be asserted (S4) to avoid wait states.
 */
case class SRamCtrl(clk64: ClockDomain) extends Component {
  val io = new Bundle {
    val bus = slave(M68kBus())
    val sel = in Bool()
    val sram = master(SRamBus())
  }

  // Since 64MHz and 16MHz clock are in phase
  // there is no need of BufferCC.
  val clock64Area = new ClockingArea(clk64) {
    // Registered outputs for stability
    val sramAddr = Reg(Bits(19 bits)) init (0)
    val sramWe = Reg(Bool()) init (True)
    val sramCe = Reg(Bool()) init (True)
    val sramOe = Reg(Bool()) init (True)
    val sramDataOut = Reg(Bits(8 bits)) init (0)
    val sramWriteEn = Reg(Bool()) init (False)
    val dtack = Reg(Bool()) init (True)
    val dataBuf = Reg(Bits(16 bits)) init (0)

    // Connect registers to IO
    io.sram.addr := sramAddr
    io.sram.we := sramWe
    io.sram.ce := sramCe
    io.sram.oe := sramOe
    io.sram.data.write := sramDataOut
    io.sram.data.writeEnable := sramWriteEn
    io.bus.DTACK := dtack
    io.bus.DATAI := dataBuf

    val fsm = new StateMachine {
      val idle: State = new State with EntryPoint {
        whenIsActive {
          sramWe := True
          sramCe := True
          sramOe := True
          sramWriteEn := False
          dtack := True

          when(!io.bus.AS && io.sel) {
            when(io.bus.RW) {
              goto(readLow)
            } otherwise {
              goto(writeLowSetup)
            }
          }
        }
      }

      // -- READ SEQUENCE (Unchanged logic, just registered) --
      val readLow: State = new State {
        whenIsActive {
          sramCe := False
          sramOe := False
          sramAddr := io.bus.ADDR(18 downto 1).asBits ## B"1"
          goto(sampleLow)
        }
      }

      val sampleLow: State = new State {
        whenIsActive {
          dataBuf(7 downto 0) := io.sram.data.read
          sramAddr := io.bus.ADDR(18 downto 1).asBits ## B"0"
          goto(sampleHigh)
        }
      }

      val sampleHigh: State = new State {
        whenIsActive {
          dataBuf(15 downto 8) := io.sram.data.read
          dtack := False
          goto(waitCpu)
        }
      }

      // -- WRITE SEQUENCE (Stretched for Timing) --
      // Step 1: Set up Address and Data (Address Setup Time)
      val writeLowSetup: State = new State {
        whenIsActive {
          sramAddr := io.bus.ADDR(18 downto 1).asBits ## B"1"
          sramDataOut := io.bus.DATAO(7 downto 0)
          sramWriteEn := True
          sramCe := False
          // Only proceed if CPU is actually driving the strobe
          when(!io.bus.LDS) {
            goto(writeLowPulse)
          } elsewhen (!io.bus.UDS) {
            goto(writeHighSetup)
          }
        }
      }

      // Step 2: Assert WE (Write Pulse Width)
      val writeLowPulse: State = new State {
        whenIsActive {
          sramWe := False
          goto(writeHighSetup)
        }
      }

      // Step 3: Setup High Byte
      val writeHighSetup: State = new State {
        whenIsActive {
          sramWe := True // End previous pulse
          sramAddr := io.bus.ADDR(18 downto 1).asBits ## B"0"
          sramDataOut := io.bus.DATAO(15 downto 8)
          when(!io.bus.UDS) {
            goto(writeHighPulse)
          } otherwise {
            dtack := False
            goto(waitCpu)
          }
        }
      }

      // Step 4: Assert WE for High Byte
      val writeHighPulse: State = new State {
        whenIsActive {
          sramWe := False
          dtack := False // Signal completion to 68k
          goto(waitCpu)
        }
      }

      val waitCpu: State = new State {
        whenIsActive {
          sramWe := True
          sramCe := False // Keep CE low until cycle ends
          sramWriteEn := !io.bus.RW // Keep driving data bus for writes
          when(io.bus.AS) {
            goto(idle)
          }
        }
      }
    }
  }
}
