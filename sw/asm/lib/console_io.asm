;------------------------------------------------------------------------------------
; PUTS - Prints the null-terminated string pointed to by A0.
;------------------------------------------------------------------------------------
PUTS:
    MOVEM.L D0/A0,-(SP)
PUTS_LOOP:
    MOVE.B  (A0)+,D0        ; Get character, increment pointer
    BEQ     PUTS_END        ; Check for null terminator (0)
    BSR     PUTCHAR         ; Print the character in D0
    BRA     PUTS_LOOP       ; Loop back
PUTS_END:
    MOVEM.L (SP)+,D0/A0
    RTS                     ; Return from subroutine

;------------------------------------------------------------------------------------
; PUTCHAR - Prints the single character stored in D0 to the UART data register.
;------------------------------------------------------------------------------------
PUTCHAR:
    MOVE.L D2,-(SP)         ; Save D2
    ; Check UART status (TX Ready)
PUTCHAR_WAIT:
    MOVE.W  UART_STAT,D2    ; Read status register
    BTST    #0,D2           ; Check TX ready (Bit 0)
    BEQ     PUTCHAR_WAIT    ; Wait until TX ready
    MOVE.W  D0,UART_DATA    ; Write the character to the data register
    MOVE.L  (SP)+,D2
    RTS

;------------------------------------------------------------------------------------
; GETCHAR - Gets a single character from the UART data register and stores it in D0
;------------------------------------------------------------------------------------
GETCHAR:
    MOVE.L D2,-(SP)         ; Save D2
    ; Check UART status (RX Ready)
GETCHAR_WAIT:
    MOVE.W  UART_STAT,D2    ; Read status register
    BTST    #1,D2           ; Check RX ready (Bit 1)
    BEQ     GETCHAR_WAIT    ; Wait until RX ready
    MOVE.W  UART_DATA,D0    ; Read character to D0
    MOVE.L  (SP)+,D2
    RTS
