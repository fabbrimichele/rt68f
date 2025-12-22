# Simulating the circuits

The simulations are based on **GHDL**, and the resulting signals can be viewed with **GTKWave**.

## Run the simulation
1. Open any simulation Scala class in IntelliJ IDEA, for example `SRamCtrlSim`.
2. Start the class execution.

## Show the results in GTKWave
Once the simulation has finished:
1. Open GTKWave from the shell by running `gtkwave`.
2. Select `File -> Open New Window` from the menu, or press `Ctrl+N`.
3. Browse to the folder containing the simulation results, for example  
   `~/rt68f/simWorkspace/TopWrapper/test`.
4. Select the waveform file: `wave.fst`.
5. In the top-left panel, select the component containing the signals, for example `TopWrapper` or `SRamCtrl`.
6. In the bottom-left panel, select the signals to add to the waveform view, for example `cpuClk` or `dut_io_bus_AS`.
7. Zoom out to find points of interest.

![GTKWave Screenshot](doc/media/GTKView.png)