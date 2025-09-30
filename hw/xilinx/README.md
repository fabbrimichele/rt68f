## Adding verilog or VHDL to the build
Verilog and VHDL files need to be added to the file `Ao68000.prj`, e.g.
```
verilog work "../hw/gen/Ao68000TopLevel.v"
vhdl work "../hw/gen/mergeRTL.vhd"
verlig work "../hw/gen/mergeRTL.v"
```

For example when adding a black box for verilog, the last line needs to be added

## Use this project as template
To create another project the following files require adjustments:
* Ao68000.prj
* Ao68000.xst

An xst report can be found in `target/${TARGET}.syr`, where `${TARGET}` is defined in the Makefile (e.g. `Ao68000.syr`)

## Setting up GHDL
Install:
* libboost 
  ```shell
  sudo apt update
  sudo apt install libboost-all-dev
  ```

## RS232 Adapter
https://www.waveshare.com/wiki/RS232_Board