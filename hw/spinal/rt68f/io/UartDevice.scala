package rt68f.io

import rt68f.core.M68kBus
import spinal.core._
import spinal.lib.com.uart._
import spinal.lib.com.uart.UartParityType.NONE
import spinal.lib.com.uart.UartStopType.ONE
import spinal.lib.StreamFifo // CORRECTED: StreamFifo is usually a direct member of spinal.lib
import spinal.lib._

import scala.language.postfixOps

case class UartDevice() extends Component {
  val io = new Bundle {
    val bus = slave(M68kBus())
    val sel = in Bool() // chip select from decoder
    val uart = master(Uart())
  }

  // 1. Uart Controller Setup
  val uartCtrl = UartCtrl(
    config = UartCtrlInitConfig(
      baudrate = 9600,
      dataLength = 7,
      parity = NONE,
      stop = ONE
    )
  )
  // Connect the UART physical pins
  io.uart <> uartCtrl.io.uart

  // 2. FIFO Buffer (8 entries deep)
  // This allows the M68k to write up to 8 characters rapidly without blocking
  val txFifo = StreamFifo(Bits(8 bits), 8)

  // 3. Connect FIFO output to UART Controller input
  // The FIFO's master stream drives the UartCtrl's write stream
  uartCtrl.io.write <> txFifo.io.pop

  // 4. Status Register Logic (Simplified)
  // The peripheral is ready if the FIFO is ready to accept a push (i.e., NOT full).
  val txReady = txFifo.io.push.ready

  // Select which register is being accessed
  // 0b00 = Data register
  // 0b10 = Status/Ctrl register
  val regSel = io.bus.ADDR(1)

  // Status register
  val statusReg = Reg(Bits(8 bits)) init 0
  // Bit 0: TX ready (1 = Ready to accept new byte)
  // The status is driven by the FIFO's push readiness
  statusReg(0) := txReady

  // Default bus signals
  io.bus.DATAI := 0     // default
  io.bus.DTACK := True  // inactive (assuming active low)


  // 5. M68k Bus Interface Logic (Writing to FIFO)

  // --- START: SINGLE-PULSE WRITE LOGIC ---
  // Tracks if the current bus cycle has already been accepted by the FIFO.
  val txAccepted = RegInit(False)

  // Reset acceptance flag when the M68k bus cycle ends (AS goes high)
  when(io.bus.AS) {
    txAccepted := False
  }

  // Determine if the M68k is requesting a write to the Data Register
  val isTxWrite = !io.bus.AS && io.sel && !io.bus.RW && !regSel

  // txPushValid is only True if:
  // 1. The M68k is requesting a write (isTxWrite is True)
  // 2. We have NOT yet accepted the data for this bus cycle (txAccepted is False)
  val txPushValid = isTxWrite && !txAccepted

  // Drive the FIFO push stream
  txFifo.io.push.valid   := txPushValid
  // Payload is combinational, taking the data directly from the bus
  txFifo.io.push.payload := io.bus.DATAO(7 downto 0)

  // Update the acceptance flag when the FIFO handshake succeeds
  when(txFifo.io.push.fire) {
    txAccepted := True
  }
  // --- END: SINGLE-PULSE WRITE LOGIC ---


  // Handle DTACK (data acknowledge) and Read transactions
  when(!io.bus.AS && io.sel) {
    io.bus.DTACK := False // acknowledge access (active low)

    when(io.bus.RW) {
      // Read
      when(regSel) {
        // Status register selected
        io.bus.DATAI := statusReg.resize(16 bits)
      }
    }
    // Write transactions are handled by the single-pulse logic above, no further action needed here.
  }
}
