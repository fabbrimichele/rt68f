## TODOs

### Firmware
* Fix all firmware examples to use the new memory map (monitor is already updated)

### SRAM
* ~~Implement a simple SRAM controller to map to/from 8-bit data from/to 16-bit data~~
* ~~Enhance the controller to have no wait states~~
* ~~Define Reset SP and PC in ROM and replace FPGA Block Memory with SRAM~~

### VGA
* ~~Center the 400 lines when using 480 lines VGA Mode~~ (DONE)
* ~~Implement a bit in the control register to switch between VGA 640x480 and VGA 640x400~~ (DONE)
* Trigger an interrupt for frame start and 68000 interrupt
* ~~Increase RAM size to 64000  (requires monitor refactoring) and increase colors at the 3 resolutions~~
  * ~~First need to make a monitor/bootloader version that is not bigger than 1536 bytes~~
  * ~~640x400 - 2 colors -> 4 colors~~
  * ~~640x200 - 4 colors -> 16 colors~~
  * ~~320x200 - 16 colors -> 256 colors~~

### Logic
* ~~Move address decoding logic and bus multiplex to a separate module/s~~ (DONE)

### UART
* Use a CPU clock that allows a standard BAUD rate for the UART
* try `tio -b 76800 /dev/ttyUSB0`

### SPI Flash
* Design the registers to read from the SPI
* Read from the SPI Flash data with a 68000 program

### Sound
* Integrate a sound core synth, see:
  * https://www.fpga4fun.com/OPL.html
  * https://github.com/jotego/jtopl
