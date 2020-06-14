package com.quanpk.chinesschess.chess;

import android.graphics.Point;

import java.util.ArrayList;

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

    @Override
    public ArrayList<State> findAllPossibleMoves() {
        allPossibleMove = new ArrayList<State>();
        int[] dx = {1, 1, -1, -1};
        int[] dy = {1, -1, 1, -1};
        for (int i = 0; i < dx.length; i++) {
            int x = CurrMove.x + dx[i];
            int y = CurrMove.y + dy[i];
            if (x >= 0 && x <= 9 && y >= 3 && y <= 5) {
                byte val1 = board.cell[CurrMove.x][CurrMove.y];
                byte val2 = board.cell[x][y];
                if ((RED && ((val2 >= 8 && val2 <= 14) || val2 == 0) && x <= 2) || (!RED && (val2 > 14 || val2 == 0) && x >= 7)) {
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
        return false;
    }

    public static int getPositionValue(Point pos, boolean RED) {
        if (RED) {
            return BishopTable[9 - pos.x][8 - pos.y];
        }
        return BishopTable[pos.x][pos.y];
    }
}
