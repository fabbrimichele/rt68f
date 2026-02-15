# OPL2

## Sound device mapped registers
How to write to a register, for example to write the value `$04` to the register `$02`
1. write the register address `$02` to `0x00407000` 
2. wait 10 ms (TODO: this might not be required, it's a soft core running at CPU Frequency)
3. write the value `$04` to `0x00407002`

## OPL2 registers
TODO

## Example of sound
1. turn off channels `[addr:$B0+ch, data:$0]`-> d5 - key on: 0 channel off, 1, channel on 
