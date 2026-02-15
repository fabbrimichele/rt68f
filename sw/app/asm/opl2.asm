    ; ===========================
    ; Program code
    ; ===========================
    ORG     $400                ; Start of RAM

START:
    BSR     MUTE_ALL
    BSR     PLAY
    BSR     DELAYL
    BSR     MUTE_ALL
    TRAP    #14


DELAYL:
    MOVE.L  #9333333,D0     ;
.LOOP:
    SUBQ.L  #1,D0           ; 4 cycles
    BNE     .LOOP        ; 10 cycles when taken
    RTS


;//Set up a patch
;for( uint8_t ch=0; ch<YM3812_NUM_CHANNELS; ch++){   // Use the same patch for all channels
;  op1_index = PROC_YM3812.channel_map[ch];
;  op2_index = op1_index + 3;                        // Always 3 higher
;
;  //Channel settings
;  PROC_YM3812.regChAlgorithm(  ch, 0x1 ); // Algorithm (Addative synthesis)
;  PROC_YM3812.regChFeedback(   ch, 0x0 ); // Feedback
;
;  //Operator 1's settings
;  PROC_YM3812.regOpAttack(   op1_index, 0xB );
;  PROC_YM3812.regOpDecay(    op1_index, 0x6 );
;  PROC_YM3812.regOpSustain(  op1_index, 0xA );
;  PROC_YM3812.regOpRelease(  op1_index, 0x2 );
;  PROC_YM3812.regOpLevel(    op1_index, 0x0 );      // 0 - loudest, 64 (0x40) is softest
;  PROC_YM3812.regOpWaveForm( op1_index, 0x1 );
;
;  //Operator 2's settings
;  PROC_YM3812.regOpAttack(   op2_index, 0xB );
;  PROC_YM3812.regOpDecay(    op2_index, 0x6 );
;  PROC_YM3812.regOpSustain(  op2_index, 0xA );
;  PROC_YM3812.regOpRelease(  op2_index, 0x2 );
;  PROC_YM3812.regOpLevel(    op2_index, 0x0 );      // 0 - loudest, 64 (0x40) is softest
;  PROC_YM3812.regOpWaveForm( op2_index, 0x1 );
;}

; First, we turn off any notes that might happen to be on:
; PROC_YM3812.regKeyOn(  0, false ); //Turn Channel 0 off
; PROC_YM3812.regKeyOn(  1, false ); //Turn Channel 1 off
; PROC_YM3812.regKeyOn(  2, false ); //Turn Channel 2 off
; PROC_YM3812.regKeyOn(  3, false ); //Turn Channel 3 off
;
;Then we set the octave we want the notes to be in:
; PROC_YM3812.regFrqBlock( 0, 4); //Fourth Octave
; PROC_YM3812.regFrqBlock( 1, 4); //Fourth Octave
; PROC_YM3812.regFrqBlock( 2, 4); //Fourth Octave
;  PROC_YM3812.regFrqBlock( 3, 4); //Fourth Octave
;
;Then we set the frequency for each of the notes:
; PROC_YM3812.regFrqFnum( 0, 0x1C9 ); // F
; PROC_YM3812.regFrqFnum( 1, 0x240 ); // A
; PROC_YM3812.regFrqFnum( 2, 0x2AD ); // C
; PROC_YM3812.regFrqFnum( 3, 0x360 ); // E
;Wait… 0x1C9, 0x240, 0x2AD and 0x360 look nothing like F A C E… Nothing to see here… we will talk about that in the next article. Anyway… Finally, we turn the notes on:
;  //Turn on all of the notes one at a time
;  PROC_YM3812.regKeyOn(  0, true );
;  delay(100);
;  PROC_YM3812.regKeyOn(  1, true );
;  delay(100);
;  PROC_YM3812.regKeyOn(  2, true );
;  delay(100);
;  PROC_YM3812.regKeyOn(  3, true );
;  delay(1000);



