package rt68f.core

import rt68f.Config
import spinal.core._
import spinal.core.sim._

import scala.language.postfixOps

object M68kToApb3Bridge16Sim extends App {
  Config.sim
    .compile {
      val dut = M68kToApb3Bridge16(addrWidth = 32)
      dut
    }
    .doSim { dut =>
      dut.clockDomain.forkStimulus(31.25 ns)
      dut.clockDomain.waitRisingEdge()
      dut.clockDomain.waitRisingEdge()
      dut.clockDomain.waitRisingEdge()

      // ----------------------------
      // Simulate write 68000 -> APB3
      // ----------------------------

      // ----------------------------
      // Cycle 0
      // ----------------------------
      var cycle = 0
      dut.io.m68k.RW #= true // Not ready yet
      dut.io.m68k.ADDR #= 1
      dut.io.m68k.DATAO #= 5
      dut.io.m68k.LDS #= true
      dut.io.m68k.UDS #= true
      dut.io.apb.PREADY #= false
      dut.clockDomain.waitRisingEdge()
      printStatus(cycle, dut)
      assert(!dut.io.apb.PENABLE.toBoolean, "Expected PENABLE to be deasserted (false)")
      assert(dut.io.m68k.DTACK.toBoolean, "Expected DTACK  to be de-asserted (true)")

      // ----------------------------
      // Cycle 1 - APB3 Setup phase
      // ----------------------------
      cycle += 1
      dut.io.m68k.AS #= false // AS asserted
      dut.io.m68k.RW #= false // Write
      dut.clockDomain.waitRisingEdge()
      printStatus(cycle, dut)
      assert(dut.io.m68k.DTACK.toBoolean, "Expected DTACK  to be de-asserted (true)")

      // ----------------------------
      // Cycle 2
      // ----------------------------
      cycle += 1
      dut.clockDomain.waitRisingEdge()
      printStatus(cycle, dut)
      assert(dut.io.apb.PWRITE.toBoolean, "Expected PWRITE  to be asserted (true)")
      assert(dut.io.apb.PSEL.toLong == 1, "Expected PSEL to be 1")
      assert(dut.io.apb.PWDATA.toLong == 5, "Expected PWDATA to be 5")
      assert(dut.io.apb.PADDR.toLong == 1, "Expected PADDR to be 1")
      assert(dut.io.m68k.DTACK.toBoolean, "Expected DTACK  to be de-asserted (true)")

      // ----------------------------
      // Cycle 3
      // ----------------------------
      cycle += 1
      dut.clockDomain.waitRisingEdge()
      printStatus(cycle, dut)
      assert(dut.io.m68k.DTACK.toBoolean, "Expected DTACK  to be de-asserted (true)")

      // ----------------------------
      // Cycle 4 - APB3 Access phase
      // ----------------------------
      dut.io.apb.PREADY #= true
      cycle += 1
      dut.clockDomain.waitRisingEdge()
      printStatus(cycle, dut)
      assert(dut.io.apb.PENABLE.toBoolean, "Expected PENABLE  to be asserted (true)")
      assert(dut.io.apb.PWRITE.toBoolean, "Expected PWRITE  to be asserted (true)")
      assert(dut.io.apb.PSEL.toLong == 1, "Expected PSEL  to be 1")
      assert(dut.io.m68k.DTACK.toBoolean, "Expected DTACK  to be de-asserted (true)")

      // ----------------------------
      // Cycle 5 - DTACK asserted for 1 cycle
      // ----------------------------
      cycle += 1
      dut.clockDomain.waitRisingEdge()
      printStatus(cycle, dut)
      assert(dut.io.apb.PENABLE.toBoolean, "Expected PENABLE  to be asserted (true)")
      assert(!dut.io.m68k.DTACK.toBoolean, "Expected DTACK  to be asserted (false)")

      // ----------------------------
      // Cycle 6 - All control signals de-asserted
      // ----------------------------
      cycle += 1
      dut.clockDomain.waitRisingEdge()
      printStatus(cycle, dut)
      assert(dut.io.apb.PSEL.toLong == 0, "Expected PSEL  to be asserted (true)")
      assert(!dut.io.apb.PENABLE.toBoolean, "Expected PENABLE  to be asserted (true)")
      assert(dut.io.m68k.DTACK.toBoolean, "Expected DTACK  to be de-asserted (true)")
    }

    def printStatus(cycle: Int, dut: M68kToApb3Bridge16): Unit = {
      val apb = dut.io.apb
      val m68k = dut.io.m68k
      println(
        s"cycle: $cycle"
          + s"|PENABLE: ${apb.PENABLE.toBoolean}"
          + s"|PWRITE: ${apb.PWRITE.toBoolean}"
          + s"|PSEL: ${apb.PSEL.toLong}"
          + s"|PREADY: ${apb.PREADY.toBoolean}"
          + s"|PADDR: ${apb.PADDR.toLong}"
          + s"|PWDATA: ${apb.PWDATA.toLong}"
          + s"|AS: ${m68k.AS.toBoolean}"
          + s"|RW: ${m68k.RW.toBoolean}"
          + s"|DTACK: ${m68k.DTACK.toBoolean}"
      )
    }
}
