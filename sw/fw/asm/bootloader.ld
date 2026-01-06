/* Define Bootloader working memory size */
__work_mem_size  = 256;

MEMORY
{
    /* The hardware maps the first 8 bytes to ROM, but we define the actual ROM storage */
    ROM (rx)    : ORIGIN = 0x00300000, LENGTH = 1122
    RAM (rwx)   : ORIGIN = 0x00000400, LENGTH = 512K - 0x400
}

SECTIONS
{
    .text : {
        *(.text*)
        *(.rodata*)
    } > ROM

    .bss :
    {
        . = ORIGIN(RAM) + LENGTH(RAM) - __work_mem_size;
        _bss_start = .;
        *(.bss*)
    } > RAM
}