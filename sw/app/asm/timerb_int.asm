; ===========================
; Program code
; ===========================
    ORG    $400             ; Start of RAM

START:
    MOVE.B  #0,COUNTER          ; Reset counter
    MOVE.W  COUNTER,LED         ; Init LED
    MOVE.L  #TMRB_ISR,VT_INT_1  ; Set interrupt handler
    MOVE.W  #$FF,TMRB_PRS       ; Set prescaler timer B
    MOVE.W  #$8000,TMRB_CNT     ; Set counter timer B -> counter + prescaler = $8000FF -> 8M = 2Hz
    OR.W    #$0002,TMRB_CTRL    ; Enable Timer B interrupt (bit 2 high)
    AND.W   #$F8FF,SR           ; Enable all interrupts on 68000 (Clear mask bits)
    TRAP    #14

TMRB_ISR:
    OR.W    #$0040,TMRB_CTRL    ; Ack interrupt (write high to bit 6)
    ADDQ.W  #1,COUNTER
    MOVE.W  COUNTER,LED
.RET:
    RTE

; Variables
COUNTER     DS.W    1

; ===========================
; Include files
; ===========================
    INCLUDE '../../lib/asm/isr_vector.asm'
    INCLUDE '../../lib/asm/timer.asm'
    INCLUDE '../../lib/asm/led.asm'

