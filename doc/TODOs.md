## TODOs

### Software
* ~~Define a linker file (LD) with the memory layout for the monitor (RAM version)~~

### Bootloader
* There might be a bug the serial loader:
  * When loading the `monitor_ram.bin` and then entering a command it prints an error, I think it doesn't read the last character sent.
  * Depending on the `monitor_ram.bin` size the last command parsing doesn't work, also in this case I think it depends on the last byte not read.

### Firmware
* Fix all firmware examples to use the new memory map (monitor is already updated)

### PS/2 Keyboard
* Design a PS/2 keyboard memory mapped device 

### SRAM
* ~~Implement a simple SRAM controller to map to/from 8-bit data from/to 16-bit data~~
* ~~Enhance the controller to have no wait states~~
* ~~Define Reset SP and PC in ROM and replace FPGA Block Memory with SRAM~~

### VGA
* ~~Center the 400 lines when using 480 lines VGA Mode~~ (DONE)
* ~~Implement a bit in the control register to switch between VGA 640x480 and VGA 640x400~~ (DONE)
* ~~Increase RAM size to 64000  (requires monitor refactoring) and increase colors at the 3 resolutions~~
  * ~~First need to make a monitor/bootloader version that is not bigger than 1536 bytes~~
  * ~~640x400 - 2 colors -> 4 colors~~
  * ~~640x200 - 4 colors -> 16 colors~~
  * ~~320x200 - 16 colors -> 256 colors~~
* ~~Trigger an interrupt for frame start and 68000 interrupt~~
* Design a screen mode 640x480 with 2 colors (will be easier to start working with EmuTOS)
* Use SRAM for the framebuffer, it'll require prioritize the access between the VGA and the CPU
  * You can start moving the FB ram out of the VGA module and create a module with 2 ports, one for the framebuffer and one for the CPU 
* Make an interrupt that triggers at a specific line, it could be used to change colors or resolution at half of the screen
* Implement a sprite (for the mouse pointer)

### Logic
* ~~Move address decoding logic and bus multiplex to a separate module/s~~ (DONE)

### Timer
* ~~Create a programmable timers that triggers an interrupt~~
* Implement single mode (repeat mode is only implemented so far)
* ~~Implement Timer B (for some reason it blocks the bootloader)~~

### UART
* ~~Integrate serial interrupts~~
* Use a CPU clock that allows a standard BAUD rate for the UART (all other clocks must be updated accordingly)
* try `tio -b 76800 /dev/ttyUSB0`

### SPI Flash
* ~~Design the registers to read from the SPI~~
* ~~Read from the SPI Flash data with a 68000 program~~
* ~~Create a bootloader in ROM that starts the monitor program from the SPI Flash~~
* Use 64Mhz clock? 

### SD Card
* Implement an SD Card controller using the SPI device

### Sound
* Integrate a sound core synth, see:
  * https://www.fpga4fun.com/OPL.html
  * https://github.com/jotego/jtopl
