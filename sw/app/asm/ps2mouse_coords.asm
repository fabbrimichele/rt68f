; TODO
; Implement a program to print Mouse coordinates and button status
;
; PS/2 Mouse Initialization
;
; Step      Command     Description         Expected Response
; 1         0xFF        Reset Mouse         0xFA then 0xAA (Self-test passed)
; 2         0xF2        Get Device ID       0xFA then 0x00 (Standard Mouse)
; 3         0xF4        Enable Reporting    0xFA
;
; After init, the mouse starts sending coords and status