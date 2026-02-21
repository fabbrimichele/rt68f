;------------------------------------------------------------------------------------
; 16450 UART
;------------------------------------------------------------------------------------
UART_BASE       EQU $00401000               ; UART base address
UART_RBR        EQU UART_BASE+$0            ; Receive Buffer Register(RBR) / Transmitter Holding Register(THR) / Divisor Latch (LSB)
UART_IER        EQU UART_BASE+$2            ; Interrupt enable register / Divisor Latch (MSB)
UART_IIR        EQU UART_BASE+$4            ; Interrupt Identification Register
UART_LCR        EQU UART_BASE+$6            ; Line control register
UART_MCR        EQU UART_BASE+$8            ; MODEM control register
UART_LSR        EQU UART_BASE+$A            ; Line status register
UART_MSR        EQU UART_BASE+$C            ; MODEM status register
; NOTE: do not remove spaces around +
