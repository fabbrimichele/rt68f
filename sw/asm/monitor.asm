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
    BEQ     PROCESS_CMD     ; then process command
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
    MOVE.B  #LF,D0
    BSR     PUTCHAR

    ; Parse Dump
    LEA     DUMP_STR,A1
    BSR     PARSE_CMD
    BTST    #0,D0
    BNE     DUMP_CMD        ; D0.0 = 1 execute DUMP

    ; Parse Write
    LEA     WRITE_STR,A1
    BSR     PARSE_CMD
    BTST    #0,D0
    BNE     WRITE_CMD        ; D0.0 = 1 execute WRITE

UNKNOWN_CMD:
    ; Print stored command
    MOVE.B  #'"',D0
    BSR     PUTCHAR
    LEA     IN_BUF,A0
    BSR     PUTS
    MOVE.B  #'"',D0
    BSR     PUTCHAR
    MOVE.B  #' ',D0
    BSR     PUTCHAR
    ; Print error message
    LEA     MSG_UNKNOWN,A0
    BSR     PUTS
    BRA     NEW_CMD

DUMP_CMD:
    MOVE.W  #(8-1),D1       ; Print 8 lines
DUMP_LINE:
    MOVE.L  A0,D0
    BSR     BINTOHEX        ; Print address
    MOVE.B  #':',D0
    BSR     PUTCHAR
    MOVE.W  #(8-1),D2       ; Print 8 cells
DUMP_CELL:
    MOVE.B  #' ',D0
    BSR     PUTCHAR
    MOVE.W  (A0)+,D0
    BSR     BINTOHEX_W      ; Print mem value
    DBRA    D2,DUMP_CELL    ; Decrement D1, branch if D1 is NOT -1

    MOVE.B  #LF,D0
    BSR     PUTCHAR
    DBRA    D1,DUMP_LINE    ; Decrement D1, branch if D1 is NOT -1
    BRA     NEW_CMD

WRITE_CMD:
    ; TODO: additional parameter with the value to be written
    MOVE.W  #$FFFF,(A0)
    BRA     NEW_CMD

; ------------------------------------------------------------
; CHECK_CMD
; A1: Points to the start of the command (NULL terminated)
;     to be compared to (e.g. DUMP_STR).
; Output
; D0.0: 1 if command found, 0 otherwise.
; A0: Points to character in the buffer after the command.
; ------------------------------------------------------------
CHECK_CMD:
    MOVEM.L D1/D2/D3/A1,-(SP)
    MOVE.L #1,D0
    LEA     IN_BUF,A0

CHK_CMD_LOOP:
    MOVE.B  (A1)+,D3
    CMP.B   #NUL,D3
    BEQ     CHK_CMD_DONE
    MOVE.B  (A0)+,D2
    CMP.B   D3,D2
    BNE     CHK_CMD_FAIL
    BRA     CHK_CMD_LOOP

CHK_CMD_FAIL:
    CLR.L   D0

CHK_CMD_DONE:
    MOVEM.L (SP)+,D1/D2/D3/A1
    RTS

; ------------------------------------------------------------
; CHECK_SEP
; Check for separator and skip whitespace to find argument.
; Input
; - A0: Points to character in the buffer after the command.
; Output
; - D0.0: 1 if separator found, 0 otherwise.
; - A0: Points to character in the buffer after the argument.
; ------------------------------------------------------------
CHECK_SEP:
    MOVEM.L D2,-(SP)
    MOVE.L #1,D0

    ; Check for separator (Space)
    MOVE.B  (A0),D2             ; Peek at the next character
    CMP.B   #' ',D2             ; Must be a space
    BNE     CHK_SEP_FAIL        ; Fail if not space

    ; Skip whitespace to find argument
CHK_SEP_SKIP_WS:
    CMP.B   #' ',(A0)+          ; Check for space, and advance A0
    BEQ     CHK_SEP_SKIP_WS     ; Loop while space
    SUBQ.L  #1,A0               ; A0 advanced one too far, backtrack
    BRA     CHK_SEP_DONE

CHK_SEP_FAIL:
    CLR.L   D0

