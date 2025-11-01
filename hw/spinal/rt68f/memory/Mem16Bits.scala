package rt68f.memory

import rt68f.core.M68kBus
import spinal.core._
import spinal.lib._

import scala.io.Source
import scala.language.postfixOps
import scala.util.Using

/**
 * ROM component with 16-bit words.
 *
 * @param size     The size of the ROM in 16-bit words.
 * @param initFile The path to the ROM content file. Each line should contain one word in hexadecimal format.
 *                 The file is read from the **classpath**, so it should be placed under
 *                 `src/main/resources/ao68000/hw/spinal/ao68000/memory/`.
 */
case class Mem16Bits(size: Int, readOnly: Boolean = false, initFile: Option[String] = None) extends Component {
  val io = new Bundle {
    val bus   = slave(M68kBus())
    val sel   = in Bool() // chip select from decoder
  }

  val mem = Mem(Bits(16 bits), size)
  initFile.foreach { filename => mem.init(readContentFromFile(filename)) }

  // Default response
  io.bus.DATAI := 0
  io.bus.DTACK := True

  when(!io.bus.AS && io.sel) {
    io.bus.DTACK := False // active
    val wordAddr = io.bus.ADDR(log2Up(size) downto 1)

    when(io.bus.RW) {
      // ------------------------------------
      // Read Access (Byte strobes are ignored by the memory block)
      // ------------------------------------
      // NOTE: mem.readSync is fine; the M68k core internally selects D15-D8 or D7-D0 based on UDS/LDS.
      io.bus.DATAI := mem.readSync(wordAddr)
    } otherwise  {
      if (!readOnly) {
        // ------------------------------------
        // Write Access (Byte strobes MUST be managed)
        // ------------------------------------
        // io.bus.UDS (D15-D8) -> mask(1)
        // io.bus.LDS (D7-D0) -> mask(0)
        val byteMask = Cat(!io.bus.UDS, !io.bus.LDS).asBits // The 2-bit byte write enable mask

        // Use writeMixedWidth to enable byte-level writing
        mem.writeMixedWidth(
          address = wordAddr,
          data    = io.bus.DATAO,
          mask    = byteMask
        )
      }
    }
  }

  private def readContentFromFile(initFile: String) = {
    val romContent = Using.resource(getClass.getResourceAsStream(initFile)) { stream =>
      val source = Source.fromInputStream(stream)
      try {
        source.getLines()
          .map { line => B(java.lang.Long.parseLong(line.trim, 16), 16 bits) }
          .toSeq
      } finally source.close()
    }
    assert(romContent.size <= size, s"ROM content file greater than $size")
    romContent ++ Seq.fill(size - romContent.size)(B(0, 16 bits))
  }
}

