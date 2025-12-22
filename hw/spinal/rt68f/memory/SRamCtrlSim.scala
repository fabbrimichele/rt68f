package rt68f.memory

import rt68f.Config.flagExplicit
import spinal.core._
import spinal.core.sim._
import spinal.sim.GhdlFlags

import scala.language.postfixOps
import scala.language.postfixOps

object SRamCtrlSim extends App {
  val config = SpinalConfig(
    targetDirectory = "hw/gen",
    defaultConfigForClockDomains = ClockDomainConfig(
      resetActiveLevel = HIGH
    ),
    defaultClockDomainFrequency = FixedFrequency(32 MHz),
    onlyStdLogicVectorAtTopLevelIo = false
  )

  val sim = SimConfig
    .withConfig(config)
    .withFstWave
    .withGHDL(GhdlFlags().withElaborationFlags(flagExplicit, "--warn-no-specs"))
    .addSimulatorFlag(flagExplicit) // Something is off, this is required, but it shouldn't

  // FIX: Explicitly define the wrapper class so the compiler and sim
  // can properly map the clock signals.
  class TopWrapper extends Component {
    val cpuClk = in Bool()
    val fastClk = in Bool()
    val reset = in Bool()

    // Map external signals to ClockDomains
    val cpuClockDomain = ClockDomain(cpuClk, reset)
    val fastClockDomain = ClockDomain(fastClk, reset)

    // Instantiate your controller using the fast clock
    val dut = SRamCtrl(fastClockDomain)

    dut.io.simPublic()
  }

  sim.compile(new TopWrapper).doSim { dut =>
    // 1. Setup Clocks using the specific ClockDomains we mapped
    val cpuPeriod = 62500
    val fastPeriod = 15625

    // forkStimulus now targets the domain with actual hardware pins
    dut.cpuClockDomain.forkStimulus(cpuPeriod)
    dut.fastClockDomain.forkStimulus(fastPeriod)

    // 2. Initialize Signals (Accessing via dut.dut)
    dut.dut.io.bus.AS #= true
    dut.dut.io.bus.UDS #= true
    dut.dut.io.bus.LDS #= true
    dut.dut.io.bus.RW #= true
    dut.dut.io.sel #= false
    dut.dut.io.sram.data.read #= 0

    // Reset Sequence (important for FSMs)
    dut.cpuClockDomain.assertReset()
    dut.fastClockDomain.assertReset()
    sleep(cpuPeriod * 2)
    dut.cpuClockDomain.deassertReset()
    dut.fastClockDomain.deassertReset()
    sleep(cpuPeriod * 2)

    /**
     * Verifies a 16-bit Read Cycle
     */
    def doRead(address: Long) {
      println(s"--- Starting Read at 0x${address.toHexString} ---")

      // S0
      dut.cpuClockDomain.waitFallingEdge()
      dut.dut.io.bus.ADDR #= address
      dut.dut.io.bus.RW #= true
      dut.dut.io.sel #= true

      // S2
      dut.cpuClockDomain.waitRisingEdge()
      dut.dut.io.bus.AS #= false
      dut.dut.io.bus.UDS #= false
      dut.dut.io.bus.LDS #= false

      // SRAM Model
      val sramModel = fork {
        // Wait for control signals
        waitUntil(dut.dut.io.sram.ce.toBoolean == false && dut.dut.io.sram.oe.toBoolean == false)

        // Simulating SRAM access time: data available after a bit
        dut.fastClockDomain.waitSampling(1)
        dut.dut.io.sram.data.read #= 0xAA // Low byte

        // Wait for FSM to switch to high byte
        waitUntil(dut.dut.io.sram.addr.toLong % 2 == 0)
        dut.fastClockDomain.waitSampling(1)
        dut.dut.io.sram.data.read #= 0xBB // High byte
      }

      // S4: Sample DTACK
      dut.cpuClockDomain.waitFallingEdge()
      val dtack = dut.dut.io.bus.DTACK.toBoolean
      if(!dtack) {
        println(s"SUCCESS: DTACK low at S4. Data: 0x${dut.dut.io.bus.DATAI.toLong.toHexString}")
      } else {
        //********************************************************
        //********************************************************
        //********************************************************
        //********************************************************
        //********************************************************
        // TODO: it fails here!
        //********************************************************
        //********************************************************
        //********************************************************
        //********************************************************
        println("FAILURE: DTACK was HIGH at S4.")
      }

      // S7: Terminate
      dut.cpuClockDomain.waitFallingEdge()
      dut.dut.io.bus.AS #= true
      dut.dut.io.bus.UDS #= true
      dut.dut.io.bus.LDS #= true
      dut.cpuClockDomain.waitRisingEdge()
      dut.dut.io.sel #= false

      sramModel.terminate()
    }

    // --- EXECUTION ---
    dut.fastClockDomain.waitSampling(10)
    doRead(0x100000L)
    sleep(cpuPeriod * 5)

    println("Simulation finished.")  }
}