CHK_SEP_DONE:
    MOVEM.L (SP)+,D2
    RTS

; ------------------------------------------------------------
; CHECK_TRAIL
; Check for trailing junk (should be called after all arguments).
; Input
; - A0: Points to character in the buffer after the command.
; Output
; - D0.0: 1 if string clean, 0 otherwise.
; ------------------------------------------------------------
CHECK_TRAIL:
    MOVEM.L D2,-(SP)
    MOVE.L #1,D0

CHK_TRL_LOOP:
    MOVE.B  (A0)+,D2            ; Peek at the character
    TST.B   D2                  ; Is it NULL?
    BEQ     CHK_TRL_DONE        ; End of line, SUCCESS
    CMP.B   #' ',D2             ; Is it a space?
    BNE     CHK_TRL_FAIL        ; If it's *anything else* (like 'X' in 'C000X'), it's junk.
    BRA     CHK_TRL_LOOP        ; continue until end of line

CHK_TRL_FAIL:
    CLR.L   D0

CHK_TRL_DONE:
    MOVEM.L (SP)+,D2
    RTS

; --------------------------------------
; PARSE_CMD: Checks for command and extracts address argument
; A1: Points to the start of the command (NULL terminated) to be compared to (e.g. DUMP_STR)
; Output D0.0: 1 if command found and address parsed, 0 otherwise.
; Output A0: If successful, contains the 32-bit starting address for the dump.
; --------------------------------------d
PARSE_CMD:
    MOVEM.L D1/D2/D3/A1,-(SP)

    JSR     CHECK_CMD           ; Chek expected command
    BTST    #0,D0               ; D0.0 equals 0, failure
    BEQ     CHECK_DONE          ; Exit on failure

    JSR     CHECK_SEP           ; Check for separator
    BTST    #0,D0               ; D0.0 equals 0, failure
    BEQ     CHECK_DONE          ; Exit on failure

    BSR     HEXTOBIN            ; Parse 1st parameter (32 bits)
    BTST    #0,D0               ; D0.0 equals 0, failure
    BEQ     CHECK_DONE          ; Exit on failure
    ; TODO: don't use A0 to return the address, rather use A1
    MOVE.L  D1,A1               ; Move the final address from D0 into A0

    JSR     CHECK_TRAIL         ; Check for trailing junk
    ; TODO: shouldn't be required once HEX2BIN is fixed
    BTST    #0,D0               ; D0.0 equals 0, failure
    BEQ     CHECK_DONE          ; Exit on failure

    ; TODO: shouldn't be required once HEX2BIN is fixed
    BSET    #0,D0               ; Set D0.0 flag to TRUE
    BRA     CHECK_DONE

    ; TODO: shouldn't be required once HEX2BIN is fixed
CHECK_FAIL:
    CLR.L   D0

CHECK_DONE:
    MOVE.L  A1,A0               ; TODO: shouldn't be required once HEX2BIN is fixed
    MOVEM.L (SP)+,D1/D2/D3/A1
    RTS

; ------------------------------
; Subroutines
; ------------------------------
    INCLUDE 'lib/console_io.asm'
    INCLUDE 'lib/conversions.asm'

; ------------------------------
; ROM Data Section
; ------------------------------

; Messages
MSG_TITLE   DC.B    'RT68F Monitor v0.1',LF,NUL
MSG_UNKNOWN DC.B    'Error: Unknown command or syntax',LF,NUL

; Commands
; They must be null terminated
DUMP_STR    DC.B    'DUMP',NUL
WRITE_STR   DC.B    'WRITE',NUL

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

; ASCII
CR          EQU 13          ; Carriage Return
LF          EQU 10          ; Line Feed
BEL         EQU 7           ; Bell character
BS          EQU 8           ; Backspace
DEL         EQU 127         ; Delete/Rubout (0x7F)
NUL         EQU 0

; Buffer
IN_BUF          EQU RAM_START           ; IN_BUF starts at 0x800
IN_BUF_LEN      EQU 80
IN_BUF_END      EQU IN_BUF+IN_BUF_LEN   ; IN_BUF_END = 0x800 + 80 = 0x850
