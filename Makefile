TARGET = Rt68f
TOPLEVEL = rt68f.Rt68fTopLevel
DEVICE = xc6slx9-tqg144-2
UCF = papilio_duo_computing_shield

all: $(TARGET)_routed.bit

$(TARGET)_routed.bit: gen/$(TARGET)TopLevel.vhdl
	hw/xilinx/build_bitstream.sh ${TARGET} ${DEVICE} ${UCF}

gen/$(TARGET)TopLevel.v: rom
	sbt "runMain ${TOPLEVEL}Verilog"

gen/$(TARGET)TopLevel.vhdl: rom
	sbt "runMain ${TOPLEVEL}Vhdl"

rom: blinker led_on keys uart uart_echo uart_tx_byte uart_hello

blinker:
	vasmm68k_mot -Fbin sw/asm/blinker.asm -o hw/gen/blinker.bin
	xxd -p -c 2 hw/gen/blinker.bin | awk '{print toupper($$0)}' > hw/spinal/rt68f/memory/blinker.hex
	mkdir -p target/scala-2.13/classes/rt68f/memory/
	cp hw/spinal/rt68f/memory/blinker.hex target/scala-2.13/classes/rt68f/memory/blinker.hex

led_on:
	vasmm68k_mot -Fbin sw/asm/led_on.asm -o hw/gen/led_on.bin
	xxd -p -c 2 hw/gen/led_on.bin | awk '{print toupper($$0)}' > hw/spinal/rt68f/memory/led_on.hex
	mkdir -p target/scala-2.13/classes/rt68f/memory/
	cp hw/spinal/rt68f/memory/led_on.hex target/scala-2.13/classes/rt68f/memory/led_on.hex

keys:
	vasmm68k_mot -Fbin sw/asm/keys.asm -o hw/gen/keys.bin
	xxd -p -c 2 hw/gen/keys.bin | awk '{print toupper($$0)}' > hw/spinal/rt68f/memory/keys.hex
	mkdir -p target/scala-2.13/classes/rt68f/memory/
	cp hw/spinal/rt68f/memory/keys.hex target/scala-2.13/classes/rt68f/memory/keys.hex


uart:
	vasmm68k_mot -Fbin sw/asm/uart.asm -o hw/gen/uart.bin
	xxd -p -c 2 hw/gen/uart.bin | awk '{print toupper($$0)}' > hw/spinal/rt68f/memory/uart.hex
	mkdir -p target/scala-2.13/classes/rt68f/memory/
	cp hw/spinal/rt68f/memory/uart.hex target/scala-2.13/classes/rt68f/memory/uart.hex

uart_echo:
	vasmm68k_mot -Fbin sw/asm/uart_echo.asm -o hw/gen/uart_echo.bin
	xxd -p -c 2 hw/gen/uart_echo.bin | awk '{print toupper($$0)}' > hw/spinal/rt68f/memory/uart_echo.hex
	mkdir -p target/scala-2.13/classes/rt68f/memory/
	cp hw/spinal/rt68f/memory/uart_echo.hex target/scala-2.13/classes/rt68f/memory/uart_echo.hex

uart_tx_byte:
	vasmm68k_mot -Fbin sw/asm/uart_tx_byte.asm -o hw/gen/uart_tx_byte.bin
	xxd -p -c 2 hw/gen/uart_tx_byte.bin | awk '{print toupper($$0)}' > hw/spinal/rt68f/memory/uart_tx_byte.hex
	mkdir -p target/scala-2.13/classes/rt68f/memory/
	cp hw/spinal/rt68f/memory/uart_tx_byte.hex target/scala-2.13/classes/rt68f/memory/uart_tx_byte.hex

uart_hello:
	vasmm68k_mot -Fbin sw/asm/uart_hello.asm -o hw/gen/uart_hello.bin
	xxd -p -c 2 hw/gen/uart_hello.bin | awk '{print toupper($$0)}' > hw/spinal/rt68f/memory/uart_hello.hex
	mkdir -p target/scala-2.13/classes/rt68f/memory/
	cp hw/spinal/rt68f/memory/uart_hello.hex target/scala-2.13/classes/rt68f/memory/uart_hello.hex

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
	rm -rf hw/spinal/rt68f/memory/blinker.hex
	rm -rf hw/spinal/rt68f/memory/led_on.hex
	rm -rf hw/spinal/rt68f/memory/keys.hex
	rm -rf hw/spinal/rt68f/memory/uart.hex
	rm -rf hw/spinal/rt68f/memory/uart_echo.hex
	rm -rf hw/spinal/rt68f/memory/uart_tx_byte.hex

