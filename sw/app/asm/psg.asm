; ===========================
; Program code
; ===========================
    ORG    $400             ; Start of RAM

START:
    MOVE.W  #$00,PSG_ADDR   ; Channel A Fine Tune
    MOVE.W  #$FE,PSG_DATA

    MOVE.W  #$00,PSG_ADDR   ; Channel A Fine Tune
    MOVE.W  #$FE,PSG_DATA

    MOVE.W  #$02,PSG_ADDR   ; Channel A Coarse Tune
    MOVE.W  #$00,PSG_DATA

    MOVE.W  #$07,PSG_ADDR   ; Mixer Control
    MOVE.W  #$7E,PSG_DATA   ; Bit 0 (Tone A) must be zero (active low)

    ; TODO: check how this register is suppose to work
    ; This command doesn't work, it mute the volume
    ;MOVE.W  #08,PSG_ADDR   ; Channel A Amplitude
    ;MOVE.W  #0F,PSG_DATA   ; Set to maximum volume

    BSR     DELAY
    MOVE.W  #$07,PSG_ADDR   ; Mixer Control
    MOVE.W  #$00,PSG_DATA   ; Mute all channels

    TRAP    #14

DELAY:
    MOVE.L  #DLY_VAL,D3     ;
DLY_LOOP:
    SUBQ.L  #1,D3           ; 4 cycles
    BNE     DLY_LOOP        ; 10 cycles when taken
    RTS

; ===========================
; Constants
; ===========================
DLY_VAL     EQU     2000000

; ===========================
; Include files
; ===========================
    INCLUDE '../../lib/asm/psg.asm'
    INCLUDE '../../lib/asm/led.asm'
