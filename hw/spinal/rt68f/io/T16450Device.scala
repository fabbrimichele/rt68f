package rt68f.io

import spinal.core._
import spinal.lib._
import rt68f.core.M68kBus
import spinal.lib.com.uart.Uart

import scala.language.postfixOps

case class T16450Device() extends Component {
  val io = new Bundle {
    val bus = slave(M68kBus())
    val sel = in Bool() // chip select from decoder
    val int = out Bool() // UART interrupt
    val uart = master(Uart())
  }

  val uart = new T16450BB()

  val uartSel = !io.bus.AS && io.sel

  // Default Settings (Copied from MG68)
  uart.io.RClk := uart.io.BaudOut
  uart.io.CS_n := !uartSel
  uart.io.Rd_n := !io.bus.RW
  uart.io.Wr_n := io.bus.RW
  uart.io.A := io.bus.ADDR(3 downto 1).asBits
  uart.io.D_In := io.bus.DATAO(7 downto 0)
  io.bus.DATAI := uart.io.D_Out.resized
  uart.io.SIn := io.uart.rxd
  uart.io.CTS_n := False
  uart.io.DSR_n := False
  uart.io.RI_n := False
  uart.io.DCD_n := False
  io.uart.txd := uart.io.SOut
  io.int := uart.io.Intr

  io.bus.DTACK := True  // inactive (assuming active low)
  when(uartSel) {
    io.bus.DTACK := False // acknowledge access (active low)
  }
}
