# --- Configuration Variables ---
TARGET = Rt68f
TOPLEVEL = rt68f.Rt68fTopLevel
DEVICE = xc6slx9-tqg144-2
UCF = papilio_duo_computing_shield
ASSEMBLIES = blinker led_on keys uart uart_echo uart_tx_byte uart_hello mem_test monitor_rom uart16450_tx_byte uart16450_echo min_mon

# App
# Define the source directory for app assembly files
ASM_APP_DIR := sw/app/asm
# Define the target directory for all output files
TARGET_APP_DIR := target/app
# Define the memory starting address for the program, used in the header
PROGRAM_ADDRESS := 00000400
# Define the list of assembly files (e.g., if you have blinker.asm and main.asm)
ASM_APP_SOURCES := $(wildcard $(ASM_APP_DIR)/*.asm)
# Target: Create the final .bin files from the list of sources
BIN_APP_TARGETS := $(patsubst $(ASM_APP_DIR)/%.asm, $(TARGET_APP_DIR)/%.bin, $(ASM_APP_SOURCES))
RAW_FILE_NAME := $(TARGET_APP_DIR)/$*_raw.bin

# Image
VGA_ADDRESS := 00200000

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
all: apps monitor $(TARGET)_routed.bit

$(TARGET)_routed.bit: gen/$(TARGET)TopLevel.vhdl
	hw/xilinx/build_bitstream.sh ${TARGET} ${DEVICE} ${UCF}

gen/$(TARGET)TopLevel.v: rom
	sbt "runMain ${TOPLEVEL}Verilog"

gen/$(TARGET)TopLevel.vhdl: rom
	sbt "runMain ${TOPLEVEL}Vhdl"

# 'rom' target depends on the list of hex files
ROM_HEX_FILES = $(patsubst %, $(HEX_CLASS_DIR)/%.hex, $(ASSEMBLIES))
rom: $(HEX_CLASS_DIR)/bootloader.hex $(ROM_HEX_FILES)

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


$(HEX_CLASS_DIR)/bootloader.hex: $(ASM_SRC_DIR)/bootloader.asm
	@echo "--- Assembling and Converting bootloader ---"
	# 1. Assemble the 68000 code to a binary file
	vasmm68k_mot -Felf -o $(BIN_GEN_DIR)/bootloader.o $(ASM_SRC_DIR)/bootloader.asm
	# 2. Link object file
	vlink -T $(ASM_SRC_DIR)/bootloader.ld -b rawbin1 -M$(BIN_GEN_DIR)/bootloader.sym -o $(BIN_GEN_DIR)/bootloader.bin $(BIN_GEN_DIR)/bootloader.o
	# 3. Convert binary to a two-byte-per-line hex file, convert to uppercase
	xxd -p -c 2 $(BIN_GEN_DIR)/bootloader.bin | awk '{print toupper($$0)}' > $(HEX_SPINAL_DIR)/bootloader.hex
	# 4. Ensure the destination directory exists
	mkdir -p $(HEX_CLASS_DIR)
	# 5. Copy the hex file to the Scala classes path for resource loading
	cp $(HEX_SPINAL_DIR)/bootloader.hex $@


blinker.bin:
	mkdir -p target/app
	vasmm68k_mot -Fbin -o target/app/blinker_raw.bin sw/app/asm/blinker.asm
	FILE_SIZE=$$(stat -c %s target/app/blinker_raw.bin); \
	HEX_SIZE=$$(printf "%08X" "$$FILE_SIZE"); \
	HEADER_HEX="00000900"$$HEX_SIZE; \
	echo "$$HEADER_HEX" | xxd -r -p | cat - target/app/blinker_raw.bin > target/app/blinker.bin

BIN_FILE ?= vga_grid.bin
load: apps
	# 1. Send LOAD command to prepare the device
	@echo "--- Loading $(BIN_FILE) to /dev/ttyUSB1 ---"
	printf "LOAD\r" > /dev/ttyUSB1
	sleep 0.5
	# 2. Transfer the contents of the chosen binary file
	cat target/app/$(BIN_FILE) > /dev/ttyUSB1
	sleep 0.5
	# 3. Send RUN command
	@echo "--- Running application at 0x4000 ---"
	printf "RUN 4000\r" > /dev/ttyUSB1


# It requires 640x400 pixel images (ideally B&W)
build-img-bin:
	@mkdir -p $(TARGET_APP_DIR)
	convert $(ASM_APP_DIR)/img.png -depth 1 GRAY:$(TARGET_APP_DIR)/img.tmp
	dd if=$(TARGET_APP_DIR)/img.tmp bs=1 count=32000 > $(TARGET_APP_DIR)/img.bin
	HEADER_HEX="$(VGA_ADDRESS)00007D00"; \
	echo "$$HEADER_HEX" | xxd -r -p | cat - $(TARGET_APP_DIR)/img.bin > $(TARGET_APP_DIR)/img_with_header.bin

 # TODO: use the script to parse the image, or just copy the image from sw/app/asm/img.tmp
build-img4col-bin:
	@mkdir -p $(TARGET_APP_DIR)
	#convert $(ASM_APP_DIR)/img4col.png -depth 2 GRAY:$(TARGET_APP_DIR)/img.tmp
	#convert $(ASM_APP_DIR)/img4col.png +dither -remap $(ASM_APP_DIR)/palette.png -depth 2 -compress none GRAY:img.tmp
	dd if=$(TARGET_APP_DIR)/img.tmp bs=1 count=32000 > $(TARGET_APP_DIR)/img.bin
	HEADER_HEX="$(VGA_ADDRESS)00007D00"; \
	echo "$$HEADER_HEX" | xxd -r -p | cat - $(TARGET_APP_DIR)/img.bin > $(TARGET_APP_DIR)/img_with_header.bin


load-img-bin:
	printf "FBCLR\r" > /dev/ttyUSB1
	sleep 0.5
	printf "LOAD\r" > /dev/ttyUSB1
	sleep 0.5
	cat $(TARGET_APP_DIR)/img_with_header.bin > /dev/ttyUSB1


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


# Monitor RAM version
MONITOR_ADDRESS = 0007C000
MONITOR_BIN = $(TARGET_APP_DIR)/monitor_ram.bin
MONITOR_SRC_DIR = sw/fw/asm

monitor: $(MONITOR_BIN)
$(MONITOR_BIN): $(MONITOR_SRC_DIR)/monitor_ram.asm
	@mkdir -p $(TARGET_APP_DIR)
	# 1. Assemble to an elf file
	vasmm68k_mot -Felf -o $(TARGET_APP_DIR)/monitor_ram.o $(MONITOR_SRC_DIR)/monitor_ram.asm
	# 2. Link object file
	vlink -T $(MONITOR_SRC_DIR)/monitor_ram.ld -b rawbin1 -M$(TARGET_APP_DIR)/monitor_ram.sym -o $(TARGET_APP_DIR)/monitor_ram_raw.bin $(TARGET_APP_DIR)/monitor_ram.o
	# 3. Calculate length and prepend the header
	@SHELL_RAW_FILE="$(TARGET_APP_DIR)/monitor_ram_raw.bin"; \
	FILE_SIZE=$$(stat -c %s $$SHELL_RAW_FILE); \
	HEX_SIZE=$$(printf "%08X" "$$FILE_SIZE"); \
	HEADER_HEX="$(MONITOR_ADDRESS)"$$HEX_SIZE; \
	echo "$$HEADER_HEX" | xxd -r -p | cat - $$SHELL_RAW_FILE > $@
	# 4. Clean up
	@echo "Built $(MONITOR_BIN) with header [$(PROGRAM_ADDRESS) | size: $$FILE_SIZE]"


# --- Utility Targets ---
prog-fpga:
	echo "Programming FPGA"
	papilio-prog -v -f target/$(TARGET).bit

prog-flash:
	echo "Programming Flash"
	papilio-prog -v -s a -r -f target/Rt68f.bit -b hw/papilio-loader/bscan_spi_xc6slx9.bit -a 80000:target/app/monitor_ram.bin
	# papilio-prog -v -s a -r -f target/$(TARGET).bit -b hw/papilio-loader/bscan_spi_xc6slx9.bit

disassemble:
	#m68k-elf-objdump -D -b binary -m m68k --adjust-vma=0x0 hw/gen/led_on.bin
	#m68k-elf-objdump -D -b binary -m m68k --adjust-vma=0x0 hw/gen/blinker.bin
	#m68k-elf-objdump -D -b binary -m m68k --adjust-vma=0x0 hw/gen/keys.bin
	#m68k-elf-objdump -D -b binary -m m68k --adjust-vma=0x0 hw/gen/bootloader.bin
	m68k-elf-objdump -D -b binary -m m68k --adjust-vma=0x0 target/app/monitor_ram.bin

# TODO: add command to debug timing: trce -v 12 -fastpaths -o design_timing_report Rt68f.ncd Rt68f.pcf

clean:
	rm -f hw/gen/*.v
	rm -f hw/gen/*.vhd
	rm -f hw/gen/*.bin
	rm -f hw/gen/*.hex
	rm -f hw/gen/*.o
	rm -f hw/gen/*.sym
	rm -f target/*.edf
	rm -f target/*.o
	rm -f target/*.sym
	rm -rf _xmsgs
	rm -rf xlnx_auto_0_xdb
	rm -rf target
	# Remove hex files in the spinal directory ONLY for listed assemblies
	rm -f $(SPINAL_HEX_FILES)
	rm -f $(HEX_SPINAL_DIR)/bootloader.hex
