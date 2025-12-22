package rt68f.core

import spinal.core.{Bundle, ClockDomain, ClockDomainConfig, Component, DoubleToBuilder, FixedFrequency, IntToBuilder, SYNC, in}
import spinal.lib.{BufferCC, ResetCtrl}

import scala.language.postfixOps

/**
 * Manages all clocks and reset.
 * See: https://spinalhdl.github.io/SpinalDoc-RTD/master/SpinalHDL/Examples/Simple%20ones/pll_resetctrl.html
 * Derived clocks needs to be synchronized, in particular the 16 MHz and 64 Mhz.
 */
case class ClockCtrl() extends Component {
  val io = new Bundle {
    val clkIn = in Bool()
    val reset = in Bool()
  }

  // Instantiate and drive the PLL
  private val pll = new Clk_32_64_25_16
  pll.io.CLK_IN32 := io.clkIn
  pll.io.RESET := io.reset

  val clk16 = ClockDomain.internal(
    name = "clk16mhz",
    frequency = FixedFrequency(16 MHz),
  )
  clk16.clock := pll.io.CLK_OUT16
  clk16.reset := ResetCtrl.asyncAssertSyncDeassert(
    input = io.reset || !pll.io.LOCKED,
    clockDomain = clk16
  )

  val clk25 = ClockDomain.internal(
    name = "clk25mhz",
    frequency = FixedFrequency(25.143 MHz),
  )
  clk25.clock := pll.io.CLK_OUT25
  clk25.reset := ResetCtrl.asyncAssertSyncDeassert(
    input = io.reset || !pll.io.LOCKED,
    clockDomain = clk25
  )

  val clk64 = ClockDomain.internal(
    name = "clk64mhz",
    frequency = FixedFrequency(64 MHz),
  )
  clk64.clock := pll.io.CLK_OUT64
  clk64.reset := ResetCtrl.asyncAssertSyncDeassert(
    input = io.reset || !pll.io.LOCKED,
    clockDomain = clk64
  )
}
