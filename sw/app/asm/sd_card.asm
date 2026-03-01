; ===========================
; Program code
; ===========================
    ORG    $400             ; Start of RAM


START:
    ; RESET
    LEA     MSG_RESET,A0
    BSR     PUTS
    BSR     SD_RESET
    MOVE.W  #$01,LED

    ; CMD0
    LEA     MSG_CMD0,A0
    BSR     PUTS
    BSR     SD_CMD0
    CMP.B   #$01,D0
    BNE     .ERR
    MOVE.W  #$02,LED

    ; CMD8
    LEA     MSG_CMD8,A0
    BSR     PUTS
    BSR     SD_CMD8
    CMP.B   #$AA,D0
    BNE     .ERR
    MOVE.W  #$04,LED

    ; OK
    LEA     MSG_OK,A0
    BSR     PUTS
    BRA     .END
.ERR:
    BSR     BINTOHEX_W
    LEA     MSG_ERR,A0
    BSR     PUTS
.END:
    TRAP    #14


; -----------------------------------------
; SD_RESET: Resets SD card
; -----------------------------------------
SD_RESET:
    MOVEM.L D0/D1,-(SP)
    MOVE.B  #$FF,SD_CTRL    ; Set CS high (inactive)
    MOVE.B  #9,D1           ; Repeat 10-1 times (DBRA)
    MOVE.B  #$FF,D0
.LOOP:
    BSR     SD_WRITE
    DBRA    D1,.LOOP
    MOVEM.L (SP)+,D0/D1
    RTS

; -----------------------------------------
; SD_CMD0: Sends CMD0
; Output: D0 result, $01 OK
; -----------------------------------------
SD_CMD0:
    MOVEM.L A0,-(SP)
    MOVE.B  #$00,SD_CTRL    ; Set CS low (active)
    LEA     CMD0,A0
    BSR     SD_SEND_CMD
    BSR     WAIT_R1
    MOVE.B  #$FF,SD_CTRL    ; Set CS high (inactive)
    MOVEM.L (SP)+,A0
    RTS

; -----------------------------------------
; SD_CMD8: Sends CMD8
; Output: D0 result, if $01 OK else ERROR
; -----------------------------------------
SD_CMD8:
    MOVEM.L A0,-(SP)
    MOVE.B  #$00,SD_CTRL    ; Set CS low (active)
    LEA     CMD8,A0
    BSR     SD_SEND_CMD
    BSR     WAIT_R1
    CMP.B   #$01,D0         ; Is it $01?
    BNE     .EXIT           ; If not $01, card rejected CMD8
    ; Read remaining bytes
    MOVE.B  #$FF,D0         ; Send dummy to get clock
.LOOP:
    JSR     SD_WRITE
    JSR     SD_WRITE
    JSR     SD_WRITE
    JSR     SD_WRITE
    MOVE.B  SD_DATA,D0      ; Wait until different from $FF
.EXIT:
    MOVE.B  #$FF,SD_CTRL    ; Set CS high (inactive)
    MOVEM.L (SP)+,A0
    RTS

; -----------------------------------------
; SD_SEND_CMD: Sends the 6-byte from memory
; Inputs: A0 - packet address
; -----------------------------------------
SD_SEND_CMD:
    MOVEM.L D0/D1/A0,-(SP)
    MOVE.W  #5,D1               ; Send 6 bytes
.LOOP:
    MOVE.B  (A0)+,D0
    JSR     SD_WRITE            ; Write byte
    DBRA    D1,.LOOP
    MOVEM.L (SP)+,D0/D1/A0
    RTS

; ---------------------------------
; WAIT_R1
; Outputs: D0 response byte
; ---------------------------------
; TODO: timeout if returns always $FF
WAIT_R1:
    MOVEM.L D1,-(SP)
    MOVE.B  #$FF,D0         ; Dummy write data
.WAIT:
    BSR     SD_WRITE        ; Clock SPI while wait
    MOVE.B  SD_DATA,D1      ; Wait until different from $FF
    CMP.B   #$FF,D1
    BEQ     .WAIT
.EXIT:
    MOVE.B  D1,D0
    MOVEM.L (SP)+,D1
    RTS

; -----------------------------------------
; SD_WRITE: Writes 1 byte to SD card
; Inputs: D0 - byte to be written
; -----------------------------------------
SD_WRITE:
    ; TODO: check TX is ready
    MOVE.B  D0,SD_DATA
    BSR     DELAY           ; TODO: use TX ready
    RTS

; ---------------------------------
; DELAY: Delay
; ---------------------------------
DELAY:
    MOVEM.L D3,-(SP)
    MOVE.L  #DLY_VAL,D3     ;
DLY_LOOP:
    SUBQ.L  #1,D3           ; 4 cycles
    BNE     DLY_LOOP        ; 10 cycles when taken
    MOVEM.L (SP)+,D3
    RTS


; ===========================
; Constants
; ===========================
DLY_VAL     EQU     1000

; SD card commands
CMD0        DC.B    $40, $00, $00, $00, $00, $95
CMD8        DC.B    $48, $00, $00, $01, $AA, $87

; Messages
MSG_RESET   DC.B LF,'RESET ',NUL
MSG_CMD0    DC.B LF,'CMD0 ',NUL
MSG_CMD8    DC.B LF,'CMD8 ',NUL
MSG_OK      DC.B LF,'OK',NUL
MSG_ERR     DC.B LF,'ERR',NUL

; ASCII
CR          EQU 13          ; Carriage Return
LF          EQU 10          ; Line Feed
BEL         EQU 7           ; Bell character
NUL         EQU 0

; ===========================
; Include files (Macro at top)
; ===========================
    INCLUDE '../../lib/asm/sd_card.asm'
    INCLUDE '../../lib/asm/led.asm'
    INCLUDE '../../lib/asm/console_io_16450.asm'
    INCLUDE '../../lib/asm/conversions.asm'


; Send ten times to reset the card (set also CS high)
; WRITE 4C0000 FFFF
;
; Reset command
; CS low (enabled)
; WRITE 4C0000 0000
; CMD0 Resets the card and puts it into SPI mode.
; WRITE 4C0002 0040
; WRITE 4C0002 0000
; WRITE 4C0002 0000
; WRITE 4C0002 0000
; WRITE 4C0002 0000
; WRITE 4C0002 0095
; Wait for $01
; WRITE 4C0002 00FF
; DUMP 4C0000
; CS high (disabled)
; WRITE 4C0000 FFFF

; DUMP 4C0000
