package rt68f.core

import spinal.core._
import spinal.lib._

import scala.language.postfixOps

//noinspection TypeAnnotation
case class BusManager() extends Component {
  val io = new Bundle {
    // Master Interface (from CPU)
    val cpuBus = slave(M68kBus())
    val ipl    = out Bits(3 bits)

    // Slave busses (for the Mux)
    val romBus   = master(M68kBus())
    val ramBus   = master(M68kBus())
    val vgaBus   = master(M68kBus())
    val uartBus  = master(M68kBus())
    val ledBus   = master(M68kBus())
    val keyBus   = master(M68kBus())
    val sramBus  = master(M68kBus())
    val flashBus = master(M68kBus())
    val timerBus = master(M68kBus())

    // Slave select signals (to peripherals)
    val romSel            = out Bool()
    val ramSel            = out Bool()
    val vgaFramebufferSel = out Bool()
    val vgaPaletteSel     = out Bool()
    val vgaControlSel     = out Bool()
    val ledDevSel         = out Bool()
    val keyDevSel         = out Bool()
    val uartDevSel        = out Bool()
    val sramSel           = out Bool()
    val flashSel          = out Bool()
    val timerSel          = out Bool()

    // Interrupts (from peripherals)
    val vgaVSyncInt       = in Bool()
    val timerAInt         = in Bool()
    val timerBInt         = in Bool()
  }

  // --------------------------------
  // Broadcast Logic
  // --------------------------------
  val peripheralBuses = List(
    io.romBus, io.ramBus, io.vgaBus, io.ledBus,
    io.keyBus, io.uartBus, io.sramBus, io.flashBus,
    io.timerBus,
  )

  for (bus <- peripheralBuses) {
    bus.AS := io.cpuBus.AS
    bus.UDS := io.cpuBus.UDS
    bus.LDS := io.cpuBus.LDS
    bus.RW := io.cpuBus.RW
    bus.ADDR := io.cpuBus.ADDR
    bus.DATAO := io.cpuBus.DATAO
  }

  // Interrupts
  // Only autovectors are used for interrupts

  // TODO: implement a proper decoder to use all levels
  // IPL is active low
  io.ipl := Cat(!io.timerAInt, !io.timerBInt, !io.vgaVSyncInt)

  // --------------------------------
  // Address decoding
  // --------------------------------
  io.romSel            := False
  io.ramSel            := False
  io.vgaFramebufferSel := False
  io.vgaPaletteSel     := False
  io.vgaControlSel     := False
  io.ledDevSel         := False
  io.keyDevSel         := False
  io.uartDevSel        := False
  io.sramSel           := False
  io.flashSel          := False
  io.timerSel          := False

  // Decoding Chain, ensures that even if an address matches
  // two ranges, only the highest priority one is selected.
  val addr = io.cpuBus.ADDR
  when(addr >= 0x00000000 && addr < 0x00000008) {
    // This is required to have Reset SP and PC defined
    // in ROM when the CPU starts, the 2 values are only
    // read during after the reset, there is no point in
    // making them writable.
    io.romSel := True
  } elsewhen(addr >= 0x00000008 && addr < 0x00080000) {
    io.sramSel := True
  } elsewhen(addr >= 0x00200000 && addr < 0x0020FA00) {
    // 64000 bytes, leaves something for the ROM
    io.vgaFramebufferSel := True
  } elsewhen(addr >= 0x00300000 && addr < 0x00300462) {
    // 1122 byte, the memory left from the framebuffer and palette
    io.romSel := True
  } elsewhen(addr === 0x00400000) {
    io.ledDevSel := True
  } elsewhen(addr === 0x00401000) {
    io.keyDevSel := True
  } elsewhen(addr >= 0x00402000 && addr < 0x00402010) {
    io.uartDevSel := True
  } elsewhen(addr >= 0x00403000 && addr < 0x00403200) {
    io.vgaPaletteSel := True
  } elsewhen(addr === 0x00403200) {
    io.vgaControlSel := True
  } elsewhen(addr >= 0x00404000 && addr < 0x00404008) {
    io.flashSel := True
  } elsewhen(addr >= 0x00405000 && addr < 0x0040500A) {
    io.timerSel := True
  }

  // --------------------------------
  // Bus Multiplexer
  // --------------------------------
  io.cpuBus.DATAI := 0
  io.cpuBus.DTACK := True

  when(!io.cpuBus.AS) {
    when(io.romSel) {
      io.cpuBus.DATAI := io.romBus.DATAI
      io.cpuBus.DTACK := io.romBus.DTACK
    } elsewhen (io.ramSel) {
      io.cpuBus.DATAI := io.ramBus.DATAI
      io.cpuBus.DTACK := io.ramBus.DTACK
    } elsewhen (io.vgaFramebufferSel || io.vgaPaletteSel || io.vgaControlSel) {
      io.cpuBus.DATAI := io.vgaBus.DATAI
      io.cpuBus.DTACK := io.vgaBus.DTACK
    } elsewhen (io.uartDevSel) {
      io.cpuBus.DATAI := io.uartBus.DATAI
      io.cpuBus.DTACK := io.uartBus.DTACK
    } elsewhen (io.ledDevSel) {
      io.cpuBus.DATAI := io.ledBus.DATAI
      io.cpuBus.DTACK := io.ledBus.DTACK
    } elsewhen (io.keyDevSel) {
      io.cpuBus.DATAI := io.keyBus.DATAI
      io.cpuBus.DTACK := io.keyBus.DTACK
    } elsewhen (io.sramSel) {
      io.cpuBus.DATAI := io.sramBus.DATAI
      io.cpuBus.DTACK := io.sramBus.DTACK
    } elsewhen (io.flashSel) {
      io.cpuBus.DATAI := io.flashBus.DATAI
      io.cpuBus.DTACK := io.flashBus.DTACK
    } elsewhen (io.timerSel) {
      io.cpuBus.DATAI := io.timerBus.DATAI
      io.cpuBus.DTACK := io.timerBus.DTACK
    } otherwise {
      // Optional: Bus Error / Default Response
      // TODO: I should trigger Bus error or at least an interrupt
      io.cpuBus.DATAI := B(0xFFFF, 16 bits)
      io.cpuBus.DTACK := False // Generate a fake DTACK so CPU doesn't hang?
    }
  }
}
