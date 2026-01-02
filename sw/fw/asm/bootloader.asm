; Bootloader

; ------------------------------
; ROM Monitor
; ------------------------------
    ORG    $300000          ; ROM Start Address

; ------------------------------
; Initial Reset SP and PC in Vector Table
; ------------------------------
    DC.L SP_START           ; Reset Stack Pointer (SP, SP move downward far from SO_RAM)
    DC.L START              ; Reset Program counter (PC) (point to the beginning of code)

; ------------------------------
; Program code
; ------------------------------
START:
    JSR     UART_INIT
    JSR     INIT_VECTOR_TABLE
    LEA     MSG_BOOTING,A0
    BSR     PUTS
    BSR     LOAD_SER        ; Load program from serial
    LEA     MSG_DONE,A0
    BSR     PUTS
    JMP     (A1)            ; Start program

INIT_VECTOR_TABLE:
    MOVE.L  #TRAP_14_HANDLER,VT_TRAP_14
    RTS

; The trap handler is not strictly necessary but it's
; useful to reuse existing programs which relay on it.
TRAP_14_HANDLER:
    MOVE.L  #SP_START,SP
    JMP     START

; -------------------------------------------------------------------------
; Load from UART a binary content to memory.
; Return start address in A1.
;
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
LOAD_SER:
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

; ------------------------------
; Libraries
; ------------------------------
    INCLUDE 'lib/console_io_16450.asm'

; ------------------------------
; ROM Data Section
; ------------------------------

; Messages
MSG_BOOTING     DC.B    'Booting...',LF,NUL
MSG_DONE        DC.B    'Done',LF,NUL


; ===========================
; Constants
; ===========================
MON_MEM_LEN EQU 256                     ; RAM allocated for the monitor

; Memory Map
RAM_START       EQU $00000400               ; Start of RAM address (after the vector table)
RAM_END         EQU $00080000               ; End of RAM address (+1)
SP_START        EQU (RAM_END-MON_MEM_LEN)   ; After SP, allocates monitor RAM
MON_MEM_START   EQU SP_START                ;
FB_START        EQU $00200000               ; Start of Framebuffer
FB_END          EQU $0020FA00               ; End of Framebuffer (+1)
FB_LEN          EQU (FB_END-FB_START)       ; Framebuffer length
LED             EQU $00400000               ; LED-mapped register base address
; 16450 UART
UART_BASE       EQU $00402000               ; UART base address
UART_RBR        EQU UART_BASE+$0            ; Receive Buffer Register(RBR) / Transmitter Holding Register(THR) / Divisor Latch (LSB)
UART_IER        EQU UART_BASE+$2            ; Interrupt enable register / Divisor Latch (MSB)
UART_IIR        EQU UART_BASE+$4            ; Interrupt Identification Register
UART_LCR        EQU UART_BASE+$6            ; Line control register
UART_MCR        EQU UART_BASE+$8            ; MODEM control register
UART_LSR        EQU UART_BASE+$A            ; Line status register
UART_MSR        EQU UART_BASE+$C            ; MODEM status register
; NOTE: do not remove spaces around +

; Vector Table
VT_TRAP_14      EQU $B8

; Monitor RAM
; Allocated after the stack point, if the monitor needs
; more memory it's sufficient to move the stack pointer
; Buffer
IN_BUF          EQU MON_MEM_START           ; IN_BUF start after the stack pointer
IN_BUF_LEN      EQU 80                      ; BUFFER LEN should be less than MON_MEM_LEN EQU
IN_BUF_END      EQU IN_BUF+IN_BUF_LEN       ;

; ASCII
CR          EQU 13          ; Carriage Return
LF          EQU 10          ; Line Feed
BEL         EQU 7           ; Bell character
NUL         EQU 0
