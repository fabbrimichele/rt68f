# Rt68f
A project in SpinalHDL for the Papilio Duo dev board (Xilinx Spartan 6)
* The hardware description is into `hw/spinal/projectname/Rt68fTopLevel.scala`
* The testbench is into `hw/spinal/projectname/Rt68fTopLevelSim.scala`

## Motorola 68000
* https://www.nxp.com/docs/en/reference-manual/MC68000UM.pdf

### 68000 cores
* https://github.com/alfikpl/ao68000
* https://github.com/alfikpl/aoOCS
* https://github.com/vfinotti/ahb3lite_wb_bridge/blob/master/wb_to_ahb3lite.v
* https://github.com/TobiFlex/TG68K.C



## ym3812
* https://www.thingsmadesimple.com/2022/11/28/ym3812-part-1-register-basics/

### ym3812 core
* https://github.com/jotego/jtopl
* https://www.fpga4fun.com/OPL.html


## To configure the project
* Java 17 is required
* Install [GHDL](https://spinalhdl.github.io/SpinalDoc-RTD/master/SpinalHDL/Simulation/install/GHDL.html)

## To build the project
```bash
make
```

## To run the simulation
```bash
sbt
runMain rt68f.Rt68fTopLevelSim
```


## To program the bitstream to the Papilio DUO
```bash
/opt/GadgetFactory/papilio-loader
```

## To load a file to the SBC
```bash
make load BIN_FILE=vga16col_palette.bin
```
where the file specified with `BIN_FILE` well be search in `/rt68f/target/app`

## Papilio prog
`papilio-prog` is the programmer for the Papilio DUO board.
For 64 bit OS needs to be recompiled:
* clone the repository: `git clone git@github.com:GadgetFactory/Papilio-Loader.git`
* enter the directory: `cd Papilio-Loader/papilio-prog`
* run: `./configure`
* configure in the Makefile: `CXXFLAGS` with `-std=c++11` in addition to the existing parameters
* run  `make`
* copy `papilio-prog` to a bin path

### To check that the FPGA is found:
```bash
papilio-prog -j
```
It should return something like:
```
Using built-in device list
JTAG chainpos: 0 Device IDCODE = 0x24001093	Desc: XC6SLX9
```

### To program the FPGA (temporary):
Run the following command (or use the Makefile):
```bash
papilio-prog -v -f stream.bit
```

### To program the SPI Flash (permanent):
Run the following command (or use the Makefile):
```bash
papilio-prog -v -s a -r -f target/$(TARGET).bit -b hw/papilio-loader/bscan_spi_xc6slx9.bit
```
| Option        | Meaning                                                 |
|---------------|---------------------------------------------------------|
| -v            | verbose                                                 |
| -s a          | write to the flash                                      |
| -r            | reset FPGA after programming                            |
| -f bitfile    | bitstream to program                                    |
| -b bitfile    | bscan_spi bit file - required to program the flash      |


## IDE
Go with IntelliJ IDEA. I couldn’t get VS Code to work reliably.

## References
* [Papilio-DUO GitHub](https://github.com/GadgetFactory/Papilio-DUO)
* [Papilio-Loader GitHub](https://github.com/GadgetFactory/Papilio-Loader)
* [Programming Papilio-DUO](https://github.com/defano/digital-design/blob/master/docs/papilio-instructions.md)
* [Computing Shield Doc](https://oe7twj.at/index.php?title=FPGA/PapilioDuo#Computing_Shield)
* [Computing Shield Schematic](https://oe7twj.at/images/1/17/BPS6001_Classic_Computing_Shield.pdf)
* [Computing Shield Pin Mapping](https://oe7twj.at/images/4/4d/Papilio_DUO_pinouts_image_for_cc.png)
* [Installing Xilinx ISE WebPACK 14.7](https://blog.rcook.org/blog/2019/papilio-duo-part-1/)