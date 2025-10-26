; --------------------------------------
; BINTOHEX: Compliant version. D0.L is converted to 8 ASCII hex characters.
; --------------------------------------
BINTOHEX:
    MOVEM.L D1/D2,-(SP)         ; Save D1, D2

    MOVEQ   #7,D2               ; Loop Counter (8 nibbles - 1 = 7)

BTH_LOOP:
    MOVE.L  D0,D1               ; Copy D0 to D1 for processing

    ; --- 1. Isolate the Most Significant Nibble (MSN) into D1.L nibble 0 ---
    ; Goal: Move MSN (bits 31-28) to LSN (bits 3-0). Total shift needed: 28.
    ; This is done by SWAP (16-bit) + two small shifts (7+5=12)

    SWAP    D1                  ; D1 = [original Low Word] [original High Word]
    ; MSN is now in the high nibble of the Low Word (bits 15-12).

    LSR.W   #7,D1               ; Shift right by 7. (MSN moves to bits 8-5)
    LSR.W   #5,D1               ; Shift right by 5. (MSN moves to bits 3-0)
    ; Total shift = 16 (SWAP) + 7 + 5 = 28. Valid shifts used.

    ANDI.L  #$0000000F,D1       ; Mask D1, leaving only the 4-bit value (0-15).

    ; --- 2. Convert value (0-15) to ASCII ('0'-'9', 'A'-'F') ---
    CMP.B   #10,D1
    BLT.S   BTH_DIGIT_CONV
    ADDI.B  #'A'-10,D1
    BRA.S   BTH_PRINT

BTH_DIGIT_CONV:
    ADDI.B  #'0',D1

BTH_PRINT:
    ; --- 3. Display Character and Restore D0 (The robust fix) ---
    MOVE.L  D0,-(SP)            ; SAFELY PUSH D0.L (THE VALUE) TO STACK

    MOVE.B  D1,D0               ; Pass the ASCII char in D0.B for PUTCHAR
    BSR     PUTCHAR             ; Display the character

    MOVE.L  (SP)+,D0            ; RESTORE D0.L (THE VALUE) from stack

    ; --- 4. Consume the MSN and prepare for the next nibble ---
    ASL.L   #4,D0               ; Shift Left D0 by 4 bits to remove the displayed MSN.

    DBRA    D2,BTH_LOOP         ; Loop 8 times

    MOVEM.L (SP)+,D1/D2          ; Restore D1, D2
    RTS


; --------------------------------------
; HEXTOBIN: Converts hex string at A0 to binary (32-bit) in D0.
; --------------------------------------
HEXTOBIN:
    MOVEM.L D3/D1/D2/A1,-(SP)   ; Save D3, D1, D2, A1

    MOVEQ   #8,D2               ; D2 = Loop counter (Max digits = 8)
    CLR.L   D0                  ; D0 = Result (cleared)
    CLR.L   D1                  ; D1 = Success Flag (0=Failure initially)
    MOVEQ   #0,D3               ; D3 = Digit Counter (must be zero)

NEXT_DIGIT:
    MOVE.B  (A0),D1             ; D1.B = Peek at next char

    ; Check for end of token (Space or NULL are delimiters, but NOT illegal)
    TST.B   D1                  ; Is it NULL?
    BEQ     HTB_TOKEN_END
    CMP.B   #' ',D1             ; Is it a Space?
    BEQ     HTB_TOKEN_END

    ; --- 1. Character Classification ---

    ; Check 0-9
    CMP.B   #'0',D1
    BLT     CHECK_A
    CMP.B   #'9',D1
    BLE     HTB_DIGIT_FOUND

CHECK_A:
    ; Check A-F (Uppercase)
    CMP.B   #'A',D1
    BLT     CHECK_a
    CMP.B   #'F',D1
    BLE     HTB_DIGIT_FOUND

CHECK_a:
    ; Check a-f (Lowercase)
    CMP.B   #'a',D1
    BLT     HTB_ILLEGAL_CHAR    ; Illegal character, stop parsing
    CMP.B   #'f',D1
    BLE     HTB_DIGIT_FOUND

    BRA     HTB_ILLEGAL_CHAR

HTB_DIGIT_FOUND:
    ; Check 8-digit limit
    DBRA    D2,HTB_CALC         ; If D2 > 0, we can process.

    ; If DBRA falls through, 8 digits were processed (D2 is -1). Stop parsing.
    BRA     HTB_TOKEN_END

    ; --- 2. Conversion and Arithmetic ---
HTB_CALC:
    ADDQ.L  #1,D3               ; Increment digit counter (D3 > 0 means success possible)

    ; Conversion logic to get 4-bit value in D1
    CMP.B   #'0',D1
    BLT     ALPHA_CALC
    CMP.B   #'9',D1
    BLE     DIGIT_CALC

ALPHA_CALC:
    CMP.B   #'A',D1
    BLT     ALPHA_LOWER_CALC
    SUB.B   #'A'-10,D1
    BRA     HTB_SHIFT

ALPHA_LOWER_CALC:
    SUB.B   #'a'-10,D1
    BRA     HTB_SHIFT

DIGIT_CALC:
    SUB.B   #'0',D1

HTB_SHIFT:
    LSL.L   #4,D0               ; Shift result left
    OR.B    D1,D0               ; OR in new nibble

    ADDQ.L  #1,A0               ; ADVANCE A0 pointer (Consumed the digit)
    BRA     NEXT_DIGIT          ; Continue loop

    ; --- Failure Exit ---
HTB_ILLEGAL_CHAR:
    CLR.L   D0                  ; D0 = 0
    CLR.L   D1                  ; D1 = 0 (Failure)

    ADDQ.L  #1,A0               ; Advance A0 past the illegal character ('W')
    BRA     HTB_END_RESTORE

    ; --- Success Check Exit ---
HTB_TOKEN_END:
    ; Check if any digits were successfully parsed (D3 > 0)
    TST.L   D3
    BNE     HTB_SUCCESS

    ; D3 = 0: Empty string or only delimiters at start. Returns D1=0.
    BRA     HTB_END_RESTORE

HTB_SUCCESS:
    BSET    #0,D1               ; Set D1.0 flag to 1 for Success

HTB_END_RESTORE:
    MOVEM.L (SP)+,D3/D1/D2/A1   ; Restore registers
    RTS
