package com.ttnt.chinesechess.chess;

import android.graphics.Point;

public class CKnight extends Piece {

    static final int[][] KNIGHT_TABLE = {
            {40, 40, 40, 40, 40, 40, 40, 40, 40}, /* KNIGHT */
            {40, 41, 42, 42, 42, 42, 42, 41, 40},
            {40, 43, 44, 44, 44, 44, 44, 43, 40},
            {40, 43, 44, 44, 44, 44, 44, 43, 40},
            {40, 43, 44, 44, 44, 44, 44, 43, 40},
            {40, 43, 44, 44, 44, 44, 44, 43, 40},
            {40, 42, 43, 43, 43, 43, 43, 42, 40},
            {40, 42, 43, 40, 40, 40, 43, 42, 40},
            {40, 41, 42, 40, 20, 40, 42, 41, 40},
            {40, 35, 40, 40, 40, 40, 40, 35, 40}
    };

    public CKnight(Board board, Point currMove) {
        super(board, currMove);
    }

    CKnight(Board board) {
        super(board);
    }

    @Override
    void generate() {
        int[] dx = {1, 1, 2, 2, -1, -1, -2, -2};
        int[] dy = {2, -2, 1, -1, 2, -2, 1, -1};
        for (int i = 0; i < dx.length; i++) {
            int x = currMove.x + dx[i];
            int y = currMove.y + dy[i];
            if (x >= 0 && x <= 9 && y >= 0 && y <= 8) {
                byte piece = board.cell[currMove.x][currMove.y];
                byte occupant = board.cell[x][y];
                if ((occupant == 0 || ((red && occupant >= 8 && occupant <= 14) || (!red && occupant > 14))) && checkProject(x, y)) {
                    offer(x, y, piece, occupant);
                }
            }
        }
    }

    /** Whether a horse standing here could reach {@code target}: its leg must be clear. */
    private boolean checkProject(int x, int y) {
        int dx = x - currMove.x;
        int dy = y - currMove.y;
        int absX = Math.abs(dx);
        int absY = Math.abs(dy);
        if ((absX == 1 && absY == 2) || (absX == 2 && absY == 1)) {
            if (absX == 1) {
                if (dy > 0) {
                    return board.cell[currMove.x][currMove.y + 1] == 0;
                } else {
                    return board.cell[currMove.x][currMove.y - 1] == 0;
                }
            } else {
                if (dx > 0) {
                    return board.cell[currMove.x + 1][currMove.y] == 0;
                } else {
                    return board.cell[currMove.x - 1][currMove.y] == 0;
                }
            }
        }
        return false;
    }
}
