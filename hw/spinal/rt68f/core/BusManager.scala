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
    val romBus    = master(M68kBus())
    val vgaBus    = master(M68kBus())
    val uartBus   = master(M68kBus())
    val ledBus    = master(M68kBus())
    val keyBus    = master(M68kBus())
    val sramBus   = master(M68kBus())
    val flashBus  = master(M68kBus())
    val timerABus = master(M68kBus())
    //val timerBBus = master(M68kBus())
    val ps2aBus   = master(M68kBus())
    val ps2bBus   = master(M68kBus())
    val psgBus    = master(M68kBus())

    // Slave select signals (to peripherals)
    val romSel            = out Bool()
    val vgaFramebufferSel = out Bool()
    val vgaPaletteSel     = out Bool()
    val vgaControlSel     = out Bool()
    val ledDevSel         = out Bool()
    val keyDevSel         = out Bool()
    val uartDevSel        = out Bool()
    val sramSel           = out Bool()
    val flashSel          = out Bool()
    val timerASel         = out Bool()
    //val timerBSel         = out Bool()
    val ps2aSel           = out Bool()
    val ps2bSel           = out Bool()
    val psgSel            = out Bool()

    // Interrupts (from peripherals)
    val vgaVSyncInt       = in Bool()
    val uartInt           = in Bool()
    val timerAInt         = in Bool()
    //val timerBInt         = in Bool()
    val ps2aInt           = in Bool()
    val ps2bInt           = in Bool()
  }

  // --------------------------------
  // Broadcast Logic
  // --------------------------------
  val peripheralBuses = List(
    io.romBus, io.vgaBus, io.ledBus,
    io.keyBus, io.uartBus, io.sramBus, io.flashBus,
    io.timerABus, /*io.timerBBus,*/ io.ps2aBus,
    io.ps2bBus, io.psgBus,
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
  // Interrupts
  // --------------------------------
  // Only autovectors are used for interrupts
  // IPL is active low
  // TODO: revisit interrupt priorities
  when(io.ps2bInt) {
    io.ipl := B"001" // bitwise not 6
  } elsewhen(io.ps2aInt) {
    io.ipl := B"010" // bitwise not 5
  } elsewhen(io.uartInt) {
    io.ipl := B"011" // bitwise not 4
  } elsewhen(io.vgaVSyncInt) {
    io.ipl := B"100" // bitwise not 3
  } elsewhen(io.timerAInt) {
    io.ipl := B"101" // bitwise not 2
  /*} elsewhen(io.timerBInt) {
    io.ipl := B"110" // bitwise not 1*/
  } otherwise {
    io.ipl := B"111" // bitwise not 0
  }

  // --------------------------------
  // Address decoding
  // --------------------------------
  io.romSel            := False
  io.vgaFramebufferSel := False
  io.vgaPaletteSel     := False
  io.vgaControlSel     := False
  io.ledDevSel         := False
  io.keyDevSel         := False
  io.uartDevSel        := False
  io.sramSel           := False
  io.flashSel          := False
  io.timerASel         := False
  //io.timerBSel         := False
  io.ps2aSel           := False
  io.ps2bSel           := False
  io.psgSel            := False

  // Decoding Chain, ensures that even if an address matches
  // two ranges, only the highest priority one is selected.
  // TODO: further optimizations should be possible:
  //  - expand device range (plenty of space unused)
  //  - reduce the address bus size further, from 24 to less
  //  - I tried to improve the mapping for video ram and rom without success
  val addr = io.cpuBus.ADDR
  when(addr(31 downto 3) === 0x00000000) { // 0x00000000 < 0x00000008
    // This is required to have Reset SP and PC defined
    // in ROM when the CPU starts, the 2 values are only
    // read during after the reset, there is no point in
    // making them writable.
    io.romSel := True
  } elsewhen(addr(31 downto 19) === 0x00000000) { // 0x00000000 < 0x00080000
    io.sramSel := True
  } elsewhen(addr(31 downto 20) === 0x002) {
    // 64000 bytes, leaves something for the ROM
    io.vgaFramebufferSel := True
  } elsewhen(addr(31 downto 20) === 0x003) {
    // 1122 byte, the memory left from the framebuffer and palette
    io.romSel := True
  } elsewhen(addr(31 downto 16) === 0x0041) {
    io.uartDevSel := True
  } elsewhen(addr(31 downto 16) === 0x0042) {
    io.vgaPaletteSel := True
  } elsewhen(addr(31 downto 16) === 0x0043) {
    io.vgaControlSel := True
  } elsewhen(addr(31 downto 16) === 0x0044) {
    io.flashSel := True
  } elsewhen(addr(31 downto 16) === 0x0045) {
    io.timerASel := True
  /*} elsewhen(addr(31 downto 16) === 0x0046) {
    io.timerBSel := True*/
  } elsewhen(addr(31 downto 16) === 0x0047) {
    io.ps2aSel := True
  } elsewhen(addr(31 downto 16) === 0x0048) {
    io.ps2bSel := True
  } elsewhen(addr(31 downto 16) === 0x0049) {
    io.psgSel := True
  } elsewhen(addr(31 downto 16) === 0x004A) {
    io.ledDevSel := True
  } elsewhen(addr(31 downto 16) === 0x004B) {
    io.keyDevSel := True
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
    } elsewhen (io.timerASel) {
      io.cpuBus.DATAI := io.timerABus.DATAI
      io.cpuBus.DTACK := io.timerABus.DTACK
    /*} elsewhen (io.timerBSel) {
      io.cpuBus.DATAI := io.timerBBus.DATAI
      io.cpuBus.DTACK := io.timerBBus.DTACK*/
    } elsewhen (io.ps2aSel) {
      io.cpuBus.DATAI := io.ps2aBus.DATAI
      io.cpuBus.DTACK := io.ps2aBus.DTACK
    } elsewhen (io.ps2bSel) {
      io.cpuBus.DATAI := io.ps2bBus.DATAI
      io.cpuBus.DTACK := io.ps2bBus.DTACK
    } elsewhen (io.psgSel) {
      io.cpuBus.DATAI := io.psgBus.DATAI
      io.cpuBus.DTACK := io.psgBus.DTACK
    } otherwise {
      // Optional: Bus Error / Default Response
      // TODO: I should trigger Bus error or at least an interrupt
      io.cpuBus.DATAI := B(0xFFFF, 16 bits)
      io.cpuBus.DTACK := False // Generate a fake DTACK so CPU doesn't hang?
    }
  }
}
