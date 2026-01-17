; ===========================
; Program code
; ===========================
    ORG    $400             ; Start of RAM

; NOTE: TIMER B IS NOT IMPLEMENT IN HW!!!
START:
    MOVE.B  #0,COUNTER          ; Reset counter
    MOVE.W  COUNTER,LED         ; Init LED
    MOVE.L  #TMRB_ISR,VT_INT_2  ; Set interrupt handler
    MOVE.W  #$FF,TMR_PRSB       ; Set prescaler timer B
    MOVE.W  #$8000,TMR_CNTB     ; Set counter timer B -> counter + prescaler = $8000FF -> 8M = 2Hz
    OR.W    #$0008,TMR_CTRL     ; Enable Timer B interrupt (bit 3 high)
    AND.W   #$F8FF,SR           ; Enable all interrupts on 68000 (Clear mask bits)
    TRAP    #14

TMRB_ISR:
    OR.W    #$0080,TMR_CTRL     ; Ack interrupt (write high to bit 6)
    ADDQ.B  #1,COUNTER
    MOVE.W  COUNTER,LED
.RET:
    RTE

; Variables
COUNTER     ds.w    1

; ===========================
; Include files
; ===========================
    INCLUDE '../../lib/asm/isr_vector.asm'
    INCLUDE '../../lib/asm/timer.asm'
    INCLUDE '../../lib/asm/led.asm'

