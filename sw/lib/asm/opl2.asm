;------------------------------------------------------------------------------------
; OPL2
;------------------------------------------------------------------------------------
OPL2_BASE       EQU $00407000               ; OPL2 base address
OPL2_ADDR       EQU OPL2_BASE+$0            ; Receive Buffer Register(RBR) / Transmitter Holding Register(THR) / Divisor Latch (LSB)
OPL2_DATA       EQU OPL2_BASE+$2            ; Interrupt enable register / Divisor Latch (MSB)

; Write a byte to OPL2
; D0.W: address
; D1.W: data
OPL2_PUT:
    MOVE.W  D0,OPL2_ADDR
    BSR     DELAY
    MOVE.W  D1,OPL2_DATA
    BSR     DELAY
    RTS

DELAY:
    MOVE.W  D0,-(SP)
    MOVE.W  #DLY_VAL,D0
.LOOP:
    DBRA    D0,.LOOP
    MOVE.W  (SP)+,D0
    RTS


    ; ===========================
    ; Constants
    ; ===========================
DLY_VAL     EQU     5
