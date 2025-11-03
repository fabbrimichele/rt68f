    ; ===========================
    ; Program code
    ; ===========================
    ORG    $4100            ; Start of RAM

START:
    ; Horizontal grid
    MOVE.L  #39,D0          ; line length
    LEA     VGA,A0
    MOVE.B  #24,D1           ; Number of lines - 1
HR_GRD_LOOP:
    BSR     HOR_LINE
    ;ADD     #16,A0          ; next line address
    ;DBRA    D1,HR_GRD_LOOP  ; Decrease and continue

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
    DBRA    D0,HOR_LINE
HOR_LINE_DONE:
    MOVEM.L (SP)+,D0/A0
    RTS

; TODO: write a full vertical grid
; TODO: write a full horizontal grid
; TODO: change the monitor to store private data at the end of RAM and move
;       the stack pointer below, this way programs can start from RAM start
;       and don't need to be moved if the Monitor requires more RAM.


    ; ===========================
    ; Constants
    ; ===========================
DLY_VAL     EQU     1333333     ; Delay iterations, 1.33 million = 0.5 sec at 32MHz
VGA         EQU     $00008000   ; VGA framebuffer base address (32KB)
