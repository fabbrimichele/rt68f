; ===========================
; Program code
; ===========================
    ORG    $400             ; Start of RAM

START:
    MOVE.W  #0,LED              ; Init LED
    MOVE.W  #$FF,PS2A_DATA      ; Reset PS/2
    MOVE.L  #PS2A_ISR,VT_INT_5  ; Set interrupt handler
    OR.W    #$0002,PS2A_CTRL    ; Enable Timer A interrupt (bit 1 high)
    AND.W   #$F8FF,SR           ; Enable all interrupts on 68000 (Clear mask bits)
    TRAP    #14

PS2A_ISR:
    MOVEM.L D0,-(SP)
    OR.W    #$0040,PS2A_CTRL    ; Ack interrupt (write high to bit 6)
    MOVE.W  PS2A_DATA,D0        ; Read key code
    MOVE.W  D0,LED              ; Show code to LED
    BSR     PUT_BYTE
.RET:
    MOVEM.L (SP)+,D0
    RTE

PUT_BYTE:
    MOVEM.L A0,-(SP)
    MOVE.L  BUF_HEAD,A0     ; Load current write position
    MOVE.B  D0,(A0)+        ; Store byte and post-increment pointer
    CMP.L   #BUF_END,A0     ; Did we hit the end?
    BNE     .SAVE_HEAD      ; If no, skip reset
    LEA     BUF_START,A0    ; If yes, wrap back to start
.SAVE_HEAD:
    MOVE.L  A0,BUF_HEAD     ; Save updated pointer back to memory
    MOVEM.L (SP)+,A0
    RTS

; TODO: Not tested
; TODO: Save registers
;GET_BYTE:
;    MOVE.L  TAIL, A0
;    MOVE.L  HEAD, A1
;    CMPA.L  A0, A1          ; Is Head == Tail?
;    BEQ.S   .EMPTY          ; If yes, buffer is empty (handle error)
;    MOVE.B  (A0)+, D0       ; Read byte and post-increment
;    CMPA.L  #BUF_END, A0    ; Did we hit the end?
;    BNE.S   .SAVE_TAIL
;    LEA     BUF_START, A0   ; Wrap back to start
;.SAVE_TAIL:
;    MOVE.L  A0, TAIL        ; Save updated pointer
;    RTS
;.EMPTY:
;    ; Handle empty buffer (e.g., set an error flag or return -1)
;    RTS

; Constants
BUF_SIZE    EQU     256

; Variables
    ORG    $500
BUF_START   DS.B    BUF_SIZE    ; Reserve 256 bytes
BUF_END     EQU     *           ; The address right after the buffer
BUF_HEAD    DC.L    BUF_START   ; Write pointer
BUF_TAIL    DC.L    BUF_START   ; Read pointer

; ===========================
; Include files
; ===========================
    INCLUDE '../../lib/asm/isr_vector.asm'
    INCLUDE '../../lib/asm/ps2.asm'
    INCLUDE '../../lib/asm/led.asm'

