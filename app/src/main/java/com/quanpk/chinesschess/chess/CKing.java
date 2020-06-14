package com.quanpk.chinesschess.chess;

import android.graphics.Point;

import java.util.ArrayList;

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
                if (((RED && ((val2 >= 8 && val2 <= 14) || val2 == 0) && x <= 2) || (!RED && (val2 > 14 || val2 == 0) && x >= 7)) && (y >= 3 && y <= 5)) {
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
        if (CurrMove.y != King.y) {
            return false;
        }
        for (int i = Math.min(CurrMove.x, King.x) + 1; i < Math.max(CurrMove.x, King.x); i++) {
            if (board.cell[i][CurrMove.y] != 0) {
                return false;
            }
        }
        return true;
    }

    public static int getPositionValue(Point pos, boolean RED) {
        if (RED) {
            return KingTable[9 - pos.x][8 - pos.y];
        }
        return KingTable[pos.x][pos.y];
    }
}