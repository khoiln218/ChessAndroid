package com.ttnt.chinesechess.chess;

import android.graphics.Point;

public class CElephant extends Piece {

    static final int[][] ELEPHANT_TABLE = {
            {0, 0, 0, 0, 0, 0, 0, 0, 0}, /* ELEPHANT */
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 22, 0, 0, 0, 22, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {23, 0, 0, 0, 28, 0, 0, 0, 23},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 25, 0, 0, 0, 25, 0, 0}
    };

    public CElephant(Board board, Point currMove) {
        super(board, currMove);
    }

    CElephant(Board board) {
        super(board);
    }

    @Override
    void generate() {
        byte[] dx = {2, 2, -2, -2};
        byte[] dy = {2, -2, 2, -2};
        for (int i = 0; i < dx.length; i++) {
            int x = currMove.x + dx[i];
            int y = currMove.y + dy[i];
            if (x >= 0 && x <= 9 && y >= 0 && y <= 8) {
                byte piece = board.cell[currMove.x][currMove.y];
                byte occupant = board.cell[x][y];
                if (((red && ((occupant >= 8 && occupant <= 14) || occupant == 0) && x <= 4) || (!red && (occupant > 14 || occupant == 0) && x >= 5)) && legClear(x, y)) {
                    offer(x, y, piece, occupant);
                }
            }
        }
    }

    /** Whether the diagonal step is open: an elephant is blocked by a piece on its own eye. */
    boolean legClear(int x, int y) {
        int dx = x - currMove.x;
        int dy = y - currMove.y;
        if (Math.abs(dx) == 2 && Math.abs(dy) == 2) {
            if (dx > 0) {
                if (dy > 0) {
                    return board.cell[currMove.x + 1][currMove.y + 1] == 0;
                } else {
                    return board.cell[currMove.x + 1][currMove.y - 1] == 0;
                }
            } else {
                if (dy > 0) {
                    return board.cell[currMove.x - 1][currMove.y + 1] == 0;
                } else {
                    return board.cell[currMove.x - 1][currMove.y - 1] == 0;
                }
            }
        }
        return false;
    }
}
