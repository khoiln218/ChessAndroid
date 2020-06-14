package com.quanpk.chinesschess.chess;

import android.graphics.Point;

import java.util.ArrayList;

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

    @Override
    public ArrayList<State> findAllPossibleMoves() {
        allPossibleMove = new ArrayList<State>();
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
        return allPossibleMove;
    }

    boolean getMoveRook(int x, int y) {
        byte val1 = board.cell[CurrMove.x][CurrMove.y];
        byte val2 = board.cell[x][y];
        if (val2 == 0 || ((RED && val2 >= 8 && val2 <= 14) || (!RED && val2 > 14))) {
            doMove(x, y);
            ArrayList<Point> arr = board.findPieces(RED);
            if (!checkProject(board, arr)) {
                allPossibleMove.add(new State(CurrMove, new Point(x, y), val1, val2));
            }
            reMove(x, y, val2);
        }
        if (val2 != 0) {
            return true;
        }
        return false;
    }

    @Override
    boolean checkProject(Point King) {
        if (CurrMove.x == King.x || CurrMove.y == King.y) {
            int count = 0;
            if (CurrMove.x == King.x) {
                for (int i = Math.min(CurrMove.y, King.y) + 1; i < Math.max(CurrMove.y, King.y); i++) {
                    if (board.cell[CurrMove.x][i] != 0) {
                        count++;
                    }
                }
            } else {
                for (int i = Math.min(CurrMove.x, King.x) + 1; i < Math.max(CurrMove.x, King.x); i++) {
                    if (board.cell[i][CurrMove.y] != 0) {
                        count++;
                    }
                }
            }
            if (count == 0) {
                return true;
            }
        }
        return false;
    }

    public static int getPositionValue(Point pos, boolean RED) {
        if (RED) {
            return RookTable[9 - pos.x][8 - pos.y];
        }
        return RookTable[pos.x][pos.y];
    }
}
