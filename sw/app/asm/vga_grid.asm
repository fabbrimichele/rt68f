    ; ===========================
    ; Program code
    ; ===========================
    ORG    $4000            ; Start of RAM

START:
    ; Set Screen mode to 640x400 2 colors
    MOVE.W  #0,VGA_CTRL

    ; Clear screen
    MOVE.W  #0,D0           ; Fill pattern blank
    BSR     FILL            ; Clear screen

    ; Horizontal grid
    MOVE.W  #39,D0          ; Line length
    MOVE.W  #24,D1          ; Number of lines - 1
    LEA     VGA,A0
HR_GRD_LOOP:
    BSR     HOR_LINE
    ADD.L   #(80*16),A0     ; Next line after 16 lines (here we count bytes not words)
    DBRA    D1,HR_GRD_LOOP  ; Decrease, check and branch
    LEA     (80*399+VGA),A0 ; Last line (at 399)
    BSR     HOR_LINE

    ; Vertical grid
    MOVE.W  #399,D0         ; Line length (in pixels)
    MOVE.W  #$8000,D1       ; Pattern
    MOVE.W  #39,D2          ; Number of lines - 1
    LEA     VGA,A0          ; First column
VR_GRD_LOOP:
    BSR     VER_LINE
    ADD.L   #2,A0           ; Next line after 16 lines (here we count in bytes not words)
    DBRA    D2,VR_GRD_LOOP  ; Decrease, check and branch
    SUB.L   #2,A0           ; Last line pattern (it's the last column of the word)
    MOVE.W  #$0001,D1
    BSR     VER_LINE

END:
    TRAP    #14


; Draw a horizontal line (in steps of 16 pixels)
; Input:
; A0 start address
; D0 (line length / 16) - 1
HOR_LINE:
    MOVEM.L D0/A0,-(SP)
HOR_LINE_LOOP:
    MOVE.W  #$FFFF,(A0)+    ; write solid line (16 pixels)
    DBRA    D0,HOR_LINE_LOOP
    MOVEM.L (SP)+,D0/A0     ; Done
    RTS

; Draw a vertical line (only at 16 * x pos)
; Input:
; A0 start address
; D0 line length
; D1 pattern
VER_LINE:
    MOVEM.L D0/A0,-(SP)
VER_LINE_LOOP:
    OR.W    D1,(A0)         ; Draw pattern
    ADD.L   #80,A0          ; Next line
    DBRA    D0,VER_LINE_LOOP
    MOVEM.L (SP)+,D0/A0     ; Done
    RTS


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
VGA         EQU     $00008000   ; VGA framebuffer base address
VGA_LEN     EQU     $4000       ; VGA framebuffer length in words
VGA_CTRL    EQU     $00013100   ; VGA Control (screen mode)
VGA_PALETTE EQU     $00013000   ; VGA Control (screen mode)
