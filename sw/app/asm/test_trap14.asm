    ; ===========================
    ; Program code
    ; ===========================
    ORG    $4000            ; Start of RAM

START:
    MOVE.W  #5,(LED)        ; Write D1 into LED register
    TRAP    #14

    ; ===========================
    ; Constants
    ; ===========================
LED         EQU     $00010000   ; LED-mapped register base address


