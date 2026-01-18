; ===========================
; Program code
; ===========================
    ORG    $400             ; Start of RAM

START:
    MOVE.B  #0,COUNTER          ; Reset counter
    MOVE.W  COUNTER,LED         ; Init LED
    MOVE.L  #TMRA_ISR,VT_INT_2  ; Set interrupt handler
    MOVE.W  #$FF,TMRA_PRS       ; Set prescaler timer A
    MOVE.W  #$FFFF,TMRA_CNT     ; Set counter timer A -> counter + prescaler = $FFFFFF -> 16M = 1Hz
    OR.W    #$0004,TMRA_CTRL    ; Enable Timer A interrupt (bit 2 high)
    AND.W   #$F8FF,SR           ; Enable all interrupts on 68000 (Clear mask bits)
    TRAP    #14

TMRA_ISR:
    OR.W    #$0040,TMRA_CTRL    ; Ack interrupt (write high to bit 6)
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

