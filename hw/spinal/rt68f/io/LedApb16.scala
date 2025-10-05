package rt68f.io

import spinal.core._
import spinal.lib.bus.amba3.apb._
import spinal.lib._

import scala.language.postfixOps

/** Simple 16-bit APB LED peripheral (inline to match 16-bit APB) */
case class LedApb16(width: Int = 4, addressWidth: Int = 12) extends Component {
  val io = new Bundle {
    val apb = slave(Apb3(Apb3Config(addressWidth = addressWidth, dataWidth = 16)))
    val leds = out Bits(width bits)
  }

  val busCtrl = Apb3SlaveFactory(io.apb)
  // create a 16-bit register and use lower 'width' bits for leds
  val reg = busCtrl.createReadAndWrite(UInt(16 bits), address = 0x0)
  io.leds := reg.asBits.resize(width)
}