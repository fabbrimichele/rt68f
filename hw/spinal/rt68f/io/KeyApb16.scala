package rt68f.io

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba3.apb._

import scala.language.postfixOps

case class KeyApb16(width: Int = 4, addressWidth: Int = 12) extends Component {
  val io = new Bundle {
    val apb = slave(Apb3(Apb3Config(addressWidth = addressWidth, dataWidth = 16)))
    val keys = in Bits(width bits)
  }

  val busCtrl = Apb3SlaveFactory(io.apb)
  val reg = busCtrl.createReadOnly(UInt(16 bits), address = 0x0)
  reg := io.keys.resize(16).asUInt
}
