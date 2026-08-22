package com.ttnt.chinesschess.chess;

import android.graphics.Point;

public class CBishop extends Piece {

    static final int[][] BISHOP_TABLE = {
            {0, 0, 0, 0, 0, 0, 0, 0, 0}, /* BISHOP */
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 19, 0, 19, 0, 0, 0},
            {0, 0, 0, 0, 22, 0, 0, 0, 0},
            {0, 0, 0, 20, 0, 20, 0, 0, 0}
    };

    public CBishop(Board board, Point currMove) {
        super(board, currMove);
    }

    CBishop(Board board) {
        super(board);
    }

    @Override
    void generate() {
        int[] dx = {1, 1, -1, -1};
        int[] dy = {1, -1, 1, -1};
        for (int i = 0; i < dx.length; i++) {
            int x = currMove.x + dx[i];
            int y = currMove.y + dy[i];
            if (x >= 0 && x <= 9 && y >= 3 && y <= 5) {
                byte piece = board.cell[currMove.x][currMove.y];
                byte occupant = board.cell[x][y];
                if ((red && ((occupant >= 8 && occupant <= 14) || occupant == 0) && x <= 2) || (!red && (occupant > 14 || occupant == 0) && x >= 7)) {
                    offer(x, y, piece, occupant);
                }
            }
        }
    }
}
