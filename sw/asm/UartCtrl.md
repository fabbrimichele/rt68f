# SpinalHDL `UartCtrl` Register Map

This document provides the register map for the SpinalHDL `UartCtrl` when mapped via a `BusSlaveFactory` (e.g., APB3 or Wishbone). It is based on the `UartCtrl.scala` source provided.

### TODO: to be verified!

---

## 1. Address Map (32-bit bus)

| Address | Register      | R/W | Description                     |
| ------- | ------------- | --- | ------------------------------- |
| 0x00    | DATA          | R/W | TX write / RX read data (FIFO)  |
| 0x04    | STATUS        | R/W | Interrupt enable + status flags |
| 0x08    | CLOCK_DIVIDER | R/W | Baud rate divider               |
| 0x0C    | FRAME_CONFIG  | R/W | Data length, parity, stop bits  |
| 0x10    | MISC          | R/W | Errors, break detection/control |

---

## 2. DATA Register (0x00)

* **Write:** Send a byte to TX FIFO
* **Read:** Receive a byte from RX FIFO
* **Read flags:**

    * Bit 16: RX valid flag (for 32-bit bus)
    * Bit 15: RX valid flag (for 16-bit bus)

---

## 3. STATUS / INTERRUPT Register (0x04)

| Bit                            | Name           | R/W | Description                 |
| ------------------------------ | -------------- | --- | --------------------------- |
| 0                              | writeIntEnable | R/W | Enable TX interrupt         |
| 1                              | readIntEnable  | R/W | Enable RX interrupt         |
| 8                              | writeInt       | R   | TX FIFO empty & enabled     |
| 9                              | readInt        | R   | RX data available & enabled |
| 15                             | TX valid flag  | R   | TX FIFO not empty           |
| 24–31 (32-bit) / 8–15 (16-bit) | FIFO occupancy | R   | Number of bytes in RX FIFO  |

---

## 4. CLOCK_DIVIDER Register (0x08)

* Width: `g.clockDividerWidth` (usually 12 or 20 bits)
* Value = `(Fclk / baudrate / rxSamplePerBit) - 1`
* Controls both TX and RX baud timing

---

## 5. FRAME_CONFIG Register (0x0C)

| Field      | Bits                         | Description             |
| ---------- | ---------------------------- | ----------------------- |
| dataLength | [7:0]                        | Number of data bits - 1 |
| parity     | [15:8]                       | 0=None, 1=Even, 2=Odd   |
| stop       | [16] (32-bit) / [0] (16-bit) | 0=1 stop, 1=2 stop bits |

---

## 6. MISC / ERROR / BREAK Register (0x10)

| Bit | Name              | R/W            | Description          |
| --- | ----------------- | -------------- | -------------------- |
| 0   | readError         | R/Clear-on-Set | Framing/parity error |
| 1   | readOverflowError | R/Clear-on-Set | RX FIFO overflow     |
| 8   | readBreak         | R              | Direct break detect  |
| 9   | breakDetected     | R/Clear-on-Set | Latched break detect |
| 10  | doBreak           | W/Set-on-Set   | Assert TX break      |
| 11  | clearBreak        | W/Clear-on-Set | Clear TX break       |

---

## Notes

* The register map assumes 32-bit bus width; for 16-bit buses, some bits are shifted (see comments above).
* `CLOCK_DIVIDER` and `FRAME_CONFIG` may be read-only or read/write depending on the `UartCtrlMemoryMappedConfig` settings.
* DATA register streams are buffered with configurable FIFO depths.
* Interrupts are generated based on `readIntEnable` and `writeIntEnable` and FIFO occupancy.

---

This document can be used as a reference for firmware programming against a SpinalHDL `UartCtrl` peripheral.
