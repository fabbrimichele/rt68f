; D0 delay value
DELAY:
    SUBQ.L  #1,D0           ; Decrement counter
    BNE     DELAY           ; Loop until D0 is zero
    RTS
