; ===========================
; Include files (Macro at top)
; ===========================
    INCLUDE '../../lib/asm/psg.asm'
    INCLUDE '../../lib/asm/led.asm'

; ===========================
; Program code
; ===========================
    ORG    $400             ; Start of RAM

; YM2149 Tutorial https://ym2149-rs.org/tutorials.html
; TODO: there is a huge problem, with VGA enabled it stops working...

START:
    PSG_WR  0,$1C   ; Channel A Low
    PSG_WR  1,$01   ; Channel A High
    PSG_WR  8,$0F   ; Volume A max
    PSG_WR  7,$3E   ; Enable Channel A
    BSR     DELAY
    PSG_WR  7,$00   ; Mute Channels

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
