# SRamCtrl
The **SRamCtrl** operates at 64 MHz, four times the frequency of the 16 MHz CPU clock. This high-speed 
differential enables the controller to perform two sequential 8-bit fetches from the SRAM within a 
single 68000 bus cycle. Consequently, the controller can provide a full 16-bit word to the CPU without 
requiring wait states. The controller is designed to assert `DTACK` during the 68000 S4 state.

## GTKWave Diagram
The diagram below illustrates the signal behavior during `SRamCtrlSim` execution. The `DTACK` signal is 
asserted as the 68000 transitions from `S3 to `S4, ensuring the CPU samples a valid data acknowledgment 
in time to complete the cycle.
![SRamCtrl GTKWave Screenshot](doc/media/SRamCtrlSimGTKWave.png)

## 68000 Read and Write-Cycle Timing Diagram
According to the 68000 bus specifications, the processor samples `DTACK` within `S4`. Providing the 
signal at this moment ensures the cycle concludes in `S7` without the insertion of wait states.
![SRamCtrl GTKWave Screenshot](doc/media/68000ReadCycle.png)
