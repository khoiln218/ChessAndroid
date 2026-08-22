package com.ttnt.chinesschess.chess;

import android.graphics.Point;

public class CBishop extends Piece {

    public static int[][] BishopTable = {
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
            int x = CurrMove.x + dx[i];
            int y = CurrMove.y + dy[i];
            if (x >= 0 && x <= 9 && y >= 3 && y <= 5) {
                byte val1 = board.cell[CurrMove.x][CurrMove.y];
                byte val2 = board.cell[x][y];
                if ((RED && ((val2 >= 8 && val2 <= 14) || val2 == 0) && x <= 2) || (!RED && (val2 > 14 || val2 == 0) && x >= 7)) {
                    offer(x, y, val1, val2);
                }
            }
        }
    }
}
