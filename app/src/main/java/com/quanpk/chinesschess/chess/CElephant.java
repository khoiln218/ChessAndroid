package com.quanpk.chinesschess.chess;

import android.graphics.Point;

import java.util.ArrayList;

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

    @Override
    public ArrayList<State> findAllPossibleMoves() {
        allPossibleMove = new ArrayList<State>();
        byte[] dx = {2, 2, -2, -2};
        byte[] dy = {2, -2, 2, -2};
        for (int i = 0; i < dx.length; i++) {
            int x = CurrMove.x + dx[i];
            int y = CurrMove.y + dy[i];
            if (x >= 0 && x <= 9 && y >= 0 && y <= 8) {
                byte val1 = board.cell[CurrMove.x][CurrMove.y];
                byte val2 = board.cell[x][y];
                if (((RED && ((val2 >= 8 && val2 <= 14) || val2 == 0) && x <= 4) || (!RED && (val2 > 14 || val2 == 0) && x >= 5)) && isCheck(new Point(x, y))) {
                    doMove(x, y);
                    ArrayList<Point> arr = board.findPieces(RED);
                    if (!checkProject(board, arr)) {
                        allPossibleMove.add(new State(CurrMove, new Point(x, y), val1, val2));
                    }
                    reMove(x, y, val2);
                }
            }
        }
        return allPossibleMove;
    }

    boolean isCheck(Point pos) {
        int dong = pos.x - CurrMove.x;
        int cot = pos.y - CurrMove.y;
        if (Math.abs(dong) == 2 && Math.abs(cot) == 2) {
            if (dong > 0) {
                if (cot > 0) {
                    if (board.cell[CurrMove.x + 1][CurrMove.y + 1] == 0) {
                        return true;
                    }
                } else {
                    if (board.cell[CurrMove.x + 1][CurrMove.y - 1] == 0) {
                        return true;
                    }
                }
            } else {
                if (cot > 0) {
                    if (board.cell[CurrMove.x - 1][CurrMove.y + 1] == 0) {
                        return true;
                    }
                } else {
                    if (board.cell[CurrMove.x - 1][CurrMove.y - 1] == 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    boolean checkProject(Point King) {
        return false;
    }

    public static int getPositionValue(Point pos, boolean RED) {
        if (RED) {
            return ElephantTable[9 - pos.x][8 - pos.y];
        }
        return ElephantTable[pos.x][pos.y];
    }
}
