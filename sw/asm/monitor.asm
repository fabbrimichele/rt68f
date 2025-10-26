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
; - Entering "DUMP W" should fail, but it doesn't.
;   Instead "DUMP WW" fails as expected.
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
    MOVE.W  A0,LED        ; DEBUG

    BRA     NEW_CMD

; --------------------------------------
; CHECK_DUMP: Checks for 'DUMP' command and extracts address argument
; A0: Points to the start of the command string (IN_BUF)
; Output D0.0: 1 if 'DUMP' found and address parsed, 0 otherwise.
; Output A0: If successful, contains the 32-bit starting address for the dump.
; --------------------------------------d
PARSE_DUMP:
    MOVEM.L D1/D2/A1,-(SP)      ; SAVED: D1, D2, A1

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

    ; 2. Check for separator (Space)
    MOVE.B  (A0),D2             ; Peek at the next character
    CMP.B   #' ',D2             ; Must be a space
    BNE     CHECK_FAIL           ; Fail if not space

    ; 3. Skip whitespace to find argument
SKIP_WS:
    CMP.B   #' ',(A0)+          ; Check for space, and advance A0
    BEQ     SKIP_WS             ; Loop while space
    SUBQ.L  #1,A0               ; A0 advanced one too far, backtrack

    ; 4. Call Hex to Binary conversion
    ; A0 points to the start of the hex address (e.g., 'C000')
    BSR     HEXTOBIN            ; Result in D0.L (address), Success Flag in D1.0

    ; --- Check HEXTOBIN result ---
    BTST    #0,D1               ; CHECK THE SUCCESS FLAG (D1.0)
    BEQ     CHECK_FAIL          ; Failure if illegal char/empty string

    ; --- Check for trailing junk ---
    ; A0 is currently pointing to the next character after the hex token.
    MOVE.B  (A0),D2             ; Peek at the character
    TST.B   D2                  ; Is it NULL?
    BEQ     HTB_CHECK_END       ; End of line, SUCCESS
    CMP.B   #' ',D2             ; Is it a space?
    BNE     CHECK_FAIL          ; If it's *anything else* (like 'X' in 'C000X'), it's junk.

HTB_CHECK_END:
    MOVE.L  D0,A0               ; Move the final address from D0 into A0
    BSET    #0,D0               ; Set D0.0 flag to TRUE
    BRA     CHECK_DONE

CHECK_FAIL:
    CLR.L   D0

CHECK_DONE:
    MOVEM.L (SP)+,D1/D2/A1      ; RESTORED: D1, D2, A1
    RTS

; ------------------------------
; Subroutines
; ------------------------------
    INCLUDE 'lib/console_io.asm'
    INCLUDE 'lib/conversions.asm'

DELAY:
    MOVE.L  #DLY_VAL,D0     ; Load delay value
DLY_LOOP:
    SUBQ.L  #1,D0           ; Decrement counter
    BNE     DLY_LOOP        ; Loop until D0 is zero
    RTS

; ------------------------------
; ROM Data Section
; ------------------------------

; Messages
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
