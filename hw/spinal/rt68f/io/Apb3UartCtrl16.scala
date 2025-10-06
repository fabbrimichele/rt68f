package rt68f.io

import spinal.core.{Bundle, Component, out}
import spinal.lib.bus.amba3.apb.{Apb3, Apb3Config, Apb3SlaveFactory}
import spinal.lib.com.uart.{Apb3UartCtrl, Uart, UartCtrl, UartCtrlMemoryMappedConfig}
import spinal.lib.{master, slave}

object Apb3UartCtrl16{
  def getApb3Config = Apb3Config(
    addressWidth = 5,
    dataWidth = 16,
    selWidth = 1,
    useSlaveError = false
  )
}

case class Apb3UartCtrl16(config : UartCtrlMemoryMappedConfig) extends Component{
  val io = new Bundle{
    val apb =  slave(Apb3(Apb3UartCtrl16.getApb3Config))
    val uart = master(Uart(ctsGen = config.uartCtrlConfig.ctsGen, rtsGen = config.uartCtrlConfig.rtsGen))
    val interrupt = out Bool()
  }

  val uartCtrl = new UartCtrl(config.uartCtrlConfig)
  io.uart <> uartCtrl.io.uart

  val busCtrl = Apb3SlaveFactory(io.apb)
  val bridge = uartCtrl.driveFrom16(busCtrl,config)
  io.interrupt := bridge.interruptCtrl.interrupt
}
