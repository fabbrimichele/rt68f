package rt68f.memory

import rt68f.core.M68kBus
import spinal.core.in.Bool
import spinal.core.{Bits, Bundle, Cat, Component, False, IntToBuilder, LiteralBuilder, True, in, out, when}
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

  io.sram.addr := io.bus.ADDR.asBits(18 downto 0)

  // Default state
  io.sram.we := True // Disabled (active low)
  io.sram.ce := True // Disabled (active low)
  io.sram.oe := True // Disabled (active low)
  io.bus.DTACK := True // Disabled (active low)
  io.bus.DATAI := B"16'x0000"
  io.sram.data.writeEnable := False
  io.sram.data.write := B"8'x00"

  // TODO:
  //  Implement 16 bits interface.
  //  Initial implementation, only 8 bits at a time.
  when(!io.bus.AS && io.sel) {
    io.bus.DTACK := False // active
    io.sram.ce := False // active

    when(io.bus.RW) {
      // Read
      io.sram.oe := False // active
      when (!io.bus.LDS) {
        // B"8'xFF"`
        io.bus.DATAI := Cat(B"8'x00", io.sram.data.read)
      } elsewhen(!io.bus.UDS) {
        io.bus.DATAI := Cat(io.sram.data.read, B"8'x00")
      } elsewhen(!io.bus.LDS && !io.bus.UDS) {
        // TODO
      }
    } otherwise {
      // Write
      io.sram.we := False // active
      when (!io.bus.LDS) {
        io.sram.data.writeEnable := True
        io.sram.data.write := io.bus.DATAO(7 downto 0)
      } elsewhen(!io.bus.UDS) {
        io.sram.data.writeEnable := True
        io.sram.data.write := io.bus.DATAO(15 downto 8)
      } elsewhen(!io.bus.LDS && !io.bus.UDS) {
        // TODO
      }
    }
  }
}
