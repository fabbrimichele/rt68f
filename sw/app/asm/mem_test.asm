    ; ------------------------------
    ; Program code
    ; ------------------------------
    ORG    $4100            ; Start of RAM

START:
    MOVEA.L #RAM_START,A0       ; A0 = Base address of RAM
    MOVE.L  #RAM_SIZE,D1        ; D1 = Loop counter (bytes)

    MOVE.W  #INIT_WORD,D0       ; D0 = $DEAD (Initial word)
    MOVE.W  #EXPECT_UPPER,D5    ; D5 = $AAAD (Expected after upper byte update)
    MOVE.W  #EXPECT_LOWER,D6    ; D6 = $DE55 (Expected after lower byte update)

    MOVE.B  #NEW_UPPER_B,D3     ; D3 = $AA (New upper byte)
    MOVE.B  #NEW_LOWER_B,D4     ; D4 = $55 (New lower byte)

    ; ------------------------------------------------------------------
    ; TEST: BYTE ISOLATION TEST (Loops through every word address)
    ; ------------------------------------------------------------------
BYTE_TEST_LOOP:
    SUBQ.L  #2,D1               ; Decrement byte counter by 2 (word address step)
    BMI     TEST_PASS           ; If counter < 0, test passed

    ; 1. Initialize Word
    MOVE.W  D0,(A0)             ; WRITE: Write initial word $DEAD to (A0)

    ; 2. Test Upper Byte Write (Uses UDS only on (A0) - even address)
    MOVE.B  D3,(A0)             ; WRITE: Overwrite UPPER byte with $AA
    MOVE.W  (A0),D2             ; READ: Read full word back
    CMP.W   D5,D2               ; Compare with EXPECT_UPPER ($AAAD)
    BNE     FAIL_UPPER_BYTE     ; If mismatch, fail

    ; 3. Re-Initialize Word
    MOVE.W  D0,(A0)             ; WRITE: Reset word to $DEAD

    ; 4. Test Lower Byte Write (Uses LDS only on 1(A0) - odd address)
    MOVE.B  D4,1(A0)            ; WRITE: Overwrite LOWER byte with $55
    MOVE.W  (A0),D2             ; READ: Read full word back
    CMP.W   D6,D2               ; Compare with EXPECT_LOWER ($DE55)
    BNE     FAIL_LOWER_BYTE     ; If mismatch, fail

    ADDA.L  #2,A0               ; Increment address by 2
    BRA     BYTE_TEST_LOOP      ; Continue loop

    * ------------------------------------------------------------------
    * RESULTS AND HALT
    * ------------------------------------------------------------------
TEST_PASS:
    MOVE.B  #STATUS_PASS,(LED)     ; Success:
    BRA     HALT

FAIL_UPPER_BYTE:
    MOVE.B  #STATUS_FAIL_UP,(LED)  ; Failure:
    BRA     HALT

FAIL_LOWER_BYTE:
    MOVE.B  #STATUS_FAIL_LOW,(LED) ; Failure:
    BRA     HALT

HALT:
    TRAP    #14


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
    ; Data Section
    ; ------------------------------
MSG_READY:
    DC.B    'Ready.',10,0 ; String with Newline (10) and Null Terminator (0)

    ; ===========================
    ; Constants
    ; ===========================
DLY_VAL     EQU 1333333     ; Delay iterations, 1.33 million = 0.5 sec at 32MHz
RAM_END     EQU $00008000   ; End of RAM address (+1)
LED         EQU $00010000   ; LED-mapped register base address
UART_BASE   EQU $00012000   ; UART-mapped data register address
UART_DATA   EQU UART_BASE+0 ; UART-mapped data register address
UART_STAT   EQU UART_BASE+2 ; UART-mapped data register address

    ; --- Memory and Peripheral Map Constants (Based on your decoder) ---
RAM_START       EQU     $004000     ; Base address of RAM (16KB)
RAM_SIZE        EQU     $004000     ; Total size of RAM in bytes (16KB)

    ; --- Test Data Constants ---
INIT_WORD       EQU     $DEAD       ; Initial 16-bit word pattern
NEW_UPPER_B     EQU     $AA         ; New pattern for the Upper Byte ($D15-D8)
NEW_LOWER_B     EQU     $55         ; New pattern for the Lower Byte ($D7-D0)

    ; --- Expected Result Constants ---
EXPECT_UPPER    EQU     $AAAD       ; Expected word after UPPER byte write ($AA | $AD)
EXPECT_LOWER    EQU     $DE55       ; Expected word after LOWER byte write ($DE | $55)

    ; --- Status Code Constants ---
STATUS_PASS     EQU     %00001111 ; Success: All tests passed
STATUS_FAIL_UP  EQU     %00000011 ; Failure: Upper byte corruption detected
STATUS_FAIL_LOW EQU     %00001100 ; Failure: Lower byte corruption detected

; NOTE: do not remove spaces around +