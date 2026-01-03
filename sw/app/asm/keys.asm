    ; ===========================
    ; Program code
    ; ===========================
    ORG     $400                ; Start of RAM

START:
    LEA     KEY,A1          ; Load KEY register address into A1
    LEA     LED,A0
    MOVE    #0,LED

    MOVE.L  #200,D2         ; Wait 2 seconds before load from Flash
SEL_LOOP:
    MOVE.L  #10,D0
    JSR     DELAY_MS        ; Wait 10ms
    MOVE.W  (A1),D1
    BTST    #KEY_DOWN,D1    ; Key down pressed?
    BNE     BOOT_SER        ; Yes, jump to boot serial
    DBRA    D2,SEL_LOOP

    MOVE    #2,LED
    BRA     DONE
BOOT_SER:
    MOVE    #1,LED
DONE:
    TRAP    #14

; ---------------------------------------------------------
; DELAY_MS
; Input:  D0.W = Number of milliseconds to wait
; Assumes: 16MHz CPU Clock
; ---------------------------------------------------------
DELAY_MS:
    MOVE.L  D1,-(SP)        ; Save D1

OUTER_LOOP:
    ; 1ms is 16,000 cycles.
    ; The inner loop (DBRA) is 12 cycles. 16000 / 12 approx 1333.
    MOVE.W  #1333,D1
INNER_LOOP:
    DBRA    D1,INNER_LOOP   ; Inner loop (1ms)
    DBRA    D0,OUTER_LOOP   ; Outer loop (number of ms)

    MOVE.L  (SP)+,D1        ; Restore D1
    RTS

    ; ===========================
    ; Constants
    ; ===========================

LED         EQU $00400000   ; LED-mapped register base address
KEY         EQU $00401000   ; KEY-mapped register base address

CTRL_RD     EQU 1

KEY_UP      EQU 0           ; Up key bit
KEY_RIGHT   EQU 1           ; Right key bit
KEY_DOWN    EQU 2           ; Down key bit
KEY_LEFT    EQU 3           ; Left key bit
