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

; Note:
; Don't test it with CuteCom, it has a confusing UI and
; you might think your writing to the serial, instead your
; just writing the output of the program.
; Use GTKTerm instead!
START:
    ; --- Send initial prompt '>' ---
    MOVE.W  #'>',UART_DATA      ; write prompt

    ; TODO: it somehow works, but it's extreamly slow!
    ;       should I try to wait also for TX?
    ;       I should probably use the interrupts
WAIT_RX:
    MOVE.W  UART_DATA,D0        ; read RX register
    BPL     WAIT_RX             ; Branch to WAIT_RX if it is a positive number
    ;                             ; Bit test doesn't work, not sure why

    ;MOVE.W  UART_STATUS,D0      ; read status flags
    ;BTST    #9,D0               ; bit 9 = RX data available
    ;BEQ     WAIT_RX
    ;MOVE.W  UART_DATA,D0        ; read received byte

    ANDI.W  #$00FF,D0           ; extract received byte

    ; --- Display on LEDs ---
    MOVE.W  D0,LED

    ; --- Echo ---
WAIT_TX:
    MOVE.W  UART_STATUS,D1     ; read status register
    BTST    #15,D1             ; test TX ready flag
    BEQ     WAIT_TX            ; loop until TX is ready

    MOVE.W  D0,UART_DATA        ; echo back received char
    BRA.S   WAIT_RX

DELAY:
    MOVE.L  #DLY_VAL,D7     ;
DLY_LOOP:
    SUBQ.L  #1,D7           ; 4 cycles
    BNE     DLY_LOOP        ; 10 cycles when taken
    RTS

    ; ===========================
    ; Constants
    ; ===========================
DLY_VAL     EQU  13333 ;33         ; Delay iterations, 1.33 million = 0.5 sec at 32MHz
END_RAM     EQU  $00001000       ; End of RAM address
LED         EQU  $00010000       ; LED-mapped register base address
KEY         EQU  $00011000       ; Key-mapped register base address

UART_BASE    EQU $00012000
UART_DATA    EQU UART_BASE + 0   ; RX/ TX data + valid bit
UART_STATUS  EQU UART_BASE + 4   ; Status register

; See UartCtrl.md for the full documentation
