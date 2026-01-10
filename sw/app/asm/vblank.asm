; ===========================
; Program code
; ===========================
    ORG    $400             ; Start of RAM

START:
    ; Set interrupt handler
    MOVE.W  #$1,LED
    MOVE.L  #VBL_ISR,VT_INT_1

    ; Enable interrupts (Clear mask bits)
    MOVE.W  #$2,LED
    MOVE.W  #$2000,SR    ; Binary: 0010 0000 0000 0000 (Supervisor bit on, Interrupt mask 0)
    ;AND.W   #$F8FF,SR   ; Bitwise AND to clear bits 8, 9, and 10
LOOP:
    MOVE.W  #$0,LED
    BRA     LOOP
;END:
;    TRAP    #14

VBL_ISR:
    MOVE.W  #$8,LED
    RTE



; ===========================
; Include files
; ===========================
    INCLUDE '../../lib/asm/isr_vector.asm'
    INCLUDE '../../lib/asm/vga.asm'
    INCLUDE '../../lib/asm/led.asm'

