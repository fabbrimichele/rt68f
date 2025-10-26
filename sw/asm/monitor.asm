    ORG    $0000        ; Start of memory

; ------------------------------
; 68000 Vector Table (first 32 entries = 0x0000-0x007C)
; Each vector is 32 bits (long)
; ------------------------------
    DC.L   RAM_END      ; 0: Initial Stack Pointer (SP)
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

;-------------------------------
; Bugs:
; - I can't hear the Bell when at the end of the buffer
;   anymore, it used to work.
;-------------------------------


; ------------------------------
; Program code
; ------------------------------
    ORG    $0080            ; Start of memory
START:
    LEA     MSG_TITLE,A0
    BSR     PUTS

NEW_CMD:
    LEA     IN_BUF,A5       ; A5 = current buffer position
    MOVE.B  #LF,D0
    BSR     PUTCHAR
    MOVE.B  #'>',D0
    BSR     PUTCHAR
LOOP:
    BSR     GETCHAR

    CMP.B   #CR,D0          ; Check for Enter
    BEQ     PROCESS_CMD     ; then start reading new command
    CMP.B   #BS,D0          ; Check for Backspace
    BEQ     BS_HANDLER
    CMP.B   #DEL,D0          ; Check for Backspace
    BEQ     BS_HANDLER

    CMP.L   #IN_BUF_END,A5  ; Check if buffer is full
    BEQ     BUFFER_FULL

    BSR     PUTCHAR         ; print character
    MOVE.B  D0,(A5)+        ; Store D0 into buffer, then increment A5
    BRA     LOOP

; --------------------------------------
; Backspace Handler
; --------------------------------------
BS_HANDLER:
    ; 1. Check if the buffer is empty
    CMP.L   #IN_BUF,A5          ; Compare current pointer (A5) to start of buffer
    BEQ     LOOP                ; If A5 == IN_BUF, buffer is empty (do nothing)

    ; 2. Correct the buffer pointer
    SUBQ.L  #1,A5               ; Decrement A5: move pointer back one position

    ; 3. Correct the terminal display (echo the standard sequence)
    ; Send BS (0x08) to move cursor left
    MOVE.B  #BS,D0
    BSR     PUTCHAR

    ; Send Space (0x20) to erase the character
    MOVE.B  #' ',D0
    BSR     PUTCHAR

    ; Send BS (0x08) again to move cursor back to erased position
    MOVE.B  #BS,D0
    BSR     PUTCHAR

    BRA     LOOP                ; Continue input loop

; --------------------------------------
; Buffer Full Handler
; --------------------------------------
BUFFER_FULL:
    ; Send BEL (7) once to alert the user that the buffer is full
    MOVE.B  #BEL,D0
    BSR     PUTCHAR

    BSR     GETCHAR             ; Get the next character
    ; Check 1: Enter pressed (CR)
    CMP.B   #CR,D0
    BEQ     PROCESS_CMD         ; Yes, go process the command
    ; Check 2: Backspace or Delete
    CMP.B   #BS,D0
    BEQ     BS_HANDLER

    ; Discard all other input
    BRA     BUFFER_FULL

PROCESS_CMD:
    MOVE.B  #0,(A5)            ; Null-terminate the string in the buffer

    ; DEBUG: Print the buffer
    MOVE.B  #LF,D0
    BSR     PUTCHAR
    MOVE.B  #'"',D0
    BSR     PUTCHAR
    LEA     IN_BUF,A0
    BSR     PUTS
    MOVE.B  #'"',D0
    BSR     PUTCHAR
    MOVE.B  #LF,D0
    BSR     PUTCHAR

    ; Parse Dump
    BSR     PARSE_DUMP
    BTST    #0,D0
    BNE     DUMP_CMD        ; D0.0 = 1 execute DUMP

UNKNOWN_CMD:
    LEA     MSG_UNKNOWN,A0
    BSR     PUTS
    MOVE.B  #LF,D0
    BSR     PUTCHAR
    BRA     NEW_CMD

DUMP_CMD:
    MOVE.B  #'+',D0
    BSR     PUTCHAR
    MOVE.B  #LF,D0
    BRA     NEW_CMD

; --------------------------------------
; CHECK_DUMP: Checks for 'DUMP' command and extracts address argument
; A0: Points to the start of the command string (IN_BUF)
; Output D0.0: 1 if 'DUMP' found and address parsed, 0 otherwise.
; Output A0: If successful, contains the 32-bit starting address for the dump.
; --------------------------------------d
PARSE_DUMP:
    MOVEM.L D1/D2/A0/A1,-(SP)      ; SAVED: D1, D2, A1

    CLR.L   D0
    MOVEQ   #DUMP_LEN-1,D1
    LEA     IN_BUF,A0
    LEA     DUMP_STR,A1

    ; 1. Compare 'DUMP'
CHECK_LOOP:
    MOVE.B  (A0)+,D2
    CMP.B   (A1)+,D2
    BNE     CHECK_FAIL
    DBRA    D1,CHECK_LOOP       ; Decrement D1, loop if not -1

    BSET    #0,D0               ; Set D0.0 flag to TRUE
    BRA     CHECK_DONE

CHECK_FAIL:
    CLR.L   D0

CHECK_DONE:
    MOVEM.L (SP)+,D1/D2/A0/A1      ; RESTORED: D1, D2, A1
    RTS

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

    ; GETCHAR - Gets a single character from the UART data register and stores it in D0
GETCHAR:
    ; Check UART status (RX Ready)
GETCHAR_WAIT:
    MOVE.W  UART_STAT,D2    ; Read status register
    BTST    #1,D2           ; Check RX ready (Bit 1)
    BEQ     GETCHAR_WAIT    ; Wait until RX ready
    ; Read character (to D0)
    MOVE.W  UART_DATA,D0    ; Read character to D0
    RTS

DELAY:
    MOVE.L  #DLY_VAL,D0     ; Load delay value
DLY_LOOP:
    SUBQ.L  #1,D0           ; Decrement counter
    BNE     DLY_LOOP        ; Loop until D0 is zero
    RTS

; ------------------------------
; ROM Data Section
; ------------------------------
MSG_TITLE   DC.B    'RT68F Monitor v0.1',LF,NUL
MSG_UNKNOWN DC.B    'Error: Unknown command or syntax',LF,NUL
; Commands
DUMP_STR    DC.B    'DUMP',0
DUMP_LEN    EQU     4

; ===========================
; Constants
; ===========================
; Memory Map
RAM_START   EQU $00000800   ; Start of RAM address
RAM_END     EQU $00001000   ; End of RAM address
LED         EQU $00010000   ; LED-mapped register base address
UART_BASE   EQU $00012000   ; UART-mapped data register address
UART_DATA   EQU UART_BASE+0 ; UART-mapped data register address
UART_STAT   EQU UART_BASE+2 ; UART-mapped data register address
; NOTE: do not remove spaces around +

; Program Constants
DLY_VAL     EQU 1333333     ; Delay iterations, 1.33 million = 0.5 sec at 32MHz
CR          EQU 13          ; Carriage Return
LF          EQU 10          ; Line Feed
BEL         EQU 7           ; Bell character
BS          EQU 8           ; Backspace
DEL         EQU 127         ; Delete/Rubout (0x7F)
NUL         EQU 0

IN_BUF          EQU RAM_START           ; IN_BUF starts at 0x800
IN_BUF_LEN      EQU 80
IN_BUF_END      EQU IN_BUF+IN_BUF_LEN   ; IN_BUF_END = 0x800 + 80 = 0x850
