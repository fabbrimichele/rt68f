## TODOs

### VGA
* Center the 400 lines when using 480 lines VGA Mode
* Implement a bit in the control register to switch between VGA 640x480 and VGA 640x400
* Trigger an interrupt for frame start

### Logic
* Move address decoding logic and bus multiplex to a separate module/s

### UART
* Use a CPU clock that allows a standard BAUD rate for the UART
* try `tio -b 76800 /dev/ttyUSB0`
