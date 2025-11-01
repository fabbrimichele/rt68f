library IEEE;
use IEEE.STD_LOGIC_1164.ALL;

-- Essential for Xilinx Primitives like DCM_SP
library UNISIM;
use UNISIM.VComponents.all;

----------------------------------------------------------------------
-- Entity: Dcm25Mhz
-- Converts 32 MHz input clock to ~25.143 MHz output using DCM_SP.
----------------------------------------------------------------------
entity Dcm25Mhz is
    port (
        clk  : in  std_logic;      -- 32 MHz input clock
        reset  : in  std_logic;      -- Active high reset
        clk25  : out std_logic;      -- ~25.143 MHz output
        locked : out std_logic       -- High when clock is stable
    );
end Dcm25Mhz;

architecture Behavioral of Dcm25Mhz is

    -- Internal signals for DCM feedback and outputs
    signal clkfb : std_logic;
    signal clk0  : std_logic;
    signal clkfx : std_logic;

begin

    -- Instance of the Xilinx DCM_SP primitive
    dcm_inst : DCM_SP
        generic map (
            -- Clock Frequency Synthesis Parameters
            CLKFX_MULTIPLY => 22,
            CLKFX_DIVIDE   => 28,
            -- Input Clock Period (1 / 32MHz = 31.25 ns)
            CLKIN_PERIOD   => 31.25,
            CLK_FEEDBACK   => "1X"
        )
        port map (
            -- Clock Input and Feedback
            CLKIN    => clk,
            CLKFB    => clkfb,
            -- Control and Status
            RST      => reset,
            LOCKED   => locked,
            PSEN     => '0',
            PSINCDEC => '0',
            PSCLK    => '0',
            DSSEN    => '0',
            -- Clock Outputs
            CLK0     => clk0,
            CLKFX    => clkfx
        );

    -- Connections (similar to Verilog 'assign' statements)
    clkfb <= clk0;                  -- Feedback path for CLK0
    clk25 <= clkfx;                 -- Map the divided/multiplied clock to the output

end Behavioral;
