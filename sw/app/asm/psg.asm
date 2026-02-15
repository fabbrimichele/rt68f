; ===========================
; Program code
; ===========================
    ORG    $400             ; Start of RAM

START:
    TRAP    #14

; ===========================
; Include files
; ===========================
    INCLUDE '../../lib/asm/psg.asm'