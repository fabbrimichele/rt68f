; TODO: the VGA FB address should be read from a linker memory map definition
VGA         EQU     $00200000   ; VGA framebuffer base address
VGA_LEN     EQU     $4000       ; VGA framebuffer length in words
VGA_PALETTE EQU     $00403000   ; VGA Control (screen mode)
VGA_CTRL    EQU     $00403100   ; VGA Control (screen mode)
