package rt68f.memory

import rt68f.core.M68kBus
import spinal.core.in.Bool
import spinal.core.{B, Bits, Bundle, Cat, Component, False, IntToBuilder, LiteralBuilder, Reg, True, in, out, when}
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

case class SRamCtrl() extends Component {
  val io = new Bundle {
    val bus = slave(M68kBus())
    val sel = in Bool()
    val sram = master(SRamBus())
  }

  // Default state
  io.sram.we := True // Disabled (active low)
  io.sram.ce := True // Disabled (active low)
  io.sram.oe := True // Disabled (active low)
  io.sram.data.writeEnable := False
  io.sram.data.write := B"8'x00"
  io.sram.addr := U"0".asBits.resized
  io.bus.DTACK := True // Disabled (active low)

  val dataBuf = Reg(Bits(16 bits)) init(0)
  io.bus.DATAI := dataBuf

  val fsm = new StateMachine {
    val idle: State = new State with EntryPoint {
      whenIsActive {
        when(!io.bus.AS && io.sel) {
          when(io.bus.RW) {
            goto(readLow)
          } otherwise {
            goto(writeLow)
          }
        }
      }
    }

    // -- READ SEQUENCE --
    // TODO: the read introduces a lot a latency that
    //  is actually not necessary considering the RAM
    //  can be as fast as 10 ns. Create an additional
    //  clock at 64 MHz only for the RAM, it'll require
    //  BufferCC for the signals (cross domain buffers)
    val readLow: State = new State {
      whenIsActive {
        io.sram.ce := False // active
        io.sram.oe := False // active
        io.sram.addr := io.bus.ADDR.asBits(18 downto 1) ##  B"1" // LDS address (odd address)
        goto(sampleLow)
      }
    }

    val sampleLow: State = new State {
      whenIsActive {
        dataBuf(7 downto 0) := io.sram.data.read
        goto(readHigh)
      }
    }

    val readHigh: State = new State {
      whenIsActive {
        io.sram.ce := False // active
        io.sram.oe := False // active
        io.sram.addr := io.bus.ADDR.asBits(18 downto 1) ##  B"0" // UDS address (even address)

        goto(sampleHigh)
      }
    }

    val sampleHigh: State = new State {
      whenIsActive {
        dataBuf(15 downto 8) := io.sram.data.read
        io.bus.DTACK := False
        goto(waitCpu)
      }
    }

    // -- WRITE SEQUENCE --
    val writeLow: State = new State {
      whenIsActive {
        when(!io.bus.LDS) {
          io.sram.ce := False
          io.sram.we := False
          io.sram.addr := io.bus.ADDR(18 downto 1) ## B"1" // LDS address (odd address)
          io.sram.data.writeEnable := True
          io.sram.data.write := io.bus.DATAO(7 downto 0)
        }
        goto(writeHigh)
      }
    }

    val writeHigh: State = new State {
      whenIsActive {
        when(!io.bus.UDS) {
          io.sram.ce := False
          io.sram.we := False
          io.sram.addr := io.bus.ADDR(18 downto 1) ## B"0" // UDS address (odd address)
          io.sram.data.writeEnable := True
          io.sram.data.write := io.bus.DATAO(15 downto 8)
        }
        io.bus.DTACK := False
        goto(waitCpu)
      }
    }

    // -- SHARE STATE --
    val waitCpu: State = new State {
      whenIsActive {
        io.bus.DTACK := False
        when(io.bus.AS) {
          goto(idle)
        }
      }
    }
  }
}
