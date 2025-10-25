    ORG    $0000        ; Start of memory

    ; ------------------------------
    ; 68000 Vector Table (first 32 entries = 0x0000-0x007C)
    ; Each vector is 32 bits (long)
    ; ------------------------------
    DC.L   END_RAM      ; 0: Initial Stack Pointer (SP)
    DC.L   START        ; 1: Reset vector (PC start address)
    DC.L   $00000000    ; 2: Bus Error
    DC.L   $00000000    ; 3: Address Error
    DC.L   $00000000    ; 4: Illegal Instruction
    DC.L   $00000000    ; 5: Divide by Zero
    DC.L   $00000000    ; 6: CHK Instruction
    DC.L   $00000000    ; 7: TRAPV Instruction
    DC.L   $00000000    ; 8: Privilege Violation
    DC.L   $00000000    ; 9: Trace
    DC.L   $00000000    ; 10: Line 1010 Emulator
    DC.L   $00000000    ; 11: Line 1111 Emulator
    DC.L   $00000000    ; 12: Reserved
    DC.L   $00000000    ; 13: Reserved
    DC.L   $00000000    ; 14: Reserved
    DC.L   $00000000    ; 15: Reserved
    DC.L   $00000000    ; 16: Reserved
    DC.L   $00000000    ; 17: Reserved
    DC.L   $00000000    ; 18: Reserved
    DC.L   $00000000    ; 19: Reserved
    DC.L   $00000000    ; 20: TRAP0
    DC.L   $00000000    ; 21: TRAP1
    DC.L   $00000000    ; 22: TRAP2
    DC.L   $00000000    ; 23: TRAP3
    DC.L   $00000000    ; 24: TRAP4
    DC.L   $00000000    ; 25: TRAP5
    DC.L   $00000000    ; 26: TRAP6
    DC.L   $00000000    ; 27: TRAP7
    DC.L   $00000000    ; 28: TRAP8
    DC.L   $00000000    ; 29: TRAP9
    DC.L   $00000000    ; 30: TRAPA
    DC.L   $00000000    ; 31: TRAPB

    ORG    $0080        ; Start of memory
    ; ------------------------------
    ; Program code
    ; ------------------------------
START:
    LEA     MESSAGE,A0      ; A0 holds pointer to the string "Hello World!"
LOOP:
    BSR     PUTS            ; Call PUTS subroutine
    JSR     DELAY
    JMP     LOOP            ; Infinite loop

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
    ; Check UART status (TX Ready)
PUTCHAR_WAIT:
    MOVE.W  UART_STAT,D2    ; Read status register
    BTST    #0,D2           ; Check TX ready (Bit 0)
    BEQ     PUTCHAR_WAIT    ; Wait until TX ready
    ; Write character (from D0)
    MOVE.W  D0,UART_DATA    ; Write the character to the data register
    RTS

DELAY:
    MOVE.L  #DLY_VAL,D0     ; Load delay value
DLY_LOOP:
    SUBQ.L  #1,D0           ; Decrement counter
    BNE     DLY_LOOP        ; Loop until D0 is zero
    RTS


    ; ------------------------------
    ; Data Section
    ; ------------------------------
MESSAGE:
    DC.B    'Hello World!',10,0 ; String with Newline (10) and Null Terminator (0)

    ; ===========================
    ; Constants
    ; ===========================
DLY_VAL     EQU 1333333     ; Delay iterations, 1.33 million = 0.5 sec at 32MHz
END_RAM     EQU $00001000   ; End of RAM address
LED         EQU $00010000   ; LED-mapped register base address
UART_BASE   EQU $00012000   ; UART-mapped data register address
UART_DATA   EQU UART_BASE+0 ; UART-mapped data register address
UART_STAT   EQU UART_BASE+2 ; UART-mapped data register address
; NOTE: do not remove spaces around +