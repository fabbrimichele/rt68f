## TODOs

### SRAM
* ~~Implement a simple SRAM controller to map to/from 8-bit data from/to 16-bit data~~
* Enhance the controller to have no wait states

### VGA
* ~~Center the 400 lines when using 480 lines VGA Mode~~ (DONE)
* ~~Implement a bit in the control register to switch between VGA 640x480 and VGA 640x400~~ (DONE)
* Trigger an interrupt for frame start
* Increase RAM size to 64000 bytes and increase colors at the 3 resolutions
  * 640x400 - 2 colors -> 4 colors
  * 640x200 - 4 colors -> 16 colors
  * 320x200 - 16 colors -> 256 colors

### Logic
* Move address decoding logic and bus multiplex to a separate module/s

### UART
* Use a CPU clock that allows a standard BAUD rate for the UART
* try `tio -b 76800 /dev/ttyUSB0`
