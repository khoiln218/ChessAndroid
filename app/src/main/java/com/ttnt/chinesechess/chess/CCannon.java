package com.ttnt.chinesechess.chess;

import android.graphics.Point;

public class CCannon extends Piece {

    static final int[][] CANNON_TABLE = {
            {50, 50, 50, 50, 50, 50, 50, 50, 50}, /* CANNON */
            {50, 51, 50, 50, 50, 50, 50, 51, 50},
            {50, 51, 50, 50, 50, 50, 50, 51, 50},
            {50, 51, 50, 50, 50, 50, 50, 51, 50},
            {50, 51, 50, 50, 50, 50, 50, 51, 50},
            {50, 51, 51, 51, 51, 51, 51, 51, 50},
            {50, 51, 50, 50, 50, 50, 50, 51, 50},
            {50, 51, 53, 53, 55, 53, 53, 51, 50},
            {50, 50, 50, 50, 50, 50, 50, 50, 50},
            {50, 50, 50, 50, 50, 50, 50, 50, 50}
    };

    public CCannon(Board board, Point currMove) {
        super(board, currMove);
    }

    CCannon(Board board) {
        super(board);
    }

    @Override
    void generate() {
        int x = currMove.x;
        int y = currMove.y;
        for (int i = y + 1; i <= 8; i++) {
            if (rayStopsAt(x, i)) {
                break;
            }
        }
        for (int i = y - 1; i >= 0; i--) {
            if (rayStopsAt(x, i)) {
                break;
            }
        }
        for (int i = x + 1; i <= 9; i++) {
            if (rayStopsAt(i, y)) {
                break;
            }
        }
        for (int i = x - 1; i >= 0; i--) {
            if (rayStopsAt(i, y)) {
                break;
            }
        }
    }

    /**
     * One square along a ray out from the cannon. An empty square is somewhere it may move to.
     * The first piece in the way is its screen - the cannon takes by firing over exactly one
     * piece - so the line past that screen is walked for something to take. Either way the ray
     * ends at an occupied square.
     *
     * @return whether the ray ends here
     */
    private boolean rayStopsAt(int x, int y) {
        byte piece = board.cell[currMove.x][currMove.y];
        byte occupant = board.cell[x][y];
        if (occupant == 0) {
            offer(x, y, piece, occupant);
        } else {
            if (currMove.x == x) {
                if (y > currMove.y) {
                    for (y = y + 1; y <= 8; y++) {
                        if (shotStopsAt(x, y)) {
                            break;
                        }
                    }
                } else {
                    for (y = y - 1; y >= 0; y--) {
                        if (shotStopsAt(x, y)) {
                            break;
                        }
                    }
                }
            } else {
                if (x > currMove.x) {
                    for (x = x + 1; x <= 9; x++) {
                        if (shotStopsAt(x, y)) {
                            break;
                        }
                    }
                } else {
                    for (x = x - 1; x >= 0; x--) {
                        if (shotStopsAt(x, y)) {
                            break;
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    /**
     * One square on the far side of the screen. The first piece met there is the only one the
     * shot can reach, and it is taken only if it belongs to the other side; anything behind it
     * is shielded by it.
     *
     * @return whether the shot ends here
     */
    private boolean shotStopsAt(int x, int y) {
        byte piece = board.cell[currMove.x][currMove.y];
        byte occupant = board.cell[x][y];
        if (occupant != 0) {
            if ((red && occupant <= 14) || (!red && occupant > 14)) {
                offer(x, y, piece, occupant);
            }
            return true;
        }
        return false;
    }

}
