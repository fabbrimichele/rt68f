MEMORY
{
    /* The hardware maps the first 8 bytes to ROM */
    RAM (rwx)   : ORIGIN = 0x00000400, LENGTH = 512K - 0x400
    ROM (rx)    : ORIGIN = 0x00300000, LENGTH = 1122
    FB  (rwx)   : ORIGIN = 0x00200000, LENGTH = 64000
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

    .fb (NOLOAD) : {
        _fb_start = .;
        *(.fb*)
        _fb_end = .;
    } > FB
}

