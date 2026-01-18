; ------------------------------
; ROM Bootloader
; ------------------------------

    SECTION .text, code
; ------------------------------
; Reset Vectors Section
; ------------------------------
    DC.L _bss_start         ; Reset Stack Pointer (SP, SP move downward far from bootloader work ram (bss))
    DC.L START              ; Reset Program counter (PC) (point to the beginning of code)

; ------------------------------
; Main Code Section
; ------------------------------
START:
    JSR     UART_INIT
    JSR     INIT_VECTOR_TABLE
    LEA     MSG_SELECT,A0
    BSR     PUTS
SELECT:
    LEA     KEY,A1          ; Load KEY register address into A1
    MOVE.L  #200,D2         ; Wait 2 seconds before load from Flash
SEL_LOOP:
    MOVE.L  #10,D0
    JSR     DELAY_MS        ; Wait 10ms
    MOVE.W  (A1),D1
    BTST    #KEY_DOWN,D1    ; Key down pressed?
    BNE     BOOT_SER        ; Yes, jump to boot serial
    DBRA    D2,SEL_LOOP
BOOT_FLASH:
    LEA     MSG_BOOT_FROM,A0
    BSR     PUTS
    LEA     MSG_BOOT_FLASH,A0
    BSR     PUTS
    BSR     LOAD_FLASH     ; Load program from SPI flash
    BRA     DONE
BOOT_SER:
    LEA     MSG_BOOT_FROM,A0
    BSR     PUTS
    LEA     MSG_BOOT_SERIAL,A0
    BSR     PUTS
    BSR     LOAD_SERIAL     ; Load program from serial
DONE:
    LEA     MSG_DONE,A0
    BSR     PUTS
    JMP     (A1)            ; Start program
    ;JSR     DUMP
STOP:
    BRA     STOP

INIT_VECTOR_TABLE:
    MOVE.L  #SPURIOUS_HANDLER,VT_INT_SP
    MOVE.L  #TRAP_14_HANDLER,VT_TRAP_14
    RTS

SPURIOUS_HANDLER:
    RTE

; The trap handler is not strictly necessary but it's
; useful to reuse existing programs which relay on it.
TRAP_14_HANDLER:
    MOVE.L  #_bss_start,SP
    LEA     MSG_PRG_RETURN,A0
    BSR     PUTS
    JMP     STOP

