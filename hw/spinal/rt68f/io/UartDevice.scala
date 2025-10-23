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

  // Select which register is being accessed
  // 0b00 = Data register
  // 0b10 = Status/Ctrl register
  val regSel = io.bus.ADDR(1)

  // Status register
  val statusReg = Reg(Bits(8 bits)) init 0
  // Flag to track if the CPU has performed the first TX write (used for initial un-sticking)
  val firstTxDone = RegInit(False)

  // Registers to hold the byte to send
  val txReg   = Reg(Bits(8 bits)) init 0
  val txValid = Reg(Bool()) init False

  // Create a synchronized, non-delayed version of the UartCtrl ready signal
  val uartCtrlReadySync = RegNext(uartCtrl.io.write.ready)

  // TODO: I haven't found yet a way to verify that the status register
  //       isn't actually always set to True, I should write a simulation.
  // This signal dictates the final state of statusReg(0)
  val txReadyStatus = Bool()
  // Logic to guarantee the CPU sees TX ready=True at startup:
  when(!firstTxDone) {
    // If we haven't written the first byte, force the status to ready (True)
    txReadyStatus := True
  } otherwise {
    // After the first write, follow the actual synchronized UartCtrl ready signal
    // This removes the *extra* RegNext latency that was causing the polling race.
    txReadyStatus := uartCtrlReadySync
  }

  // Bit 0: TX ready (1 = Ready to accept new byte)
  statusReg(0) := txReadyStatus

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
        // Data register selected
        txReg := io.bus.DATAO(7 downto 0) // lower byte
        txValid := True
      }
    }
  }

  // Connect to UART stream
  uartCtrl.io.write.payload := txReg
  uartCtrl.io.write.valid   := txValid

  // Clear valid after UART accepted it
  when(uartCtrl.io.write.fire) {
    txValid := False
  }
}
