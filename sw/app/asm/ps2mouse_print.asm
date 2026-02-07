; PS/2 Mouse Initialization
;
; Step      Command     Description         Expected Response
; 1         0xFF        Reset Mouse         0xFA then 0xAA (Self-test passed)
; 2         0xF2        Get Device ID       0xFA then 0x00 (Standard Mouse)
; 3         0xF4        Enable Reporting    0xFA
;
; After init, the mouse starts sending coords and status

; ===========================
; Program code
; ===========================
    ORG    $400             ; Start of RAM

START:
    MOVE.W  #0,LED              ; Init LED
    BSR     MS_INIT
.LOOP
    BSR     MS_READ
    BSR     PRINT_MS
    BRA     .LOOP

PRINT_MS:
    MOVE.W  MS_ST,LED
    RTS

MS_READ:
    BSR     PS2_READ            ; Mouse status
    MOVE.W  D0,MS_ST
    BSR     PS2_READ            ; Mouse X movements
    MOVE.W  D0,MS_X
    BSR     PS2_READ            ; Mouse Y movements
    MOVE.W  D0,MS_Y
    RTS

MS_INIT:
    MOVE.W  #$FF,D0             ; Reset mouse
    BSR     PS2_WRITE
    BSR     PS2_READ            ; TODO: expect 0xFA (ACK)
    BSR     PS2_READ            ; TODO: expect 0xAA (SELF TEST OK)
    BSR     PS2_READ            ; TODO: expect 0x00 (DEVIDE ID)
    MOVE.W  #$F4,D0             ; Enable mouse stream
    BSR     PS2_WRITE
    BSR     PS2_READ            ; TODO: expect 0xFA (ACK)
    RTS

; Reads PS/2 data and returns in D0 (blocking)
PS2_READ:
.WAIT:
    MOVE.W  PS2B_CTRL,D0        ; BTST can't be used directly because of unimplemented LDS/UDS
    BTST    #4,D0               ; Check RX buffer full
    BEQ     .WAIT               ; RX buffer full = 0 -> buffer empty, wait
    MOVE.W  PS2B_DATA,D0        ; Read data to D0
    RTS

; Write D0 to PS/2 data (blocking)
PS2_WRITE:
    MOVEM.L D1,-(SP)            ; Save registers
.WAIT:
    MOVE.W  PS2B_CTRL,D1        ; BTST can't be used directly because of unimplemented LDS/UDS
    BTST    #5,D1               ; Check TX buffer full
    BNE     .WAIT               ; TX buffer full = 1 -> full, wait
    MOVE.W  D0,PS2B_DATA        ; Write D0 to data
    MOVEM.L (SP)+,D1            ; Restore registers
    RTS

; Variables
MS_ST   DS.W   1
MS_X    DS.W   1
MS_Y    DS.W   1

; ===========================
; Include files
; ===========================
    INCLUDE '../../lib/asm/ps2.asm'
    INCLUDE '../../lib/asm/led.asm'
    INCLUDE '../../lib/asm/console_io_16450.asm'
    INCLUDE '../../lib/asm/ps2_ascii.asm'
