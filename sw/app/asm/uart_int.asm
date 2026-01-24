; ===========================
; Program code
; ===========================
    ORG    $400             ; Start of RAM

; UART_IER:
; B7-4 : unused
; BIT-3: modem receive interrupt
; BIT-2: receive line status interrupt
; BIT-1: transmit holding register
; BIT-0: receive holding register

START:
    MOVE.L  #UART_ISR,VT_INT_4  ; Set interrupt handler
    MOVE.B  #$01,UART_IER	    ; Enable interrupt on receive holding register
    AND.W   #$F8FF,SR           ; Enable all interrupts on 68000 (Clear mask bits)
LOOP:
    CMP.B   #'Q',CHAR
    BNE     LOOP
    MOVE.B  #$00,UART_IER	    ; Disable interrupt on receive holding register
    AND.W   #$FFFF,SR           ; Disable all interrupts on 68000 (Clear mask bits)
    TRAP    #14

UART_ISR:
    MOVEM.L D0,-(SP)            ; Save D0
    MOVE.W  UART_IIR,D0         ; Read interrupt status register (and ack interrupt)
    CMP.W   #4,D0               ; Received Data Ready (0100)
    BEQ     .READ
    CMP.W   #2,D0               ; Transmitter Holding Register Ready (0010)
    BEQ     .WRITE
    BNE     .RET
.READ:
    MOVE.W  UART_RBR,D0         ; Read character to D0
    MOVE.W  D0,LED
    MOVE.B  D0,CHAR
    BNE     .RET
.WRITE:
.RET:
    MOVEM.L (SP)+,D0            ; Restore D0
    RTE                         ; Return from int

; Variables
CHAR        DS.B    1

; ===========================
; Include files
; ===========================
    INCLUDE '../../lib/asm/isr_vector.asm'
    INCLUDE '../../lib/asm/uart_16450.asm'
    INCLUDE '../../lib/asm/led.asm'

