; ===========================
; Program code
; ===========================
    ORG    $400             ; Start of RAM

; To simulate tones, see: https://ym2149-rs.org/tutorials.html

START:
    MOVE.W  #$00,PSG_ADDR   ; Channel A Low
    MOVE.W  #$1C,PSG_DATA

    MOVE.W  #$01,PSG_ADDR   ; Channel A High
    MOVE.W  #$01,PSG_DATA

    MOVE.W  #$08,PSG_ADDR   ; Volume A max
    MOVE.W  #$07,PSG_DATA   ;

    ; TODO: setting the tone to max $0F mute the channel
    MOVE.W  #$07,PSG_ADDR   ; Mixer Control
    MOVE.W  #$3E,PSG_DATA   ; Bit 0 (Tone A) must be zero (active low)

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
