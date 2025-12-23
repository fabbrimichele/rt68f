    ; ------------------------------
    ; Program code
    ; ------------------------------
    ORG    $400                 ; Start of RAM

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
    ; Data Section
    ; ------------------------------
MSG_READY:
    DC.B    'Ready.',10,0 ; String with Newline (10) and Null Terminator (0)

    ; ===========================
    ; Constants
    ; ===========================
LED         EQU $00400000           ; LED-mapped register base address

    ; --- Memory and Peripheral Map Constants (Based on your decoder) ---

    ; FPGA Mem
;RAM_START       EQU     $004000     ; Base address of RAM (16KB)
;RAM_SIZE        EQU     $004000     ; Total size of RAM in bytes (16KB)

    ; SRAM
RAM_START       EQU     $001000     ; Base address of RAM (1MB)
RAM_SIZE        EQU     $07F000     ; Total size of RAM in bytes (512KB - 4KB)

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