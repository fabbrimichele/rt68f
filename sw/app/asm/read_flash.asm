    ; ===========================
    ; Program code
    ; ===========================
    ORG     $400                ; Start of RAM

START:
    MOVE.L  #$0,FLASH_ADDR      ; Reset Flash address
    MOVE.W  #255,D2             ; Bytes to read - 1
    LEA     DATA,A0

LOOP:
    MOVE.B  #CTRL_RD,FLASH_CTRL ; Read command
WAIT:
    ; Status should be checked after sending the
    ; command to wait for the flash to be read.
    TST.B   FLASH_CTRL          ; Is Flash ready (bit 7)?
    BMI     WAIT                ; Busy if set to 1 (negative test)
    MOVE.B  FLASH_DATA,(A0)+    ; Copy byte read to SRAM
    DBRA    D2,LOOP             ; Read next byte (address autoincrements)

END:
    TRAP    #14


    ORG     $1000                ; Start copied data
DATA:
    DS.B    256


    ; ===========================
    ; Constants
    ; ===========================
FLASH_BASE  EQU     $404000
FLASH_CTRL  EQU     FLASH_BASE+$1   ; Lower byte contains actual status and control bits
FLASH_DATA  EQU     FLASH_BASE+$3   ; Lower byte contains data
FLASH_ADDR  EQU     FLASH_BASE+$4   ; 4 bytes

CTRL_RD     EQU     1