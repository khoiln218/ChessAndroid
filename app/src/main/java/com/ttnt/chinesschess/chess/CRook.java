package com.ttnt.chinesschess.chess;

import android.graphics.Point;

public class CRook extends Piece {

    static int[][] RookTable = {
            {90, 90, 90, 90, 90, 90, 90, 90, 90}, /* ROOK */
            {90, 92, 91, 91, 90, 91, 91, 92, 90},
            {90, 91, 90, 90, 90, 90, 90, 91, 90},
            {90, 91, 90, 91, 90, 91, 90, 91, 90},
            {90, 93, 90, 91, 90, 91, 90, 93, 90},
            {90, 94, 90, 94, 90, 94, 90, 94, 90},
            {90, 91, 90, 91, 90, 91, 90, 91, 90},
            {90, 92, 90, 91, 90, 91, 90, 92, 90},
            {91, 92, 90, 93, 90, 93, 90, 92, 91},
            {89, 92, 90, 90, 90, 90, 90, 92, 89}
    };

    public CRook(Board board, Point currMove) {
        super(board, currMove);
    }

    CRook(Board board) {
        super(board);
    }

    @Override
    void generate() {
        int x = CurrMove.x;
        int y = CurrMove.y;
        for (int i = y + 1; i <= 8; i++) {
            if (getMoveRook(x, i)) {
                break;
            }
        }
        for (int i = y - 1; i >= 0; i--) {
            if (getMoveRook(x, i)) {
                break;
            }
        }
        for (int i = x + 1; i <= 9; i++) {
            if (getMoveRook(i, y)) {
                break;
            }
        }
        for (int i = x - 1; i >= 0; i--) {
            if (getMoveRook(i, y)) {
                break;
            }
        }
    }

    boolean getMoveRook(int x, int y) {
        byte val1 = board.cell[CurrMove.x][CurrMove.y];
        byte val2 = board.cell[x][y];
        if (val2 == 0 || ((RED && val2 >= 8 && val2 <= 14) || (!RED && val2 > 14))) {
            offer(x, y, val1, val2);
        }
        return val2 != 0;
    }
}
