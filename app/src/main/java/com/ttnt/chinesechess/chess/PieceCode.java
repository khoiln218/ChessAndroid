package com.ttnt.chinesechess.chess;

/**
 * How a square of {@link Board#cell} says what stands on it. Empty is 0; Black's seven kinds run
 * from 8 to 14 and Red's the same seven, in the same order, from 15 to 21 - so a piece's kind is
 * its offset from its own side's general, and its side is which block it falls in.
 */
public final class PieceCode {

    public static final byte EMPTY = 0;

    public static final byte BLACK_KING = 8;
    public static final byte BLACK_ADVISOR = 9;
    public static final byte BLACK_ELEPHANT = 10;
    public static final byte BLACK_KNIGHT = 11;
    public static final byte BLACK_ROOK = 12;
    public static final byte BLACK_CANNON = 13;
    public static final byte BLACK_PAWN = 14;

    public static final byte RED_KING = 15;
    public static final byte RED_ADVISOR = 16;
    public static final byte RED_ELEPHANT = 17;
    public static final byte RED_KNIGHT = 18;
    public static final byte RED_ROOK = 19;
    public static final byte RED_CANNON = 20;
    public static final byte RED_PAWN = 21;

    /** One past the highest code: the length of an array indexed by code. */
    public static final int COUNT = RED_PAWN + 1;

    /** Kinds, as the offset of a code from its own side's general. */
    public static final int KING = 0;
    public static final int ADVISOR = 1;
    public static final int ELEPHANT = 2;
    public static final int KNIGHT = 3;
    public static final int ROOK = 4;
    public static final int CANNON = 5;
    public static final int PAWN = 6;
    /** How many kinds there are: the length of an array indexed by kind. */
    public static final int KINDS = PAWN + 1;

    private PieceCode() {
    }

    /** Whether {@code code} is a red piece. */
    public static boolean isRed(byte code) {
        return code >= RED_KING;
    }

    /** Whether {@code code} is a black piece. */
    public static boolean isBlack(byte code) {
        return code >= BLACK_KING && code <= BLACK_PAWN;
    }

    /** Whether {@code code} is a piece of {@code red}'s side - red if {@code true}. */
    public static boolean belongsTo(byte code, boolean red) {
        return code >= BLACK_KING && isRed(code) == red;
    }

    /** The kind of piece {@code code} is, {@link #KING} to {@link #PAWN}. */
    public static int kind(byte code) {
        return isRed(code) ? code - RED_KING : code - BLACK_KING;
    }

    /** The code of a {@code kind} piece of {@code red}'s side. */
    public static byte of(int kind, boolean red) {
        return (byte) ((red ? RED_KING : BLACK_KING) + kind);
    }
}
