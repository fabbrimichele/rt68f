## Programming the Papilio DUO SPI Flash

The Papilio DUO uses an SPI Flash chip (typically 2MB to 4MB) to store the FPGA bitstream. Usually, 
the bitstream occupies the lower portion of the memory, and the remaining space is available for 
user data (your 68k program).

### 1. How to Write Data to the Flash

To save your program to the Flash, you generally have two options:

* **Option A: The "Golden Image" Method**
Concatenate your FPGA bitstream and your binary file into one file. The Papilio Loader tool can 
then flash the entire block. `cat fpga_bitstream.bit program_data.bin > combined.bit`

* **Option B: Papilio-Loader Offset**
Use the papilio-loader command-line tool to write specifically to a high address offset(e.g., 0x100000)
so you don't overwrite your FPGA configuration.
```
papilio-loader -f program.bin -a 0x100000
```