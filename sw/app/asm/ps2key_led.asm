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