package rt68f.core

import spinal.core.{B, Component, False, IntToBuilder, True, out, when}
import spinal.lib.experimental.chisel.Bundle
import spinal.lib.{master, slave}

import scala.language.postfixOps

//noinspection TypeAnnotation
case class BusManager() extends Component {
  val io = new Bundle {
    // Master Interface (from CPU)
    val cpuBus = slave(M68kBus())

    // Slave busses (for the Mux)
    val romBus  = master(M68kBus())
    val ramBus  = master(M68kBus())
    val vgaBus  = master(M68kBus())
    val uartBus = master(M68kBus())
    val ledBus  = master(M68kBus())
    val keyBus  = master(M68kBus())
    val sramBus = master(M68kBus())

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
  }

  // --------------------------------
  // Broadcast Logic
  // --------------------------------
  val peripheralBuses = List(
    io.romBus, io.ramBus, io.vgaBus, io.ledBus,
    io.keyBus, io.uartBus, io.sramBus
  )

  for (bus <- peripheralBuses) {
    bus.AS := io.cpuBus.AS
    bus.UDS := io.cpuBus.UDS
    bus.LDS := io.cpuBus.LDS
    bus.RW := io.cpuBus.RW
    bus.ADDR := io.cpuBus.ADDR
    bus.DATAO := io.cpuBus.DATAO
  }


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

  // Decoding Chain, ensures that even if an address matches
  // two ranges, only the highest priority one is selected.
  val addr = io.cpuBus.ADDR
  when(addr >= 0x00000 && addr < 0x04000) {
    io.romSel := True
  } elsewhen(addr >= 0x04000 && addr < 0x08000) {
    io.ramSel := True
  } elsewhen(addr >= 0x08000 && addr < 0x10000) {
    io.vgaFramebufferSel := True
  } elsewhen(addr === 0x10000) {
    io.ledDevSel := True
  } elsewhen(addr === 0x11000) {
    io.keyDevSel := True
  } elsewhen(addr >= 0x12000 && addr < 0x12010) {
    io.uartDevSel := True
  } elsewhen(addr >= 0x13000 && addr < 0x13020) {
    io.vgaPaletteSel := True
  } elsewhen(addr === 0x13100) {
    io.vgaControlSel := True
  } elsewhen(addr >= 0x100000 && addr < 0x180000) {
    io.sramSel := True
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
    } otherwise {
      // Optional: Bus Error / Default Response
      // TODO: I should trigger Bus error or at least an interrupt
      io.cpuBus.DATAI := B(0xFFFF, 16 bits)
      io.cpuBus.DTACK := False // Generate a fake DTACK so CPU doesn't hang?
    }
  }
}
