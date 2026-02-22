; # How to access registers
;
; - WRITE ADDRESS: MOVE Dx,PSG_ADDR
; - WRITE DATA   : MOVE Dx,PSG_DATA
; - READ DATA(*) : MOVE PSG_ADDR,Dx
;
; (*) Data are read from PSG_ADDR!
;     The access mimics the Atari ST

PSG_WR  MACRO
        MOVE.W  #\1,PSG_ADDR    ; Select Register (\1 is the first argument)
        MOVE.W  #\2,PSG_DATA    ; Write Data (\2 is the second argument)
        ENDM

PSG      EQU   $00409000 ; PSG Base Address
PSG_ADDR EQU   PSG       ; Address Register
PSG_DATA EQU   PSG+2     ; Data Register
