; ===========================
; Program code
; ===========================
    ORG    $400             ; Start of RAM

PRINT MACRO
    LEA     \1,A0
    BSR     PUTS
    ENDM

PCHAR MACRO
    MOVE.B  \1,D0
    BSR     PUTCHAR
    ENDM

START:
    PRINT   MSG_INIT
    BSR     SD_INIT
    CMP.B   #0,D0
    BNE     .ERR
    PRINT   MSG_OK
    BRA     .END
.ERR:
    PRINT   MSG_ERR
    PRINT   MSG_BYTE
    BSR     BINTOHEX_W  ; Print last byte read
    PCHAR   #LF
    PRINT   MSG_CMD
    MOVE.B  D1,D0
    BSR     BINTOHEX_W  ; Print last command
    PCHAR   #LF
.END:
    TRAP    #14


; -----------------------------------------
; SD_INIT: Initialize SD card
; Outputs: D0 - last byte read
;          D1 - last command executed (e.g. $55)
;          D2 - 0 OK, 1 error
; -----------------------------------------
SD_INIT:
    MOVEM.L A0,-(SP)

    ; RESET
    BSR     SD_RESET

    ; CMD0
    MOVE.B  #$00,D1
    LEA     CMD0,A0
    BSR     SD_CMD
    CMP.B   #$01,D0
    BNE     .ERR

    ; CMD8
    MOVE.B  #$08,D1
    BSR     SD_CMD8
    CMP.B   #$AA,D0
    BNE     .ERR

.INIT_LOOP:
    ; CMD55
    MOVE.B  #$55,D1
    LEA     CMD55,A0
    BSR     SD_CMD
    CMP.B   #$01,D0
    BNE     .ERR

    ; ACMD41
    MOVE.B  #$41,D1
    LEA     ACMD41,A0
    BSR     SD_CMD
    CMP.B   #$01,D0
    BEQ     .INIT_LOOP  ; Card is still busy, repeat init loop
    CMP.B   #$00,D0
    BNE     .ERR

    ; Card initialized
    MOVE.B  #$0,D2      ; D2 = OK
    BRA     .END
.ERR:
    MOVE.B  #$1,D2      ; D2 = ERR
.END:
    MOVEM.L (SP)+,A0
    RTS

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
; SD_CMD: Sends CMD
; Inputs: A0 command pointer
; Output: D0 result
; -----------------------------------------
SD_CMD:
    MOVEM.L A0,-(SP)
    MOVE.B  #$00,SD_CTRL    ; Set CS low (active)
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
    MOVEM.L D1,-(SP)
.WAIT:
    MOVE.B  SD_CTRL,D1      ; Check status
    BTST    #6,D1           ; Test TX Ready
    BEQ     .WAIT           ; Bit 6 = 0 wait
    MOVE.B  D0,SD_DATA      ; Write data
    MOVEM.L (SP)+,D1
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
CMD0        DC.B  $40, $00, $00, $00, $00, $95
CMD8        DC.B  $48, $00, $00, $01, $AA, $87
CMD55       DC.B  $77, $00, $00, $00, $00, $FF
ACMD41      DC.B  $69, $40, $00, $00, $00, $FF

; Messages
MSG_INIT    DC.B 'INIT SD CARD... ',NUL
MSG_OK      DC.B 'OK',LF,NUL
MSG_ERR     DC.B 'ERR',LF,NUL
MSG_BYTE    DC.B 'BYTE: ',NUL
MSG_CMD     DC.B 'CMD: ',NUL

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
