package com.ttnt.chinesschess.chess;

import android.graphics.Point;

public class CKing extends Piece {

    static final int[][] KING_TABLE = {
            {0, 0, 0, 0, 0, 0, 0, 0, 0}, /* KING */
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 1, 1, 1, 0, 0, 0},
            {0, 0, 0, 15, 15, 15, 0, 0, 0},
            {0, 0, 0, 30, 35, 30, 0, 0, 0}
    };

    public CKing(Board board, Point currMove) {
        super(board, currMove);
    }

    CKing(Board board) {
        super(board);
    }

    @Override
    void generate() {
        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};
        for (int i = 0; i < dx.length; i++) {
            int x = currMove.x + dx[i];
            int y = currMove.y + dy[i];
            if (x >= 0 && x <= 9 && y >= 0 && y <= 8) {
                byte piece = board.cell[currMove.x][currMove.y];
                byte occupant = board.cell[x][y];
                if (((red && ((occupant >= 8 && occupant <= 14) || occupant == 0) && x <= 2) || (!red && (occupant > 14 || occupant == 0) && x >= 7)) && (y >= 3 && y <= 5)) {
                    offer(x, y, piece, occupant);
                }
            }
        }
    }
}