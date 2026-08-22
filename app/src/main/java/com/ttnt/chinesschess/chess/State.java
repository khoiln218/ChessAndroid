package com.ttnt.chinesschess.chess;

import android.graphics.Point;

/**
 * One move: the two squares, and the two pieces the squares held before it was made. Carrying
 * what was taken is what lets a move be undone from the move alone, which the search does
 * millions of times per turn without ever copying the board.
 */
public class State {

    /** The square the piece left. */
    public Point from;
    /** The square it landed on. */
    public Point to;
    /** The piece that moved. */
    public byte piece;
    /** What stood on {@link #to} and was taken, or 0 if the square was empty. */
    public byte captured;

    public State(Point from, Point to, byte piece, byte captured) {
        this(from, to.x, to.y, piece, captured);
    }

    /**
     * The target as plain coordinates. Move generation has them as numbers already, and this is
     * on the hottest path in the app - a point built only to be copied is one object per move.
     */
    public State(Point from, int x, int y, byte piece, byte captured) {
        this.from = new Point(from);
        this.to = new Point(x, y);
        this.piece = piece;
        this.captured = captured;
    }
}
