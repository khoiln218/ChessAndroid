package com.quanpk.chinesschess.chess;

import android.graphics.Point;

import java.util.ArrayList;

public class CKnight extends Piece {

    static int[][] KnightTable = {
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

    @Override
    public ArrayList<State> findAllPossibleMoves() {
        allPossibleMove = new ArrayList<State>();
        int[] dx = {1, 1, 2, 2, -1, -1, -2, -2};
        int[] dy = {2, -2, 1, -1, 2, -2, 1, -1};
        for (int i = 0; i < dx.length; i++) {
            int x = CurrMove.x + dx[i];
            int y = CurrMove.y + dy[i];
            if (x >= 0 && x <= 9 && y >= 0 && y <= 8) {
                byte val1 = board.cell[CurrMove.x][CurrMove.y];
                byte val2 = board.cell[x][y];
                if ((val2 == 0 || ((RED && val2 >= 8 && val2 <= 14) || (!RED && val2 > 14))) && checkProject(new Point(x, y))) {
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

    @Override
    boolean checkProject(Point King) {
        int dong = King.x - CurrMove.x;
        int cot = King.y - CurrMove.y;
        int d = Math.abs(dong);
        int c = Math.abs(cot);
        if ((d == 1 && c == 2) || (d == 2 && c == 1)) {
            if (d == 1) {
                if (cot > 0) {
                    if (board.cell[CurrMove.x][CurrMove.y + 1] == 0) {
                        return true;
                    }
                } else {
                    if (board.cell[CurrMove.x][CurrMove.y - 1] == 0) {
                        return true;
                    }
                }
            } else {
                if (dong > 0) {
                    if (board.cell[CurrMove.x + 1][CurrMove.y] == 0) {
                        return true;
                    }
                } else {
                    if (board.cell[CurrMove.x - 1][CurrMove.y] == 0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static int getPositionValue(Point pos, boolean RED) {
        if (RED) {
            return KnightTable[9 - pos.x][8 - pos.y];
        }
        return KnightTable[pos.x][pos.y];
    }
}
