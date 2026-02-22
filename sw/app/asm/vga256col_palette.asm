    ; ===========================
    ; Program code
    ; ===========================
    ORG    $400             ; Start of RAM

START:
    ; Set Screen mode to 320x200 256 colors
    MOVE.W  #2,VGA_CTRL

    ; Clear screen
    MOVE.W  #0,D0           ; Fill pattern blank
    BSR     FILL            ; Clear screen

    LEA     VGA,A0
    MOVE.W  #199,D4 ; Number of lines -1
V_LOOP:
    ; Line length in bytes - 1
    ; each pixel 8 bits -> 320 px = 320 bytes
    MOVE.W  #319,D0 ; Line length in words -1
    MOVE.B  #0,D1   ; pixel color (1 byte)
H_LOOP:
    MOVE.B  D1,(A0)+
    ADD.B   #1,D1
    DBRA    D0,H_LOOP
    DBRA    D4,V_LOOP

END:
    TRAP    #14

; Fill the screen with a pattern (16 bits pattern)
; Input:
; D0 pattern (e.g. 5555)
FILL:
    MOVEM.L D0/D1/A0,-(SP)
    LEA     VGA,A0
    MOVE.W  #(VGA_LEN-1),D1     ; Video memory length - 1
FILL_LOOP:
    MOVE.W  D0,(A0)+            ; write solid line (16 pixels)
    DBRA    D1,FILL_LOOP

    MOVEM.L (SP)+,D0/D1/A0      ; Done
    RTS

; ===========================
; Constants
; ===========================
DLY_VAL     EQU     1333333     ; Delay iterations, 1.33 million = 0.5 sec at 32MHz

; ===========================
; Include files
; ===========================
    INCLUDE '../../lib/asm/vga.asm'
