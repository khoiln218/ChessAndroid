package com.ttnt.chinesechess.chess;

public class CRook extends Piece {

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
        for (int i = y + 1; i < Board.COL; i++) {
            if (rayStopsAt(x, i)) {
                break;
            }
        }
        for (int i = y - 1; i >= 0; i--) {
            if (rayStopsAt(x, i)) {
                break;
            }
        }
        for (int i = x + 1; i < Board.ROW; i++) {
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
        if (canLandOn(occupant)) {
            offer(x, y, piece, occupant);
        }
        return occupant != PieceCode.EMPTY;
    }
}
