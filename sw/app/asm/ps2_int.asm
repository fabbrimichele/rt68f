; ===========================
; Program code
; ===========================
    ORG    $400             ; Start of RAM

START:
    MOVE.W  #0,LED              ; Init LED
    MOVE.W  #0,BUFFER           ; Init BUFFER
    MOVE.L  #PS2B_ISR,VT_INT_5  ; Set interrupt handler
    OR.W    #$0002,PS2B_CTRL    ; Enable Timer A interrupt (bit 1 high)
    AND.W   #$F8FF,SR           ; Enable all interrupts on 68000 (Clear mask bits)
    TRAP    #14

PS2B_ISR:
    MOVEM.L D0,-(SP)
    OR.W    #$0040,PS2B_CTRL    ; Ack interrupt (write high to bit 6)
    MOVE.W  PS2B_DATA,D0        ; Read key code
    MOVE.B  D0,BUFFER           ; Save code
    MOVE.W  D0,LED              ; Show code to LED
.RET:
    MOVEM.L (SP)+,D0
    RTE

; Variables
    ORG    $500
BUFFER      DS.B    1


; ===========================
; Include files
; ===========================
    INCLUDE '../../lib/asm/isr_vector.asm'
    INCLUDE '../../lib/asm/ps2.asm'
    INCLUDE '../../lib/asm/led.asm'

