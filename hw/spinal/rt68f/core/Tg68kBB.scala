package rt68f.core

import spinal.core._

import scala.language.postfixOps

case class Tg68kConfig(
  srRead: Int = 2,
  vbrStackframe: Int = 2,
  extAddrMode: Int = 2,
  mulMode: Int = 2,
  divMode: Int = 2,
  BitField: Int = 2,
  BarrelShifter: Int = 1,
  mulHardware: Int = 1,
)

/**
 *
 * @param cpuType 0->68000  1->68010  13->68020
 */
class Tg68kBB(config: Tg68kConfig = Tg68kConfig()) extends BlackBox {
  //addGeneric("CPU", B"00")

  val io = new Bundle {
    val CLK           = in Bool() //
    val RESET         = in Bool()
    val HALT          = in Bool()
    val BERR          = in Bool() //     -- only 68000 Stackpointer dummy for Atari ST core
    val IPL           = in Bits(3 bits) //:="111";
    val ADDR          = out Bits(32 bits)
    val FC            = out Bits(3 bits)
    val DATAI         = in Bits(16 bits)
    val DATAO         = out Bits(16 bits)
    val AS            = out Bool()
    val UDS           = out Bool()
    val LDS           = out Bool()
    val RW            = out Bool()
    val DTACK         = in Bool()
    val E             = out Bool()
    val VPA           = in Bool()
    val VMA           = out Bool()
  }

  /*
  addGeneric("SR_Read", 2)        // 0=>user,   1=>privileged,		2=>switchable with CPU(0)
  addGeneric("VBR_Stackframe", 0) // 0=>no,     1=>yes/extended,	2=>switchable with CPU(0)
  addGeneric("extAddr_Mode", 0)   // 0=>no,			1=>yes,				    2=>switchable with CPU(1)
  addGeneric("MUL_Mode", 1)       // 0=>16Bit,	1=>32Bit,			    2=>switchable with CPU(1),  3=>no MUL
  addGeneric("DIV_Mode", 0)       // 0=>16Bit,	1=>32Bit,			    2=>switchable with CPU(1),  3=>no DIV
  addGeneric("BitField", 0)       // 0=>no,			1=>yes,				    2=>switchable with CPU(1)
  addGeneric("BarrelShifter", 0)  // 0=>no,			1=>yes,				    2=>switchable with CPU(1)
  addGeneric("MUL_Hardware", 1)   // 0=>no,			1=>yes,

  val io = new Bundle {
    val clk						 = in Bool()
    val nReset				 = in Bool() // low active
    val clkena_in			 = in Bool() // '1'
    val data_in				 = in Bits(16 bits)
    val IPL						 = in Bits(3 bits) //:="111";
    val IPL_autovector = in Bool() // '0'
    val berr					 = in Bool() // '0'
    val CPU						 = in Bits(2 bits)   // 00->68000  01->68010  11->68020(only some parts - yet)
    val addr_out			 = out Bits(32 bits)
    val data_write		 = out Bits(16 bits)
    val nWr						 = out Bool()
    val nUDS					 = out Bool()
    val nLDS					 = out Bool()
    val busstate			 = out Bits(2 bits)	// 00-> fetch code 10->read data 11->write data 01->no memaccess
    val longword			 = out Bool()
    val nResetOut			 = out Bool()
    val FC						 = out Bits(3 bits)
    val clr_berr			 = out Bool()
    // for debug
    val skipFetch			 = out Bool()
    val regin_out			 = out Bits(32 bits)
    val CACR_out			 = out Bits(4 bits)
    val VBR_out				 = out Bits(32 bits)
  }
  */

  // Map the clock domain
  // Mapped in the wrapper
  mapClockDomain(clock = io.CLK, reset = io.RESET, resetActiveLevel = LOW/*, enable = io.clkena_in*/)

  setDefinitionName("TG68K") // This tells SpinalHDL which VHDL module to instantiate
  addRTLPath("hw/vhdl/TG68K.vhd") // Merge the file to the generated 'mergeRTL.vhd' file
  addRTLPath("hw/vhdl/TG68K_Pack.vhd") // Merge the file to the generated 'mergeRTL.vhd' file
  addRTLPath("hw/vhdl/TG68K_ALU.vhd") // Merge the file to the generated 'mergeRTL.vhd' file
  addRTLPath("hw/vhdl/TG68KdotC_Kernel.vhd") // Merge the file to the generated 'mergeRTL.vhd' file
  noIoPrefix() // Remove io_ prefix
}
