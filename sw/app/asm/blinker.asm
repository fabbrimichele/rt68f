    ; ===========================
    ; Program code
    ; ===========================
    ORG    $400             ; Start of RAM

START:
    MOVE.W  #1,D1

LOOP:
    MOVE.W  D1,(LED)        ; Write D1 into LED register
    ADDQ.W  #1,D1           ; Increment register
    JSR     DELAY           ; Call delay
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
LED         EQU     $00400000   ; LED-mapped register base address


