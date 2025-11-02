    ; ===========================
    ; Program code
    ; ===========================
    ORG    $4100            ; Start of RAM

START:
    LEA     VGA,A0

LINE:
    MOVE.L  #39,D0          ; 40 words to cover 640 pixels (DBRA requires - 1)
LINE_LOOP:
    MOVE.W  #$FFFF,(A0)+    ; write solid line (16 pixels)
    DBRA    D0,LINE_LOOP

; TODO: write a full vertical grid
; TODO: write a full horizontal grid
; TODO: change the monitor to store private data at the end of RAM and move
;       the stack pointer below, this way programs can start from RAM start
;       and don't need to be moved if the Monitor requires more RAM.

END:
    TRAP    #14

    ; ===========================
    ; Constants
    ; ===========================
DLY_VAL     EQU     1333333     ; Delay iterations, 1.33 million = 0.5 sec at 32MHz
VGA         EQU     $00008000   ; VGA framebuffer base address (32KB)