; -------------------------------------------------------------------------
; Load File Structure
; PROTOCOL: Length-Prefixed Binary (Big-Endian)
;
; HEADER (8 bytes, sent first):
; [32-bit Load Address] (A0)
; [32-bit Content Length] (D2)
;
; BODY:
; [L bytes of raw binary content]
;
; Example (File Content in Hex Bytes):
; 00 00 08 10 ; Load Address: $00000810
; 00 00 00 02 ; Content Length: 2 bytes (it doesn't include the headers)
; 55 55       ; Actual Content: Two bytes ($55, $55)
; GTKTerm format:
; 00;00;08;10;00;00;00;02;55;55
; -------------------------------------------------------------------------

; -------------------------------------------------------------------------
; Load from FLASH a binary content to memory.
; File expcted to be at Flash address: $80000
; Return start address in A1.
; -------------------------------------------------------------------------
LOAD_FLASH:
    MOVE.L  #$80000,FLASH_ADDR  ; Flash address to read from
    BSR     FLASH_RD_LONG
    MOVE.L  D0,A0               ; A0 start address, for loading
    MOVE.L  D0,A1               ; A0 start address, to be returned
    BSR     FLASH_RD_LONG       ; D1 content length in bytes
    MOVE.L  D0,D1
    LSR     #1,D1               ; D1 content length in words (files have always even length)
    CMP     #0,D1
    BEQ     LOAD_FLASH_DONE     ; If D1 = 0, exit

    SUBQ.L  #1,D1               ; Decrement counter (required by DBRA)
LOAD_FLASH_LOOP:
    MOVE.B  #FL_CMD_RD,FLASH_CTRL ; Read command
LOAD_FLASH_WAIT:
    TST.B   FLASH_CTRL          ; Is Flash ready (bit 7)?
    BMI     LOAD_FLASH_WAIT     ; Busy if set to 1 (negative test)
    MOVE.W  FLASH_DATA,(A0)+    ; Copy byte read to SRAM
    DBRA    D1,LOAD_FLASH_LOOP  ; Read next word, or terminate

LOAD_FLASH_DONE:
; TODO: Load - Add checksum at the end
    RTS

FLASH_RD_LONG:
    BSR     FLASH_RD            ; Read a word in D0
    LSL.L   #8,D0               ;
    LSL.L   #8,D0               ;
    BSR     FLASH_RD            ; Read a word in D0
    RTS

; Read one word from flash
; Address should already be set
; Return the word in D0
FLASH_RD:
    MOVE.B  #FL_CMD_RD,FLASH_CTRL ; Read command
FLASH_WAIT:
    TST.B   FLASH_CTRL            ; Is Flash ready (bit 7)?
    BMI     FLASH_WAIT            ; Busy if set to 1 (negative test)
    MOVE.W  FLASH_DATA,D0
    RTS


; -------------------------------------------------------------------------
; Load from UART a binary content to memory.
; Return start address in A1.
; -------------------------------------------------------------------------
LOAD_SERIAL:
    ; Read header start address (32 bits)
    JSR     READ_32BIT_WORD     ; Result in D1.L
    MOVE.L  D1,A0               ; A0 start address, for loading
    MOVE.L  D1,A1               ; A0 start address, to be returned
    ; Read content length
    JSR     READ_32BIT_WORD     ; Result in D1.L
                                ; D1 content lenght
    CMP     #0,D1
    BEQ     LOA_SER_DONE        ; If D1 = 0, exit
    SUBQ.L  #2,D1               ; Decrement counter (required by DBRA)

    ; Read content
LOA_SER_LOOP:
    JSR     GETCHAR             ; Read byte from UART to D0
    MOVE.B  D0,(A0)+            ; Copy read byte to memory
    DBRA    D1,LOA_SER_LOOP     ; Decrement D1, if != -1 exit

LOA_SER_DONE:
    RTS

; TODO: Load - Add checksum at the end

; -------------------------------------------------------------
; READ_32BIT_WORD: Reads 4 bytes from UART and assembles into D1.L
; Input: None
; Output: D1.L = 32-bit value
; Uses: GETCHAR (assumed to return 8-bit char in D0.B)
; -------------------------------------------------------------
READ_32BIT_WORD:
    MOVEM.L D0/D2,-(SP)     ; Save D0 (used for GETCHAR) and D2 (used for loop counter)

    MOVEQ   #4-1,D2         ; D2 = 3 (loop 4 times for 4 bytes)
    CLR.L   D1              ; D1 = Accumulator (cleared for the 32-bit result)

READ_LOOP:
    BSR     GETCHAR         ; D0.B = Get one byte from the serial port

    ; 1. Shift the current result (D1) left by 8 bits (makes room for the new byte)
    LSL.L   #8,D1

    ; 2. OR the new byte (D0.B) into the least significant position of D1
    OR.B    D0,D1

    DBRA    D2,READ_LOOP    ; Loop 4 times total (D2 counts down from 3)

    MOVEM.L (SP)+,D0/D2      ; Restore registers
    RTS

; A1 - Dump address
DUMP:
    MOVE.W  #(8-1),D1       ; Print 8 lines
DUMP_LINE:
    MOVE.L  A1,D0
    BSR     BINTOHEX        ; Print address
    MOVE.B  #':',D0
    BSR     PUTCHAR
    MOVE.W  #(8-1),D2       ; Print 8 cells
DUMP_CELL:
    MOVE.B  #' ',D0
    BSR     PUTCHAR
    MOVE.W  (A1)+,D0
    BSR     BINTOHEX_W      ; Print mem value
    DBRA    D2,DUMP_CELL    ; Decrement D1, branch if D1 is NOT -1

    MOVE.B  #LF,D0
    BSR     PUTCHAR
    DBRA    D1,DUMP_LINE    ; Decrement D1, branch if D1 is NOT -1
    RTS

; ---------------------------------------------------------
; DELAY_MS
; Input:  D0.W = Number of milliseconds to wait
; Assumes: 16MHz CPU Clock
; ---------------------------------------------------------
DELAY_MS:
    MOVE.L  D1,-(SP)        ; Save D1

OUTER_LOOP:
    ; 1ms is 16,000 cycles.
    ; The inner loop (DBRA) is 12 cycles. 16000 / 12 approx 1333.
    MOVE.W  #1333,D1
INNER_LOOP:
    DBRA    D1,INNER_LOOP   ; Inner loop (1ms)
    DBRA    D0,OUTER_LOOP   ; Outer loop (number of ms)

    MOVE.L  (SP)+,D1        ; Restore D1
    RTS

; ------------------------------
; Libraries
; ------------------------------
    INCLUDE '../../lib/asm/console_io_16450.asm'
    INCLUDE '../../lib/asm/spi_flash.asm'
    INCLUDE '../../lib/asm/key.asm'
    INCLUDE '../../lib/asm/isr_vector.asm'
    INCLUDE '../../lib/asm/conversions.asm'

; ------------------------------
; ROM Data Section
; ------------------------------
; Messages
MSG_SELECT      DC.B LF,'Press <down> to boot from serial (2s).',LF,NUL
MSG_BOOT_FROM   DC.B 'Booting from ',NUL
MSG_BOOT_FLASH  DC.B 'flash...',NUL
MSG_BOOT_SERIAL DC.B 'serial...',NUL
MSG_DONE        DC.B ' OK',LF,LF,NUL
MSG_PRG_RETURN  DC.B 'Program returned, press reset to restart.',LF,NUL

; ------------------------------
; RAM Data Section (bootloader mem)
; ------------------------------
    SECTION .bss

; No data required so far.

; ===========================
; Constants
; ===========================
; ASCII
CR          EQU 13          ; Carriage Return
LF          EQU 10          ; Line Feed
BEL         EQU 7           ; Bell character
NUL         EQU 0
