    ; ===========================
    ; Program code
    ; ===========================
    ORG     $400                ; Start of RAM

START:
    ; Configure SPI for 8 bits lenght and max speed 8 MHz
    MOVE.B  #%00001000,SPI_CONF ; LENGTH = 01 (8 bits), DIVIDE = 000 (clk/2)

    ; Assert CS
    MOVE.B  #%00010010,SPI_CDST ; SPIAD = 001, IRQ = 0, CS = 1, START = 0

    MOVE.L  #$00080000,D0       ; Start flash memory address
    BSR     READ_COMMAND        ; Send read command and address

    MOVE.W  #15,D2              ; Read 16 words (-1 for DBRA)
.LOOP:
    BSR     READ_BYTE           ; Read a MSB in D0.B
    LSL.W   #8,D0               ; Shift MSB in D0.W
    BSR     READ_BYTE           ; Read a MSB in D0.B
    BSR     PRINT_W             ; Print D0.W
    DBRA    D2,.LOOP            ; Loop 16 times

    ; Deassert CS
    MOVE.B  #%00010000,SPI_CDST  ; SPIAD = 001, IRQ = 0, CS = 0, START = 0
END:
    BRA     END
    ;TRAP   #14


;--------------------------------
; Send read command and address
; (3 bytes) to SPI.
; D0.L flash memory address
;--------------------------------
READ_COMMAND:
    ; Send read command $03
    MOVE.L  D0,D1
    MOVE.B  #$03,D0
    BSR     SEND_BYTE
    MOVE.L  D1,D0

    ; Send address
    ; D0: [XX][AA][BB][CC]
    SWAP    D0                  ; D0: [BB][CC][XX][AA]
    BSR     SEND_BYTE           ; Send [AA]
    ROL.L   #8,D0               ; D0: [CC][XX][AA][BB]
    BSR     SEND_BYTE           ; Send [BB]
    ROL.L   #8,D0               ; D0: [XX][AA][BB][CC]
    BSR     SEND_BYTE           ; Send [CC]
    RTS

;--------------------------------
; Read a byte from SPI
; D0 byte to read
;--------------------------------
READ_BYTE:
    MOVE.B  #$FF,D0             ; Trigger 8 clock pulses
    BSR     SEND_BYTE
    BSR     WAIT_READY
    MOVE.B  SPI_DTLW,D0
    RTS

;--------------------------------
; Send a byte to SPI
; D0 byte to send
;--------------------------------
SEND_BYTE:
    ;BSR    PRINT_W              ; DEBUG, print D0.W
    BSR     WAIT_READY
    MOVE.B  D0,SPI_DTLW
    MOVE.B  #%00010011,SPI_CDST  ; SPIAD = 001, IRQ = 0, CS = 1, START = 1
    RTS

;--------------------------------
; Wait for BUSY
;--------------------------------
WAIT_READY:
    BTST    #0,SPI_CDST     ; Test bit 0 of the Status Register
    BNE     WAIT_READY      ; Branch if Not Equal (to zero).
    RTS

;--------------------------------
; Print D0.L
;--------------------------------
PRINT_W:
    MOVEM.L D0,-(SP)
    BSR     BINTOHEX_W
    MOVE.B  #13,D0
    BSR     PUTCHAR
    MOVEM.L (SP)+,D0
    RTS

; ===========================
; Include files
; ===========================
    INCLUDE '../../lib/asm/spi.asm'
    INCLUDE '../../lib/asm/led.asm'
    INCLUDE '../../lib/asm/console_io_16450.asm'
    INCLUDE '../../lib/asm/conversions.asm'
