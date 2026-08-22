package com.ttnt.chinesschess.chess;

import android.graphics.Point;

public class CRook extends Piece {

    static final int[][] ROOK_TABLE = {
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
        int x = currMove.x;
        int y = currMove.y;
        for (int i = y + 1; i <= 8; i++) {
            if (rayStopsAt(x, i)) {
                break;
            }
        }
        for (int i = y - 1; i >= 0; i--) {
            if (rayStopsAt(x, i)) {
                break;
            }
        }
        for (int i = x + 1; i <= 9; i++) {
            if (rayStopsAt(i, y)) {
                break;
            }
        }
        for (int i = x - 1; i >= 0; i--) {
            if (rayStopsAt(i, y)) {
                break;
            }
        }
    }

    /**
     * One square along a ray out from the chariot. An empty square is somewhere it may move to
     * and an enemy piece is something it may take, and the ray ends at the first occupied square
     * either way: unlike the cannon beside it, a chariot passes over nothing.
     *
     * @return whether the ray ends here
     */
    private boolean rayStopsAt(int x, int y) {
        byte piece = board.cell[currMove.x][currMove.y];
        byte occupant = board.cell[x][y];
        if (occupant == 0 || ((red && occupant >= 8 && occupant <= 14) || (!red && occupant > 14))) {
            offer(x, y, piece, occupant);
        }
        return occupant != 0;
    }
}
