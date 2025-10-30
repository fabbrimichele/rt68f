# --- Configuration Variables ---
TARGET = Rt68f
TOPLEVEL = rt68f.Rt68fTopLevel
DEVICE = xc6slx9-tqg144-2
UCF = papilio_duo_computing_shield
ASSEMBLIES = blinker led_on keys uart uart_echo uart_tx_byte uart_hello mem_test monitor

# --- Directories ---
ASM_SRC_DIR = sw/fw/asm
BIN_GEN_DIR = hw/gen
HEX_SPINAL_DIR = hw/spinal/rt68f/memory
HEX_CLASS_DIR = target/scala-2.13/classes/rt68f/memory

# --- Derived File Lists ---
# List of all hex files in the Spinal directory
SPINAL_HEX_FILES = $(patsubst %, $(HEX_SPINAL_DIR)/%.hex, $(ASSEMBLIES))

# --- Targets ---
all: $(TARGET)_routed.bit

$(TARGET)_routed.bit: gen/$(TARGET)TopLevel.vhdl
	hw/xilinx/build_bitstream.sh ${TARGET} ${DEVICE} ${UCF}

gen/$(TARGET)TopLevel.v: rom
	sbt "runMain ${TOPLEVEL}Verilog"

gen/$(TARGET)TopLevel.vhdl: rom
	sbt "runMain ${TOPLEVEL}Vhdl"

# 'rom' target depends on the list of hex files
ROM_HEX_FILES = $(patsubst %, $(HEX_CLASS_DIR)/%.hex, $(ASSEMBLIES))
rom: $(ROM_HEX_FILES)

# ----------------------------------------------------------------------
# 🌟 Pattern Rule for 68000 Assembly and ROM Image Generation 🌟
# This rule generates the final .hex file in the classes directory.
# Target: target/scala-2.13/classes/rt68f/memory/%.hex
# Prerequisites: The corresponding assembly file
#
# $@: The full target name (e.g., target/.../blinker.hex)
# $*: The stem (the part that matched the %, e.g., blinker)
# $<: The first prerequisite (the assembly file)
# ----------------------------------------------------------------------
$(HEX_CLASS_DIR)/%.hex: $(ASM_SRC_DIR)/%.asm
	@echo "--- Assembling and Converting $* ---"
	# 1. Assemble the 68000 code to a binary file
	vasmm68k_mot -Fbin $< -o $(BIN_GEN_DIR)/$*.bin
	# 2. Convert binary to a two-byte-per-line hex file, convert to uppercase
	xxd -p -c 2 $(BIN_GEN_DIR)/$*.bin | awk '{print toupper($$0)}' > $(HEX_SPINAL_DIR)/$*.hex
	# 3. Ensure the destination directory exists
	mkdir -p $(HEX_CLASS_DIR)
	# 4. Copy the hex file to the Scala classes path for resource loading
	cp $(HEX_SPINAL_DIR)/$*.hex $@

# --- Utility Targets ---

prog-fpga:
	echo "Programming FPGA"
	papilio-prog -v -f target/$(TARGET).bit

prog-flash:
	echo "Programming Flash"
	papilio-prog -v -s a -r -f target/$(TARGET).bit -b hw/papilio-loader/bscan_spi_xc6slx9.bit

disassemble:
	m68k-elf-objdump -D -b binary -m m68k --adjust-vma=0x0 hw/gen/led_on.bin
	m68k-elf-objdump -D -b binary -m m68k --adjust-vma=0x0 hw/gen/blinker.bin
	m68k-elf-objdump -D -b binary -m m68k --adjust-vma=0x0 hw/gen/keys.bin

clean:
	rm -f hw/gen/*.v
	rm -f hw/gen/*.vhd
	rm -f hw/gen/*.bin
	rm -f hw/gen/*.hex
	rm -f target/*.edf
	rm -rf _xmsgs
	rm -rf xlnx_auto_0_xdb
	rm -rf target
	# Remove hex files in the spinal directory ONLY for listed assemblies
	rm -f $(SPINAL_HEX_FILES)

