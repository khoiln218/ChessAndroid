package com.ttnt.chinesechess.chess;

import java.util.Arrays;

/**
 * A move already played, and what it did. Kept for every move of the game, because judging a
 * repetition means looking back at how the position was reached, not just that it repeated.
 */
public final class MoveRecord {
    /**
     * The squares after the move, row by row - what stood on each of the ninety. Kept whole
     * rather than as a hash, so two positions are only ever the same when they really are.
     */
    private final byte[] squares;
    public final boolean redMoved;
    /** Whether the move left the other side in check. */
    public final boolean gaveCheck;
    /** Whether it threatened to take an undefended enemy piece other than the general. */
    public final boolean chasing;

    MoveRecord(byte[] squares, boolean redMoved, boolean gaveCheck, boolean chasing) {
        this.squares = squares;
        this.redMoved = redMoved;
        this.gaveCheck = gaveCheck;
        this.chasing = chasing;
    }

    /** What stands on row {@code x}, column {@code y} after the move. */
    public byte at(int x, int y) {
        return squares[x * Board.COL + y];
    }

    /** Whether red is to move after it. */
    public boolean redToMove() {
        return !redMoved;
    }

    /** The same squares with the same side to move. */
    public boolean samePosition(MoveRecord other) {
        return redMoved == other.redMoved && Arrays.equals(squares, other.squares);
    }
}
