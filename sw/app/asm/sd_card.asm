; ===========================
; Include files (Macro at top)
; ===========================
    INCLUDE '../../lib/asm/sd_card.asm'
    INCLUDE '../../lib/asm/led.asm'

; ===========================
; Program code
; ===========================
    ORG    $400             ; Start of RAM


START:
.LOOP:
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


; http://poi.ribbon.free.fr/tmp/freq2regs.htm
; Note	        Hex Period (hi/lo)
C_4_HI      EQU $01
C_4_LO	    EQU $DE
Cc4_HI	    EQU $01
Cc4_LO	    EQU $C2
D_4_HI	    EQU $01
D_4_LO	    EQU $A8
Dc4_HI	    EQU $01
Dc4_LO	    EQU $90
E_4_HI	    EQU $01
E_4_LO	    EQU $7B
F_4_HI	    EQU $01
F_4_LO	    EQU $66
Fc4_HI	    EQU $01
Fc4_LO	    EQU $52
G_4_HI	    EQU $01
G_4_LO	    EQU $40
Gc4_HI	    EQU $01
Gc4_LO	    EQU $2E
A_4_HI	    EQU $01
A_4_LO	    EQU $1C
Ac4_HI	    EQU $01
Ac4_LO	    EQU $0C
B_4_HI	    EQU $00
B_4_LO	    EQU $FD
