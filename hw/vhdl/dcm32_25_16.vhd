------------------------------------------------------------------------------
-- "Output    Output      Phase     Duty      Pk-to-Pk        Phase"
-- "Clock    Freq (MHz) (degrees) Cycle (%) Jitter (ps)  Error (ps)"
------------------------------------------------------------------------------
-- CLK_OUT1____25.143______0.000______50.0______995.455____150.000
-- CLK_OUT2____16.000______0.000______50.0______300.000____150.000
--
------------------------------------------------------------------------------
-- "Input Clock   Freq (MHz)    Input Jitter (UI)"
------------------------------------------------------------------------------
-- __primary__________32.000____________0.010

library ieee;
use ieee.std_logic_1164.all;
use ieee.std_logic_unsigned.all;
use ieee.std_logic_arith.all;
use ieee.numeric_std.all;

library unisim;
use unisim.vcomponents.all;

entity dcm32_25_16 is
    port
    (-- Clock in ports
     CLK_IN1           : in     std_logic;
     -- Clock out ports
     CLK_OUT1          : out    std_logic;
     CLK_OUT2          : out    std_logic;
     -- Status and control signals
     RESET             : in     std_logic;
     LOCKED            : out    std_logic
    );
end dcm32_25_16;

architecture xilinx of dcm32_25_16 is
    -- Output clock buffering
    signal clkfb             : std_logic;
    signal clk0              : std_logic;
    signal clkfx             : std_logic;
    signal clkdv             : std_logic;
    signal clkfbout          : std_logic;
    signal locked_internal   : std_logic;
begin
    -- Clocking primitive
    --------------------------------------

    -- Instantiation of the DCM primitive
    --    * Unused inputs are tied off
    --    * Unused outputs are labeled unused
    dcm_sp_inst: DCM_SP
        generic map
        (CLKDV_DIVIDE          => 2.000,
         CLKFX_DIVIDE          => 14,
         CLKFX_MULTIPLY        => 11,
         CLKIN_DIVIDE_BY_2     => FALSE,
         CLKIN_PERIOD          => 31.25,
         CLKOUT_PHASE_SHIFT    => "NONE",
         CLK_FEEDBACK          => "1X",
         DESKEW_ADJUST         => "SYSTEM_SYNCHRONOUS",
         PHASE_SHIFT           => 0,
         STARTUP_WAIT          => FALSE)
        port map
        -- Input clock
        (CLKIN                 => CLK_IN1,
         CLKFB                 => clkfb,
         -- Output clocks
         CLK0                  => clk0,
         CLK90                 => open,
         CLK180                => open,
         CLK270                => open,
         CLK2X                 => open,
         CLK2X180              => open,
         CLKFX                 => clkfx,
         CLKFX180              => open,
         CLKDV                 => clkdv,
         -- Ports for dynamic phase shift
         PSCLK                 => '0',
         PSEN                  => '0',
         PSINCDEC              => '0',
         PSDONE                => open,
         -- Other control and status signals
         LOCKED                => LOCKED,
         RST                   => RESET,
         -- Unused pin, tie low
         DSSEN                 => '0');


    -- Output buffering
    -------------------------------------
    clkf_buf : BUFG
        port map
        (O => clkfb,
         I => clk0);


    clkout1_buf : BUFG
        port map
        (O   => CLK_OUT1,
         I   => clkfx);



    clkout2_buf : BUFG
        port map
        (O   => CLK_OUT2,
         I   => clkdv);

end xilinx;