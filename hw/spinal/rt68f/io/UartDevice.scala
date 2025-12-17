package rt68f.io

import rt68f.core.M68kBus
import spinal.core._
import spinal.lib.com.uart._
import spinal.lib.com.uart.UartParityType.NONE
import spinal.lib.com.uart.UartStopType.ONE
import spinal.lib.StreamFifo // CORRECTED: StreamFifo is usually a direct member of spinal.lib
import spinal.lib._

import scala.language.postfixOps

// TODO: proper UDS/LDS handling?
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

  // 2a. RX FIFO Buffer (8 entries deep)
  // Stores received data until the M68k reads it.
  val rxFifo = StreamFifo(Bits(8 bits), 8)

  // 3. Connect FIFO output to UART Controller input
  // The FIFO's master stream drives the UartCtrl's write stream
  uartCtrl.io.write <> txFifo.io.pop

  // 3a. Connect UART Controller output to RX FIFO input
  // The UartCtrl's read stream (received data) drives the Rx FIFO's push stream
  rxFifo.io.push <> uartCtrl.io.read

  // 4. Status Register Logic (Simplified)
  val txReady = txFifo.io.push.ready // M68k can write (TX FIFO not full)
  val rxValid = rxFifo.io.pop.valid  // M68k can read (RX FIFO has data)

  // Select which register is being accessed
  // 0b00 = Data register
  // 0b10 = Status/Ctrl register
  val regSel = io.bus.ADDR(1)

  // Status register
  val statusReg = Reg(Bits(8 bits)) init 0
  // Bit 0: TX ready (1 = Ready to accept new byte)
  statusReg(0) := txReady
  // Bit 1: RX ready (1 = Data available for M68k to read)
  statusReg(1) := rxValid

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

  // Define the exact condition for the M68k to read the Data Register AND for the FIFO to have data.
  // This signal asserts the rxFifo's 'ready' and consumes the payload.
  val isRxReadFire = !io.bus.AS && io.sel && io.bus.RW && !regSel && rxValid
  rxFifo.io.pop.ready := isRxReadFire // Acknowledge the pop

  // TODO: try to delay DTACK with a state machine?

  // Handle DTACK (data acknowledge) and Read transactions
  when(!io.bus.AS && io.sel) {
    io.bus.DTACK := False // acknowledge access (active low)

    when(io.bus.RW) {
      // Read
      when(regSel) {
        // Status register selected
        io.bus.DATAI := statusReg.resize(16 bits)
      } otherwise {
        // Data register selected (0b00) - Read from Rx FIFO
        // The payload is driven combinatorially regardless of 'valid'.
        // The FIFO ensures the 'ready' signal is only asserted when valid is true.
        io.bus.DATAI := rxFifo.io.pop.payload.resize(16 bits)
      }
    }
    // Write transactions are handled by the single-pulse logic above, no further action needed here.
  }
}
