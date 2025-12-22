package rt68f.core

import spinal.core.{Bundle, ClockDomain, ClockDomainConfig, Component, DoubleToBuilder, FixedFrequency, IntToBuilder, SYNC, in}
import spinal.lib.BufferCC

import scala.language.postfixOps

/**
 * - Derived clocks needs to be synchronized, in particular the 16 MHz and 64 Mhz.
 * - Needs to be inside the 32MHz clock, that is the input for `Clk_32_64_25_16`
 */
case class ClockManager() extends Component {
  private val clk = new Clk_32_64_25_16()

  val clk16 = ClockDomain(
    clock = clk.io.CLK_OUT16,
    reset = !clk.io.LOCKED,
    frequency = FixedFrequency(16 MHz),
  )

  val clk25 = ClockDomain(
    clock = clk.io.CLK_OUT25,
    reset = !clk.io.LOCKED,
    frequency = FixedFrequency(25.143 MHz),
  )

  val clk64 = ClockDomain(
    clock = clk.io.CLK_OUT64,
    reset = !clk.io.LOCKED,
    frequency = FixedFrequency(16 MHz),
  )
}
