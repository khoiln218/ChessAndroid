package com.ttnt.chinesechess.chess;

/**
 * A square on the board: {@code x} is the row, 0 to 9 from Red's side, and {@code y} the column,
 * 0 to 8. The rules' own, so the chess package needs nothing from Android and runs on a plain JVM.
 * Mutable, like the pieces that walk one of these around the board instead of making new ones.
 */
public final class Point {

    public int x;
    public int y;

    public Point() {
    }

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Point(Point other) {
        this(other.x, other.y);
    }
}
