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


    ; ===========================
    ; Program code
    ; ===========================
    ORG    $0080            ; Start of memory

START:

LOOP:
    JSR     DELAY           ; Call delay
    MOVE.W  (KEY),D1        ; Read keyboard
    MOVE.W  D1,(LED)        ; LED feedback
    CMP.W   #1,D1           ;
    BEQ     IS_UP           ;
    CMP.W   #2,D1           ;
    BEQ     IS_LEFT        ;
    CMP.W   #4,D1           ;
    BEQ     IS_DOWN         ;
    CMP.W   #8,D1           ;
    BEQ     IS_RIGHT        ;
    JMP     LOOP            ;
IS_UP:
    MOVE.W  #'U',(UART)     ; Write 'U' to UART data register
    JMP     LOOP            ; Infinite loop
IS_LEFT:
    MOVE.W  #'L',(UART)     ; Write 'L' to UART data register
    JMP     LOOP            ; Infinite loop
IS_RIGHT:
    MOVE.W  #'R',(UART)     ; Write 'R' to UART data register
    JMP     LOOP            ; Infinite loop
IS_DOWN:
    MOVE.W  #'D',(UART)     ; Write 'R' to UART data register
    JMP     LOOP            ; Infinite loop

DELAY:
    MOVE.L  #DLY_VAL,D0     ;
DLY_LOOP:
    SUBQ.L  #1,D0           ; 4 cycles
    BNE     DLY_LOOP        ; 10 cycles when taken
    RTS


    ; ===========================
    ; Constants
    ; ===========================
DLY_VAL     EQU     1333333     ; Delay iterations, 1.33 million = 0.5 sec at 32MHz
END_RAM     EQU     $00001000   ; End of RAM address
LED         EQU     $00010000   ; LED-mapped register base address
KEY         EQU     $00011000   ; Key-mapped register base address
UART        EQU     $00012000   ; UART register base address

