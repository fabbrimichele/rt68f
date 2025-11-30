package rt68f

import spinal.core._
import spinal.core.sim._
import spinal.sim.GhdlFlags

object Config {
  def spinal = SpinalConfig(
    targetDirectory = "hw/gen",
    defaultConfigForClockDomains = ClockDomainConfig(
      resetActiveLevel = HIGH
    ),
    defaultClockDomainFrequency = FixedFrequency(32 MHz),
    onlyStdLogicVectorAtTopLevelIo = false
  )

  val flagExplicit = "-fexplicit" // This is required to make GHDL compile TG68.vhd
  def sim = SimConfig
    .withConfig(spinal)
    .withFstWave
    .withGHDL(GhdlFlags().withElaborationFlags(flagExplicit, "--warn-no-specs"))
    .addSimulatorFlag(flagExplicit) // Something is off, this is required, but it shouldn't
    // TODO: find a way to use relative paths
    .addRtl("/home/michele/rt68f/hw/vhdl/TG68.vhd")
    .addRtl("/home/michele/rt68f/hw/vhdl/TG68_fast.vhd")
    .addRtl("/home/michele/rt68f/hw/vhdl/dcm32_25_8.vhd")
}
