;------------------------------------------------------------------------------------
; SPI FLASH
;------------------------------------------------------------------------------------
FLASH_BASE      EQU $440000
FLASH_CTRL      EQU FLASH_BASE+$1           ; Lower byte contains actual status and control bits
FLASH_DATA      EQU FLASH_BASE+$2           ; Word contains data
FLASH_ADDR      EQU FLASH_BASE+$4           ; 4 bytes

; Flash Constants
FL_CMD_RD   EQU 1           ; Read command
