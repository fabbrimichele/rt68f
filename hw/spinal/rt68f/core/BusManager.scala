package rt68f.core

import spinal.core.{B, Component, False, IntToBuilder, True, out, when}
import spinal.lib.experimental.chisel.Bundle
import spinal.lib.{master, slave}

import scala.language.postfixOps

//noinspection TypeAnnotation
case class BusManager() extends Component {
  val io = new Bundle {
    // Master Interface (from CPU)
    val masterBus = slave(M68kBus())

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

  io.romSel            := False
  io.ramSel            := False
  io.vgaFramebufferSel := False
  io.vgaPaletteSel     := False
  io.vgaControlSel     := False
  io.ledDevSel         := False
  io.keyDevSel         := False
  io.uartDevSel        := False
  io.sramSel           := False

  // --------------------------------
  // Address decoding
  // --------------------------------
  // Decoding Chain, ensures that even if an address matches
  // two ranges, only the highest priority one is selected.
  val addr = io.masterBus.ADDR
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
  io.masterBus.DATAI := 0
  io.masterBus.DTACK := True

  when(!io.masterBus.AS) {
    when(io.romSel) {
      io.masterBus.DATAI := io.romBus.DATAI
      io.masterBus.DTACK := io.romBus.DTACK
    } elsewhen (io.ramSel) {
      io.masterBus.DATAI := io.ramBus.DATAI
      io.masterBus.DTACK := io.ramBus.DTACK
    } elsewhen (io.vgaFramebufferSel || io.vgaPaletteSel || io.vgaControlSel) {
      io.masterBus.DATAI := io.vgaBus.DATAI
      io.masterBus.DTACK := io.vgaBus.DTACK
    } elsewhen (io.uartDevSel) {
      io.masterBus.DATAI := io.uartBus.DATAI
      io.masterBus.DTACK := io.uartBus.DTACK
    } elsewhen (io.ledDevSel) {
      io.masterBus.DATAI := io.ledBus.DATAI
      io.masterBus.DTACK := io.ledBus.DTACK
    } elsewhen (io.keyDevSel) {
      io.masterBus.DATAI := io.keyBus.DATAI
      io.masterBus.DTACK := io.keyBus.DTACK
    } elsewhen (io.sramSel) {
      io.masterBus.DATAI := io.sramBus.DATAI
      io.masterBus.DTACK := io.sramBus.DTACK
    } otherwise {
      // Optional: Bus Error / Default Response
      // TODO: I should trigger Bus error or at least an interrupt
      io.masterBus.DATAI := B(0xFFFF, 16 bits)
      io.masterBus.DTACK := False // Generate a fake DTACK so CPU doesn't hang?
    }
  }
}
