package com.ttnt.chinesechess.chess;

import android.graphics.Point;

public class CPawn extends Piece {

    static final int[][] PAWN_TABLE = {
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

    CPawn(Board board) {
        super(board);
    }

    @Override
    void generate() {
        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};
        for (int i = 0; i < dx.length; i++) {
            int x = currMove.x + dx[i];
            int y = currMove.y + dy[i];
            if (x >= 0 && x <= 9 && y >= 0 && y <= 8) {
                byte piece = board.cell[currMove.x][currMove.y];
                byte occupant = board.cell[x][y];
                if ((occupant == 0 || ((red && occupant >= 8 && occupant <= 14) || (!red && occupant > 14))) && (((!isOver(x) && i < 2) || isOver(x)) && ((red && i != 1) || (!red && i != 0)))) {
                    offer(x, y, piece, occupant);
                }
            }
        }
    }

    boolean isOver(int x) {
        return (red && x > 4) || (!red && x < 5);
    }
}
