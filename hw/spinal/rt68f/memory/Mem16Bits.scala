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
      // Read
      io.bus.DATAI := mem.readSync(wordAddr)
    } otherwise  {
      if (!readOnly) {
        // Write if not read only
        mem.write(wordAddr, io.bus.DATAO)
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
    romContent ++ Seq.fill(1024 - romContent.size)(B(0, 16 bits))
  }
}

