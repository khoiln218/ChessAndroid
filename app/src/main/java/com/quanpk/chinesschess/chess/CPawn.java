package com.quanpk.chinesschess.chess;

import android.graphics.Point;

import java.util.ArrayList;

public class CPawn extends Piece {

    static int[][] PawnTable = {
            {11, 12, 13, 14, 14, 14, 13, 12, 11}, /* PAWN*/
            {20, 21, 21, 23, 22, 23, 21, 21, 20},
            {20, 21, 21, 23, 23, 23, 21, 21, 20},
            {20, 21, 21, 22, 22, 22, 21, 21, 20},
            {20, 20, 20, 20, 20, 20, 20, 20, 20},
            {10, 0, 13, 0, 10, 0, 13, 0, 10},
            {10, 0, 12, 0, 15, 0, 12, 0, 10},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0}
    };

    public CPawn(Board board, Point currMove) {
        super(board, currMove);
    }

    @Override
    public ArrayList<State> findAllPossibleMoves() {
        allPossibleMove = new ArrayList<State>();
        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};
        for (int i = 0; i < dx.length; i++) {
            int x = CurrMove.x + dx[i];
            int y = CurrMove.y + dy[i];
            if (x >= 0 && x <= 9 && y >= 0 && y <= 8) {
                byte val1 = board.cell[CurrMove.x][CurrMove.y];
                byte val2 = board.cell[x][y];
                if ((val2 == 0 || ((RED && val2 >= 8 && val2 <= 14) || (!RED && val2 > 14))) && (((!isOver(x, y) && i < 2) || isOver(x, y)) && ((RED && i != 1) || (!RED && i != 0)))) {
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

    boolean isOver(int x, int y) {
        if ((RED && x > 4) || (!RED && x < 5)) {
            return true;
        }
        return false;
    }

    @Override
    boolean checkProject(Point King) {
        if ((RED && King.x < CurrMove.x) || (!RED && (King.x > CurrMove.x))) {
            return false;
        }
        int dx = Math.abs(CurrMove.x - King.x);
        int dy = Math.abs(CurrMove.y - King.y);
        if ((dx == 1 && dy == 0) || (dx == 0 && dy == 1)) {
            return true;
        }
        return false;
    }

    public static int getPositionValue(Point pos, boolean RED) {
        if (RED) {
            return PawnTable[9 - pos.x][8 - pos.y];
        }
        return PawnTable[pos.x][pos.y];
    }
}
