package com.ttnt.chinesschess.chess;

import android.graphics.Point;

public class CKing extends Piece {

    static int[][] KingTable = {
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
            int x = CurrMove.x + dx[i];
            int y = CurrMove.y + dy[i];
            if (x >= 0 && x <= 9 && y >= 0 && y <= 8) {
                byte val1 = board.cell[CurrMove.x][CurrMove.y];
                byte val2 = board.cell[x][y];
                if (((RED && ((val2 >= 8 && val2 <= 14) || val2 == 0) && x <= 2) || (!RED && (val2 > 14 || val2 == 0) && x >= 7)) && (y >= 3 && y <= 5)) {
                    offer(x, y, val1, val2);
                }
            }
        }
    }
}