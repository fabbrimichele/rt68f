; # How to access registers
;
; - WRITE ADDRESS: MOVE Dx,PSG_ADDR
; - WRITE DATA   : MOVE Dx,PSG_DATA
; - READ DATA(*) : MOVE PSG_ADDR,Dx
;
; (*) Data are read from PSG_ADDR!
;     The access mimics the Atari ST

PSG      EQU   $00407000 ; PSG Base Address
PSG_ADDR EQU   PSG       ; Address Register
PSG_DATA EQU   PSG+2     ; Data Register
