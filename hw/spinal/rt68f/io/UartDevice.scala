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

  // ------------------------
  // 1. Uart Controller Setup
  // ------------------------
  val uartCtrl = UartCtrl(
    config = UartCtrlInitConfig(
      baudrate = 9600,
      dataLength = 7,
      parity = NONE,
      stop = ONE
    )
  )
  io.uart <> uartCtrl.io.uart

  // ------------------------
  // 2. FIFO Buffer (8 entries deep)
  // ------------------------
  val txFifo = StreamFifo(
    dataType = Bits(8 bits),
    depth = 8
  )

  // Default the FIFO push input to ensure non-write cycles don't trigger anything
  txFifo.io.push.valid   := False
  txFifo.io.push.payload := 0

  // Connect FIFO output to UART Controller input
  uartCtrl.io.write <> txFifo.io.pop

  // ------------------------
  // 3. Status Register Logic
  // ------------------------
  // Bit 0 = TX ready (FIFO not full)
  val txReady = txFifo.io.push.ready
  val statusReg = Reg(Bits(8 bits))
  statusReg(0) := txReady
  statusReg(7 downto 1) := 0

  // ------------------------
  // 4. Bus Interface
  // ------------------------
  // 0b00 = Data register
  // 0b10 = Status/Ctrl register
  val regSel = io.bus.ADDR(1)

  // Default bus signals
  io.bus.DATAI := 0     // default
  io.bus.DTACK := True  // inactive (assuming active low)

  when(!io.bus.AS && io.sel) {
    io.bus.DTACK := False // acknowledge access (active low)

    when(io.bus.RW) {
      // Read
      when(regSel) {
        // Status register selected
        io.bus.DATAI := statusReg.resize(16 bits)
      }
    } otherwise {
      // Write
      when(!regSel) {
        // Data register selected: Write into the FIFO
        txFifo.io.push.valid   := True
        txFifo.io.push.payload := io.bus.DATAO(7 downto 0) // lower byte
      }
    }
  }
}
