; ===========================
; Program code
; ===========================
    ORG    $400             ; Start of RAM

START:
    MOVE.W  #$00,D0         ; Channel A Fine Tune
    MOVE.W  #$FE,D1
    BSR     PSG_WRITE

    MOVE.W  #$02,D0         ; Channel A Coarse Tune
    MOVE.W  #$00,D1
    BSR     PSG_WRITE

    MOVE.W  #$07,D0         ; Mixer Control
    MOVE.W  #$7E,D1         ; Bit 0 (Tone A) must be zero (active low)
    BSR     PSG_WRITE

    ; TODO: check how this register is suppose to work
    ; This command doesn't work, it mute the volume
    MOVE.W  #08,D0          ; Channel A Amplitude
    MOVE.W  #0F,D1          ; Set to maximum volume
    ;BSR     PSG_WRITE

    BSR     DELAY
    MOVE.W  #$07,D0         ; Mixer Control
    MOVE.W  #$00,D1         ; Mute all channels
    BSR     PSG_WRITE

    TRAP    #14

; Write to PSG registers
; D0 register
; D1 data
PSG_WRITE:
    MOVE.W  D0,PSG_ADDR
    MOVE.W  D1,PSG_DATA
    RTS

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
