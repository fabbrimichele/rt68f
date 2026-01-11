; ===========================
; Program code
; ===========================
    ORG    $400             ; Start of RAM

START:
    MOVE.B  #INT_CNT,DELAY      ; Reset delay
    MOVE.B  #0,COUNTER          ; Reset counter
    MOVE.W  COUNTER,LED         ; Init LED
    MOVE.L  #VBL_ISR,VT_INT_1   ; Set interrupt handler
    OR.W    #$0008,VGA_CTRL     ; Enable VBlank interrupt on VGA (bit 3 high)
    AND.W   #$F8FF,SR           ; Enable all interrupts on 68000 (Clear mask bits)
    TRAP    #14

VBL_ISR:
    OR.W    #$0040,VGA_CTRL     ; Ack interrupt (write high to bit 6)
    SUBQ.B  #1,DELAY
    BNE     .RET
    MOVE.B  #INT_CNT,DELAY      ; Reset DELAY
    ADDQ.B  #1,COUNTER
    MOVE.W  COUNTER,LED
.RET:
    RTE

; Variables
DELAY       ds.b    1
COUNTER     ds.w    1

; Constants
INT_CNT     equ     30

; ===========================
; Include files
; ===========================
    INCLUDE '../../lib/asm/isr_vector.asm'
    INCLUDE '../../lib/asm/vga.asm'
    INCLUDE '../../lib/asm/led.asm'

