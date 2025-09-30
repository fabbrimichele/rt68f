package rt68f

import spinal.core._
import spinal.lib.com.uart._
import spinal.lib._

import scala.language.postfixOps

/**
 * Hardware definition
 * @param romFilename name of the file containing the ROM content
 */
//noinspection TypeAnnotation
case class Rt68fTopLevel(romFilename: String) extends Component {
  val io = new Bundle {
    val clk = in Bool()
    val reset = in Bool()
    val led = out Bits(4 bits)
  }

  io.led := B("0001")

  // Remove io_ prefix
  noIoPrefix()
}

object Rt68fTopLevelVhdl extends App {
  //private val romFilename = "keys.hex"
  private val romFilename = "blinker.hex"
  private val report = Config.spinal.generateVhdl(Rt68fTopLevel(romFilename))
  report.mergeRTLSource("mergeRTL") // Merge all rtl sources into mergeRTL.vhd and mergeRTL.v files
  report.printPruned()
}
