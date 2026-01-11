; ===========================
; Program code
; ===========================
    ORG    $400             ; Start of RAM

START:
    MOVE.B  #INT_CNT,COUNTER    ; Reset counter
    MOVE.W  #$FFFF,LED          ; Init LED
    MOVE.L  #VBL_ISR,VT_INT_1   ; Set interrupt handler
    OR.W    #$0008,VGA_CTRL     ; Enable VBlank interrupt on VGA (bit 3 high)
    AND.W   #$F8FF,SR           ; Enable all interrupts on 68000 (Clear mask bits)
    TRAP    #14

VBL_ISR:
    OR.W    #$0040,VGA_CTRL     ; Ack interrupt (write high to bit 6)
    SUBQ.B  #1,COUNTER
    BNE     .RET
    MOVE.B  #INT_CNT,COUNTER    ; Reset counter
    NOT.W   LED
.RET:
    RTE

; Variables
COUNTER     ds.b    1

; Constants
INT_CNT     equ     30

; ===========================
; Include files
; ===========================
    INCLUDE '../../lib/asm/isr_vector.asm'
    INCLUDE '../../lib/asm/vga.asm'
    INCLUDE '../../lib/asm/led.asm'

