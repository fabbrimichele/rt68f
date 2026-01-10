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
    .user (NOLOAD) : {
        _user_start = .;
        *(.user*)
    } > RAM

    .text 0x0007C000 : {
        _init_sp = .;
        _text_start = .;
        *(.text*)
        *(.rodata*)
    } > RAM

    .bss (NOLOAD) : {
        _bss_start = .;
        *(.bss*)
    } > RAM
}

