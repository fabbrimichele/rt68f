; ===========================
; Vector Table
; ===========================
; Spurious Interrupt
VT_INT_SP       EQU $60
; Interrupts (Autovectors)
VT_INT_1        EQU $64
VT_INT_2        EQU $68
VT_INT_3        EQU $6C
VT_INT_4        EQU $70
VT_INT_5        EQU $74
VT_INT_6        EQU $78
VT_INT_7        EQU $7C

; Traps
VT_TRAP_14      EQU $B8

