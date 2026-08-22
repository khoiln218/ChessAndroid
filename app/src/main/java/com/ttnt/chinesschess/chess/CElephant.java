package com.ttnt.chinesschess.chess;

import android.graphics.Point;

public class CElephant extends Piece {

    static int[][] ElephantTable = {
            {0, 0, 0, 0, 0, 0, 0, 0, 0}, /* ELEPHAN */
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
            int x = CurrMove.x + dx[i];
            int y = CurrMove.y + dy[i];
            if (x >= 0 && x <= 9 && y >= 0 && y <= 8) {
                byte val1 = board.cell[CurrMove.x][CurrMove.y];
                byte val2 = board.cell[x][y];
                if (((RED && ((val2 >= 8 && val2 <= 14) || val2 == 0) && x <= 4) || (!RED && (val2 > 14 || val2 == 0) && x >= 5)) && legClear(x, y)) {
                    offer(x, y, val1, val2);
                }
            }
        }
    }

    /** Whether the diagonal step is open: an elephant is blocked by a piece on its own eye. */
    boolean legClear(int x, int y) {
        int dong = x - CurrMove.x;
        int cot = y - CurrMove.y;
        if (Math.abs(dong) == 2 && Math.abs(cot) == 2) {
            if (dong > 0) {
                if (cot > 0) {
                    return board.cell[CurrMove.x + 1][CurrMove.y + 1] == 0;
                } else {
                    return board.cell[CurrMove.x + 1][CurrMove.y - 1] == 0;
                }
            } else {
                if (cot > 0) {
                    return board.cell[CurrMove.x - 1][CurrMove.y + 1] == 0;
                } else {
                    return board.cell[CurrMove.x - 1][CurrMove.y - 1] == 0;
                }
            }
        }
        return false;
    }
}
