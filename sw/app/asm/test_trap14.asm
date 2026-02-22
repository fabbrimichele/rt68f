; ===========================
; Include files (Macro at top)
; ===========================
    INCLUDE '../../lib/asm/led.asm'

; ===========================
; Program code
; ===========================
    ORG    $400             ; Start of RAM

START:
    MOVE.W  #5,(LED)        ; Write D1 into LED register
    TRAP    #14

; ===========================
; Constants
; ===========================