PLAY:
    ; Settings on channel 0

    ; -- Set algorithm to 1 and feedback to 0 --
    ; Register $C0, value $01
    MOVE.W  #$C0,D0         ; OPL2 address
    MOVE.W  #$01,D1         ; OPL2 data
    BSR     OPL2_PUT        ; Write to OPL2

    ; -- Operator 1: Set the Attack/Decay and Sustain/Release (ADSR) --
    ; Register $60: Attack=0xB (fastest), Decay=0x6
    MOVE.W  #$60,D0         ; OPL2 address
    MOVE.W  #$B6,D1         ; OPL2 data
    BSR     OPL2_PUT        ; Write to OPL2
    ; Register $80: Sustain=0xA (loudest), Release=0x2 (fastest)
    MOVE.W  #$80,D0         ; OPL2 address
    MOVE.W  #$A2,D1         ; OPL2 data
    BSR     OPL2_PUT        ; Write to OPL2
    ; Level $40: $0
    MOVE.W  #$40,D0         ; OPL2 address
    MOVE.W  #$00,D1         ; OPL2 data
    BSR     OPL2_PUT        ; Write to OPL2
    ; Waveform $E0: $1
    MOVE.W  #$E0,D0         ; OPL2 address
    MOVE.W  #$01,D1         ; OPL2 data
    BSR     OPL2_PUT        ; Write to OPL2

    ; -- Operator 2: Set the Attack/Decay and Sustain/Release (ADSR) --
    ; Register $63: Attack=0xB (fastest), Decay=0x6
    MOVE.W  #$63,D0         ; OPL2 address
    MOVE.W  #$B6,D1         ; OPL2 data
    BSR     OPL2_PUT        ; Write to OPL2
    ; Register $83: Sustain=0xA (loudest), Release=0x2 (fastest)
    MOVE.W  #$83,D0         ; OPL2 address
    MOVE.W  #$A2,D1         ; OPL2 data
    BSR     OPL2_PUT        ; Write to OPL2
    ; Level $43: $0
    MOVE.W  #$43,D0         ; OPL2 address
    MOVE.W  #$00,D1         ; OPL2 data
    BSR     OPL2_PUT        ; Write to OPL2
    ; Waveform $E3: $1
    MOVE.W  #$E3,D0         ; OPL2 address
    MOVE.W  #$01,D1         ; OPL2 data
    BSR     OPL2_PUT        ; Write to OPL2


    ; 1. Set the Characteristic of Operator 1 (Channel 0)
    ; Register 0x20: Tremolo, Vibrato, Sustain, KSR, Multiplier
    ; 0x01 = No effects, Multiplier = 1
    MOVE.W  #$20,D0         ; OPL2 address
    MOVE.W  #$01,D1         ; OPL2 data
    BSR     OPL2_PUT        ; Write to OPL2

    ; 2. Set the Volume/Attenuation of Operator 1
    ; Register 0x40: KSL and Total Level
    ; 0x00 = Loudest volume (0dB attenuation)
    MOVE.W  #$40,D0         ; OPL2 address
    MOVE.W  #$00,D1         ; OPL2 data
    BSR     OPL2_PUT        ; Write to OPL2

    ; 1. Set the Characteristic of Operator 2 (Channel 0)
    ; Register 0x23: Tremolo, Vibrato, Sustain, KSR, Multiplier
    ; 0x01 = No effects, Multiplier = 1
    MOVE.W  #$23,D0         ; OPL2 address
    MOVE.W  #$01,D1         ; OPL2 data
    BSR     OPL2_PUT        ; Write to OPL2

    ; 2. Set the Volume/Attenuation of Operator 2
    ; Register 0x40: KSL and Total Level
    ; 0x00 = Loudest volume (0dB attenuation)
    MOVE.W  #$43,D0         ; OPL2 address
    MOVE.W  #$00,D1         ; OPL2 data
    BSR     OPL2_PUT        ; Write to OPL2


    ; 4. Set the Frequency and Turn the Note ON
    ; Register 0xA0: Lower 8 bits of frequency (F-Number)
    MOVE.W  #$A0,D0         ; OPL2 address
    MOVE.W  #$FF,D1         ; OPL2 data
    BSR     OPL2_PUT        ; Write to OPL2

    ; Register 0xB0: Key-On, Octave (Block), and upper 2 bits of F-Number
    ; 0x21: 001 000 01 -> KeyOn = 1, Block = 4, F-Number MSB = 1
    MOVE.W  #$B0,D0         ; OPL2 address
    MOVE.W  #$21,D1         ; OPL2 data
    BSR     OPL2_PUT        ; Write to OPL2

    RTS


MUTE_ALL:
    MOVE.L  D2,-(SP)        ; Save D1

    ; --- Key-Off all 9 channels ---
    MOVE.B  #8,D2           ; Loop 9 times
    MOVE.W  #$B0,D0         ; OPL2 address
    MOVE.W  #$00,D1         ; OPL2 data
.LOOP_KEYS:
    BSR     OPL2_PUT        ; Write to OPL2
    ADDQ.W  #1,D0           ; Next OPL2 register
    DBRA    D2,.LOOP_KEYS   ; D2 = D2 - 1. If D2 != -1, jump to loop_start

    ; --- Max Attenuation for all 18 Operators ---
    ; Registers $40-$55 (22 registers total in that range,
    ; but some are empty. Writing to all is safer.)
    MOVE.W  #21,D2          ; Loop 22 times ($40 to $55)
    MOVE.W  #$40,D0         ; Level/KSL registers
    MOVE.W  #$3F,D1         ; $3F = 63 (Max attenuation/Silence)
.LOOP_VOLS:
    BSR     OPL2_PUT
    ADDQ.W  #1,D0
    DBRA    D2,.LOOP_VOLS

    ; --- Done ---
    MOVE.L  (SP)+,D2        ; Restore D1
    RTS


; ===========================
; Include files
; ===========================
    INCLUDE '../../lib/asm/opl2.asm'
