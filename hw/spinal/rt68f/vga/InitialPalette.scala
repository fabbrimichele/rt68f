package rt68f.vga

import spinal.core.{B, Bits, IntToBuilder, LiteralBuilder}

import scala.language.postfixOps

object InitialPalette {
  private val paletteSize = 256

  // 1. Standard 16 EGA/CGA colors
  private val egaColors: Seq[Bits] = Seq(
    B"12'x000", // Black
    B"12'x00A", // Blue
    B"12'x0A0", // Green
    B"12'x0AA", // Cyan
    B"12'xA00", // Red
    B"12'xA0A", // Magenta
    B"12'xA50", // Brown (Special Case: R=A, G=5, B=0)
    B"12'xAAA", // Light Gray
    B"12'x555", // Dark Gray
    B"12'x55F", // Bright Blue
    B"12'x5F5", // Bright Green
    B"12'x5FF", // Bright Cyan
    B"12'xF55", // Bright Red
    B"12'xF5F", // Bright Magenta
    B"12'xFF5", // Bright Yellow
    B"12'xFFF", // Bright White (Pure White)
  )

  // 2. Grayscale Ramp (16 levels)
  private val grayscaleRamp: Seq[Bits] = (0 to 15).map ( i =>
    B((i << 8) | (i << 4) | i, 12 bits)
  )

  /**
   * 3. The Color Ramp (216 colors)
   * The VGA BIOS defines this as 3 groups of 72 colors.
   * Group 1: High Intensity
   * Group 2: Medium Intensity
   * Group 3: Low Intensity
   */
  private val colorRamp: Seq[Bits] = {
    // Standard VGA intensity levels (scaled from 6-bit to 4-bit)
    // High: ~100% (15), Med: ~70% (10), Dim: ~40% (6)
    val levels = Seq(15, 10, 6)

    levels.flatMap { max =>
      val mid = (max * 0.5).toInt // Half intensity for that ramp

      // Each group of 72 consists of a transition through 6 primary/secondary hue phases
      // R->Y, Y->G, G->C, C->B, B->M, M->R
      // In your 12-bit space, we define the transitions manually to match the BIOS logic
      val phases = Seq(
        (max, 0, 0),   // Red
        (max, mid, 0), // Orange
        (max, max, 0), // Yellow
        (mid, max, 0), // Chartreuse
        (0, max, 0),   // Green
        (0, max, mid), // Spring Green
        (0, max, max), // Cyan
        (0, mid, max), // Azure
        (0, 0, max),   // Blue
        (mid, 0, max), // Violet
        (max, 0, max), // Magenta
        (max, 0, mid)  // Rose
      )

      // VGA BIOS further divides these 12 anchors into 6 steps each (12 * 6 = 72)
      // For a static 12-bit palette, we can simplify this or map the 72 directly.
      // Below is the logic to interpolate the 72 indices per level.
      (0 until 72).map { i =>
        val phaseIdx = (i / 6) % 12
        val nextPhaseIdx = (phaseIdx + 1) % 12
        val step = i % 6

        val start = phases(phaseIdx)
        val end = phases(nextPhaseIdx)

        def interp(s: Int, e: Int): Int = s + ((e - s) * step / 6)

        val r = interp(start._1, end._1)
        val g = interp(start._2, end._2)
        val b = interp(start._3, end._3)

        B((r << 8) | (g << 4) | b, 12 bits)
      }
    }
  }

  val colors: Seq[Bits] = (egaColors ++ grayscaleRamp ++ colorRamp).padTo(paletteSize, B"12'x000")
}
