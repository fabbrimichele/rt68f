# SPI Master Controller
This document describes the `spi-master.vhd` module.
The module is based on [spi_master](https://www.volkerschatz.com/hardware/vhdocl-example/sources/spi-master.html) 
with some minor modification.
This version features **Manual Chip Select (CS)** control, allowing the CPU to assert and de-assert the SPI slave independently of the data transfer.

---
## Register Map

The controller occupies 4 bytes of address space.
Registers are mapped to words in the rt68f memory.

| Address Offset | Name | Description |
| :--- | :--- | :--- |
| **Base + $00** | **DL** | Data Low Register (Bits 7:0) |
| **Base + $01** | **DH** | Data High Register (Bits 15:8) |
| **Base + $02** | **CDST** | Command / Status Register |
| **Base + $03** | **CONF** | Configuration Register |

---
## Register Definitions

### DL / DH : Data Registers
* **Write:** Load the data to be transmitted.
* **Read:** Retrieve the data shifted in during the last transaction (MISO).
* Use **DL** for 4-bit and 8-bit transfers. Use both for 12-bit and 16-bit transfers.

---
### CDST : Command / Status Register
Used to control the transaction state and chip select lines.

**Write Access:**

| Bit | Name      | Description |
|:----|:----------| :--- |
| 0   | **START** | Write `1` to start the SPI transfer. Automatically clears. |
| 1   | **CS**    | Manual Chip Select. `1` = Assert (Pull Low), `0` = De-assert (Pull High). |
| 2   | **IRQEN** | `1` = Enable Interrupt Request at the end of transfer. |
| 3   | **-**     | Unused |
| 4-6 | **SPIAD** | SPI Device Address. Selects which `spi_cs_n[7:0]` line to activate. |
| 7   | **-**     | Reserved. |

**Read Access:**

| Bit | Name | Description |
| :--- | :--- | :--- |
| 0 | **BUSY** | `1` = Transfer in progress; `0` = Idle. |

---
### CONF : Configuration Register
Defines the physical bus characteristics.

**Write Access:**

| Bit | Name | Description |
| :--- | :--- | :--- |
| 0-2 | **DIVIDE** | **SPI Clock Divisor.** See table below. |
| 3-4 | **LENGTH** | **Transfer Length.** `00`=4-bit, `01`=8-bit, `10`=12-bit, `11`=16-bit. |
| 5-7 | **-** | Reserved. |

#### Clock Divisor Table (DIVIDE)
The SPI clock frequency is derived from the System Clock (`clk`).

| DIVIDE (Bits 2:0) | Divisor | Example (16MHz clk) |
| :--- | :--- | :--- |
| `000` | clk / 2 | 8.0 MHz |
| `001` | clk / 4 | 4.0 MHz |
| `010` | clk / 8 | 2.0 MHz |
| `011` | clk / 16 | 1.0 MHz |
| `100` | clk / 32 | 500 kHz |
| `101` | clk / 64 | 250 kHz |
| `110` | clk / 128 | 125 kHz |
| `111` | clk / 256 | 62.5 kHz |

---
## Programming Guide

### Typical Transaction (e.g., SD Card)
1. **Initialize:** Write to `CONF` to set speed and 8-bit length.
2. **Select Device:** Write to `CDST` with `CS = 1` and `SPIAD` set to target device.
3. **Send Byte:** - Write data to `DL`.
    - Write to `CDST` with `START = 1` and `CS = 1`.
    - Poll `CDST` bit 0 until `BUSY == 0`.
4. **Repeat:** Repeat step 3 for all bytes in the frame.
5. **Deselect Device:** Write to `CDST` with `CS = 0`.
