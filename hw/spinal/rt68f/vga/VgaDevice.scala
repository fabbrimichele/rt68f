package rt68f.vga

import rt68f.vga.VgaDevice.rgbConfig
import spinal.core.{Bundle, ClockDomain, ClockingArea, Component, False, True, when}
import spinal.lib.graphic.RgbConfig
import spinal.lib.graphic.vga.{Vga, VgaCtrl}
import spinal.lib.master

object VgaDevice {
  val rgbConfig = RgbConfig(4, 4, 4)
}

case class VgaDevice() extends Component {
  val io = new Bundle {
    val vga = master(Vga(VgaDevice.rgbConfig))
  }

  // TODO: add memory and make it visible to the top level as memory
  //       this will make the implementation simpler, even though it
  //       relies on FPGA memory (later I can think how to integrate
  //       it with hardware SRAM).

  val dcm = new Dcm25MhzBB()

  val pixelClock = ClockDomain(
    clock = dcm.io.clk25,
    reset = ~dcm.io.locked
  )

  // Clock domain area for VGA timing logic
  new ClockingArea(pixelClock) {
    val ctrl = new VgaCtrl(rgbConfig)
    ctrl.io.softReset := False
    ctrl.io.timings.setAs_h640_v480_r60
    ctrl.io.pixels.valid := True

    ctrl.io.pixels.r := 0
    ctrl.io.pixels.g := 0
    ctrl.io.pixels.b := 0

    when(ctrl.io.vga.colorEn) {
      ctrl.io.pixels.r := 15
      ctrl.io.pixels.g := 15
      ctrl.io.pixels.b := 0
    }

    ctrl.io.vga <> io.vga
  }
}
