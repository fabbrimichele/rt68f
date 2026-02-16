; ===========================
; Program code
; ===========================
    ORG    $400             ; Start of RAM

START:
    MOVE.W  #$00,PSG_ADDR   ; Channel A Fine Tune
    BSR     DELAY
    MOVE.W  #$FE,PSG_DATA   ;
    BSR     DELAY
    MOVE.W  #$02,PSG_ADDR   ; Channel A Coarse Tune
    BSR     DELAY
    MOVE.W  #$00,PSG_DATA   ;
    BSR     DELAY
    MOVE.W  #$07,PSG_ADDR   ; Mixer Control
    BSR     DELAY
    MOVE.W  #%01111110,PSG_DATA     ; Bit 0 (Tone A) must be zero (active low)
    BSR     DELAY
    MOVE.W  #08,PSG_ADDR    ; Channel A Amplitude
    BSR     DELAY
    MOVE.W  #0F,PSG_DATA    ; Set to maximum volume
    TRAP    #14

DELAY:
    MOVE.L  #DLY_VAL,D0     ;
DLY_LOOP:
    SUBQ.L  #1,D0           ; 4 cycles
    BNE     DLY_LOOP        ; 10 cycles when taken
    RTS

    ; ===========================
    ; Constants
    ; ===========================
DLY_VAL     EQU     2000     ;

; ===========================
; Include files
; ===========================
    INCLUDE '../../lib/asm/psg.asm'