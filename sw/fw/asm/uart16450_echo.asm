; ------------------------------
; 68000 Vector Table (first 32 entries = 0x0000-0x007C)
; Each vector is 32 bits (long)
; ------------------------------
    ORG    $0000            ; Start of memory
    DC.L   SP_START         ; 0: Initial Stack Pointer (SP)
    DC.L   START            ; 1: Reset vector (PC start address)
    DC.L   $00000000        ; 2: Bus Error
    DC.L   $00000000        ; 3: Address Error
    DC.L   $00000000        ; 4: Illegal Instruction
    DC.L   $00000000        ; 5: Divide by Zero
    DC.L   $00000000        ; 6: CHK Instruction
    DC.L   $00000000        ; 7: TRAPV Instruction
    DC.L   $00000000        ; 8: Privilege Violation
    DC.L   $00000000        ; 9: Trace
    DC.L   $00000000        ; 10: Line 1010 Emulator
    DC.L   $00000000        ; 11: Line 1111 Emulator

    ORG    $0080            ; TRAP #0~15
    DC.L   $00000000        ; 32: TRAP0
    DC.L   $00000000        ; 33: TRAP1
    DC.L   $00000000        ; 34: TRAP2
    DC.L   $00000000        ; 35: TRAP3
    DC.L   $00000000        ; 36: TRAP4
    DC.L   $00000000        ; 37: TRAP5
    DC.L   $00000000        ; 38: TRAP6
    DC.L   $00000000        ; 39: TRAP7
    DC.L   $00000000        ; 40: TRAP8
    DC.L   $00000000        ; 41: TRAP9
    DC.L   $00000000        ; 42: TRAP10
    DC.L   $00000000        ; 43: TRAP11
    DC.L   $00000000        ; 44: TRAP12
    DC.L   $00000000        ; 45: TRAP13
    DC.L   $00000000        ; 46: TRAP14
    DC.L   $00000000        ; 47: TRAP15

; ------------------------------
; Program code
; ------------------------------
    ORG    $0400            ; Start of memory
START:
    JSR     UART_INIT
    LEA     MSG_READY,A0    ; A0 holds pointer to the message
    BSR     PUTS            ; Call PUTS subroutine
LOOP:
    BSR     GETCHAR
    BSR     PUTCHAR
    BRA     LOOP


    ; ------------------------------
    ; Subroutines
    ; ------------------------------

    ; PUTS - Prints the null-terminated string pointed to by A0.
PUTS:
    MOVE.L  A0,-(SP)        ; Save the current string pointer (A0)
PUTS_LOOP:
    MOVE.B  (A0)+,D0        ; Get character, increment pointer
    BEQ     PUTS_END        ; Check for null terminator (0)
    BSR     PUTCHAR         ; Print the character in D0
    BRA     PUTS_LOOP       ; Loop back
PUTS_END:
    MOVE.L  (SP)+,A0        ; Restore the string pointer for the next loop iteration (A0)
    RTS                     ; Return from subroutine

    ; PUTCHAR - Prints the single character stored in D0 to the UART data register.
PUTCHAR:
    ; TODO: Don't access directly memory with BTST
    ;       it won't work, not sure if it suppose
    ;       to work like that (I don't think so) or
    ;       if it's a bug.
    MOVE.W  UART_LSR,D2     ; Read status register
	BTST    #5,D2   		; write buffer empty?
	BEQ     PUTCHAR 		; eq 0, not ready, check again
	MOVE.B  D0,UART_RBR		; write D0 to serial
	RTS						; return

    ; GETCHAR - Gets a single character from the UART data register and stores it in D0
GETCHAR:
    ; Check UART status (RX Ready)
GETCHAR_WAIT:
    MOVE.W  UART_LSR,D2     ; Read status register
    BTST    #0,D2           ; read full?
    BEQ     GETCHAR_WAIT    ; Wait until RX ready
    ; Read character (to D0)
    MOVE.W  UART_RBR,D0     ; Read character to D0
    RTS

; Serial initialization 19200 baud, 8N1
UART_INIT:
	MOVE.B #$80,UART_LCR	; select DLAB = 1, to access the Divisor Latches of the Baud Generator
	MOVE.B #$00,UART_IER	; set divisor MSB to 0
	MOVE.B #52,UART_RBR     ; set divisor LSB to 52: 16MHz/16/52 = 19230 (should be 19200)
	MOVE.B #$00,UART_LCR	; select DLAB = 0
	MOVE.B #$03,UART_LCR	; set options to 8N1
	MOVE.B #$00,UART_IER	; disable interrupt
	RTS						;

; ------------------------------
; Data Section
; ------------------------------
MSG_READY:
    DC.B    'Ready.',10,0 ; String with Newline (10) and Null Terminator (0)

; ===========================
; Constants
; ===========================
MON_MEM_LEN EQU 256                     ; RAM allocated for the monitor

; Memory Map
RAM_START       EQU $00004000               ; Start of RAM address
RAM_END         EQU $00008000               ; End of RAM address (+1)
SP_START        EQU (RAM_END-MON_MEM_LEN)   ; After SP, allocates monitor RAM
MON_MEM_START   EQU SP_START                ;
LED             EQU $10000                  ; LED-mapped register base address
UART_RBR        EQU $12000                  ; Receive Buffer Register(RBR) / Transmitter Holding Register(THR) / Divisor Latch (LSB)
UART_IER        EQU $12002                  ; Interrupt enable register / Divisor Latch (MSB)
UART_IIR        EQU $12004                  ; Interrupt Identification Register
UART_LCR        EQU $12006                  ; Line control register
UART_MCR        EQU $12008                  ; MODEM control register
UART_LSR        EQU $1200A                  ; Line status register
UART_MSR        EQU $1200C                  ; MODEM status register

DLY_VAL     EQU     1333333     ; Delay iterations, 1.33 million = 0.5 sec at 32MHz
