;------------------------------------------------------------------------------------
; 16450 UART
;------------------------------------------------------------------------------------
UART_BASE       EQU $00402000               ; UART base address
UART_RBR        EQU UART_BASE+$0            ; Receive Buffer Register(RBR) / Transmitter Holding Register(THR) / Divisor Latch (LSB)
UART_IER        EQU UART_BASE+$2            ; Interrupt enable register / Divisor Latch (MSB)
UART_IIR        EQU UART_BASE+$4            ; Interrupt Identification Register
UART_LCR        EQU UART_BASE+$6            ; Line control register
UART_MCR        EQU UART_BASE+$8            ; MODEM control register
UART_LSR        EQU UART_BASE+$A            ; Line status register
UART_MSR        EQU UART_BASE+$C            ; MODEM status register
; NOTE: do not remove spaces around +

;------------------------------------------------------------------------------------
; Serial initialization 19200 baud, 8N1
; Note: when changing this file a `make clean` is required.
;------------------------------------------------------------------------------------
UART_INIT:
	MOVE.B  #$80,UART_LCR	; select DLAB = 1, to access the Divisor Latches of the Baud Generator
	MOVE.B  #$00,UART_IER	; set divisor MSB to 0
	;MOVE.B #52,UART_RBR     ; set divisor LSB to 52: 16MHz/16/52 = 19230 (should be 19200)
	;MOVE.B #26,UART_RBR     ; set divisor LSB to 26: 16MHz/16/26 = 38461 (should be 38400)
	MOVE.B  #13,UART_RBR     ; set divisor LSB to 13: 16MHz/16/13 = 76923 (should be 76800, it's not common, but it seems to work)
	MOVE.B  #$00,UART_LCR	; select DLAB = 0
	MOVE.B  #$03,UART_LCR	; set options to 8N1
	MOVE.B  #$00,UART_IER	; disable interrupt
	RTS						;

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
    ; TODO: Don't access directly memory with BTST
    ;       it won't work, not sure if it suppose
    ;       to work like that (I don't think so) or
    ;       if it's a bug.
    MOVE.W  UART_LSR,D2     ; Read status register
    BTST    #5,D2           ; Check TX ready (Bit 5)
    BEQ     PUTCHAR_WAIT    ; Wait until TX ready
    MOVE.B  D0,UART_RBR     ; Write the character to the data register
    MOVE.L  (SP)+,D2
    RTS

;------------------------------------------------------------------------------------
; GETCHAR - Gets a single character from the UART data register and stores it in D0
;------------------------------------------------------------------------------------
GETCHAR:
    MOVE.L D2,-(SP)         ; Save D2
    ; Check UART status (RX Ready)
GETCHAR_WAIT:
    ; TODO: Don't access directly memory with BTST
    ;       it won't work, not sure if it suppose
    ;       to work like that (I don't think so) or
    ;       if it's a bug.
    MOVE.W  UART_LSR,D2     ; Read status register
    BTST    #0,D2           ; Check RX ready (Bit 1)
    BEQ     GETCHAR_WAIT    ; Wait until RX ready
    MOVE.W  UART_RBR,D0     ; Read character to D0
    MOVE.L  (SP)+,D2
    RTS
