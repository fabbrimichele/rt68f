# --- Configuration Variables ---
TARGET = Rt68f
TOPLEVEL = rt68f.Rt68fTopLevel
DEVICE = xc6slx9-tqg144-2
UCF = papilio_duo_computing_shield
ASSEMBLIES = blinker led_on keys uart uart_echo uart_tx_byte uart_hello mem_test monitor

# App
# Define the source directory for app assembly files
ASM_APP_DIR := sw/app/asm
# Define the target directory for all output files
TARGET_APP_DIR := target/app
# Define the memory starting address for the program, used in the header
PROGRAM_ADDRESS := 00000900
# Define the list of assembly files (e.g., if you have blinker.asm and main.asm)
ASM_APP_SOURCES := $(wildcard $(ASM_APP_DIR)/*.asm)
# Target: Create the final .bin files from the list of sources
BIN_APP_TARGETS := $(patsubst $(ASM_APP_DIR)/%.asm, $(TARGET_APP_DIR)/%.bin, $(ASM_APP_SOURCES))
RAW_FILE_NAME := $(TARGET_APP_DIR)/$*_raw.bin

# --- Directories ---
ASM_SRC_DIR = sw/fw/asm
BIN_GEN_DIR = hw/gen
HEX_SPINAL_DIR = hw/spinal/rt68f/memory
HEX_CLASS_DIR = target/scala-2.13/classes/rt68f/memory

# --- Derived File Lists ---
# List of all hex files in the Spinal directory
SPINAL_HEX_FILES = $(patsubst %, $(HEX_SPINAL_DIR)/%.hex, $(ASSEMBLIES))

.PHONY: all apps clean rom prog-fpga prog-flash disassemble

# --- Targets ---
all: apps $(TARGET)_routed.bit

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

blinker.bin:
	mkdir -p target/app
	vasmm68k_mot -Fbin -o target/app/blinker_raw.bin sw/app/asm/blinker.asm
	FILE_SIZE=$$(stat -c %s target/app/blinker_raw.bin); \
	HEX_SIZE=$$(printf "%08X" "$$FILE_SIZE"); \
	HEADER_HEX="00000900"$$HEX_SIZE; \
	echo "$$HEADER_HEX" | xxd -r -p | cat - target/app/blinker_raw.bin > target/app/blinker.bin

# 'apps' target: invoked specifically to build all application binaries
apps: $(BIN_APP_TARGETS)

$(TARGET_APP_DIR)/%.bin: $(ASM_APP_DIR)/%.asm
	@mkdir -p $(TARGET_APP_DIR)
	# 1. Assemble to a raw binary image (*_raw.bin)
	vasmm68k_mot -Fbin -o $(RAW_FILE_NAME) $<
	# 2. Calculate length and prepend the header. All steps in ONE shell session.
	SHELL_RAW_FILE="$(RAW_FILE_NAME)"; \
	FILE_SIZE=$$(stat -c %s $$SHELL_RAW_FILE); \
	HEX_SIZE=$$(printf "%08X" "$$FILE_SIZE"); \
	HEADER_HEX="$(PROGRAM_ADDRESS)"$$HEX_SIZE; \
	echo "$$HEADER_HEX" | xxd -r -p | cat - $$SHELL_RAW_FILE > $@
	# 3. Clean up the intermediate raw file
	@rm $(RAW_FILE_NAME)


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

