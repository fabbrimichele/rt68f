; TODO
; Implement a program to switch Keyboard's LED on and off
; Maybe just a simple counter
;
; The Command Sequence
; 0. Send reset 0xFF (only new keyboard)
; 1. Byte 1: 0xED (Set/Reset Status Indicators)
; 2. Wait for 0xFA (ACK from keyboard)
; 3. Byte 2: LED State Byte (The bitmask for which LEDs to light)
;
; The LED Bitmask (Byte 2)
; ------------------------
; Bit       LED Function
; Bit 0     Scroll Lock
; Bit 1     Num Lock
; Bit 2     Caps Lock
; Bits 3-7  Always 0

; ===========================
; Program code
; ===========================
    ORG    $400             ; Start of RAM

START:
    MOVE.W  #0,LED              ; Init LED
    MOVE.W  #$FF,D0             ; FF - reset PS/2
    BSR     PS2_WRITE
.LOOP:
    BSR     PS2_GET             ; Read PS/2 control register high nibble
    MOVE.W  D0,LED              ; Display on LED
    BSR     PUTCHAR             ; Print key
    BRA     .LOOP

PS2_GET:
    LEA     PS2_ASCII,A1
    BSR     PS2_READ            ; Read byte from PS/2
    CMP.W   #$F0,D0             ; Is release key?
    BNE     .TOASCII
    BSR     PS2_READ            ; Skip F0 and following key
    BRA     PS2_GET
.TOASCII:
    MOVE.B  (A1,D0),D0          ; Map key code to ascii
    RTS

; Reads PS/2 data and returns in D0 (blocking)
PS2_READ:
.WAIT:
    MOVE.W  PS2A_CTRL,D0        ; BTST can't be used directly because of unimplemented LDS/UDS
    BTST    #4,D0               ; Check RX buffer full
    BEQ     .WAIT               ; RX buffer full = 0 -> buffer empty, wait
    MOVE.W  PS2A_DATA,D0        ; Read data to D0
    RTS

; Write D0 to PS/2 data (blocking)
PS2_WRITE:
    MOVEM.L D1,-(SP)            ; Save registers
.WAIT:
    MOVE.W  PS2A_CTRL,D1        ; BTST can't be used directly because of unimplemented LDS/UDS
    BTST    #5,D1               ; Check TX buffer full
    BNE     .WAIT               ; TX buffer full = 1 -> full, wait
    MOVE.W  D0,PS2A_DATA        ; Write D0 to data
    MOVEM.L (SP)+,D1            ; Restore registers
    RTS

; Variables
RELEASE  DS.B   1               ; F0 pressed


; ===========================
; Include files
; ===========================
    INCLUDE '../../lib/asm/ps2.asm'
    INCLUDE '../../lib/asm/led.asm'
    INCLUDE '../../lib/asm/console_io_16450.asm'
    INCLUDE '../../lib/asm/ps2_ascii.asm'
