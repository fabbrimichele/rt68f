package rt68f.io

import rt68f.core.M68kBus
import spinal.core._
import spinal.lib.com.uart._
import spinal.lib.com.uart.UartParityType.NONE
import spinal.lib.com.uart.UartStopType.ONE
import spinal.lib._

import scala.language.postfixOps

case class UartDevice() extends Component {
  val io = new Bundle {
    val bus = slave(M68kBus())
    val sel = in Bool() // chip select from decoder
    val uart = master(Uart())
  }

  val uartCtrl = UartCtrl(
    config = UartCtrlInitConfig(
      baudrate = 9600,
      dataLength = 7,
      parity = NONE,
      stop = ONE
    )
  )
  io.uart <> uartCtrl.io.uart


  // Registers to hold the byte to send
  val txReg   = Reg(Bits(8 bits)) init 0
  val txValid = Reg(Bool()) init False

  // Default bus signals
  io.bus.DATAI := 0     // default
  io.bus.DTACK := True  // inactive (assuming active low)

  when(!io.bus.AS && io.sel) {
    io.bus.DTACK := False // acknowledge access (active low)

    when(io.bus.RW) {
      // Read
      //io.bus.DATAI := dataReg
    } otherwise {
      // Write
      txReg := io.bus.DATAO(7 downto 0) // lower byte
      txValid := True
    }
  }

  // Connect to UART stream
  uartCtrl.io.write.payload := txReg
  uartCtrl.io.write.valid   := txValid

  // Clear valid after UART accepted it
  when(uartCtrl.io.write.fire) {
    txValid := False
  }

  /*
  // See: https://spinalhdl.github.io/SpinalDoc-RTD/master/SpinalHDL/Examples/Intermediates%20ones/uart.html#example-with-test-bench
  val producer = Stream(Bits(8 bits))
  producer.valid := CounterFreeRun(2000).willOverflow
  producer.payload := B('a',8 bits)

  // Without fifo, it doesn't work
  val fifo = StreamFifo(Bits(8 bits), depth = 4)
  producer >> fifo.io.push
  fifo.io.pop >> uartCtrl.io.write
   */
}
