package rt68f.io

import spinal.core._
import spinal.lib.com.uart._
import spinal.lib.com.uart.UartParityType.NONE
import spinal.lib.com.uart.UartStopType.ONE
import spinal.lib._

import scala.language.postfixOps

case class UartDevice() extends Component {
  val io = new Bundle {
    val uart = master(Uart())
  }

  // See: https://spinalhdl.github.io/SpinalDoc-RTD/master/SpinalHDL/Examples/Intermediates%20ones/uart.html#example-with-test-bench
  val uartCtrl = UartCtrl(
    config = UartCtrlInitConfig(
      baudrate = 9600,
      dataLength = 7,
      parity = NONE,
      stop = ONE
    )
  )

  io.uart <> uartCtrl.io.uart

  val producer = Stream(Bits(8 bits))
  producer.valid := CounterFreeRun(2000).willOverflow
  producer.payload := B('a',8 bits)


  val fifo = StreamFifo(Bits(8 bits), depth = 4)
  producer >> fifo.io.push
  fifo.io.pop >> uartCtrl.io.write

  /*
  val write = Stream(Bits(8 bits))
  write.valid := CounterFreeRun(2000).willOverflow
  // TODO: implement transmission as first step
  //  1. create a register for data out
  //  2. create a control register (ready to send, etc...)
  //  configuration (baud, stop, etc.) fixed for the time being
  write.payload := B('a', 8 bits)
  write >-> uartCtrl.io.write

   */
}
