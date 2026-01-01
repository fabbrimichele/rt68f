    ; ===========================
    ; Program code
    ; ===========================
    ORG     $400                ; Start of RAM

START:
    MOVE.W  #$0,FLASH_ADDRH     ; Reset hi address (only low is used in this test)
    MOVE.W  #$0,D1              ; Start Flash address
    MOVE.W  #255,D2             ; Bytes to read - 1
    LEA     DATA,A0

LOOP:
    MOVE.W  D1,FLASH_ADDRL      ; Set flash address
    MOVE.W  #CTRL_RD,FLASH_CTRL ; Read command
WAIT:
    ; Status should be checked after sending the
    ; command to wait for the flash to be read.
    MOVE.W  FLASH_CTRL,D0
    BTST    #7,D0               ; Is Flash ready?
    BNE     WAIT                ; Busy if not set to zero
    MOVE.B  FLASH_DATA,(A0)+    ; Copy byte read to SRAM
    ADDQ.W  #1,D1               ; Next address
    DBRA    D2,LOOP             ; Next read until all bytes read

END:
    TRAP    #14


    ORG     $1000                ; Start copied data
DATA:
    DS.B    256


    ; ===========================
    ; Constants
    ; ===========================
FLASH_BASE  EQU     $404000
FLASH_CTRL  EQU     FLASH_BASE
FLASH_DATA  EQU     FLASH_BASE+$3   ; Lower byte contains data
FLASH_ADDR  EQU     FLASH_BASE+$4
FLASH_ADDRH EQU     FLASH_BASE+$4
FLASH_ADDRL EQU     FLASH_BASE+$6

CTRL_RD     EQU     1