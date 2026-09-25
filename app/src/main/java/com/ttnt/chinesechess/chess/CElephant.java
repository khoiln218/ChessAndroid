package com.ttnt.chinesechess.chess;

public class CElephant extends Piece {

    public CElephant(Board board, Point currMove) {
        super(board, currMove);
    }

    CElephant(Board board) {
        super(board);
    }

    @Override
    void generate() {
        byte[] dx = {2, 2, -2, -2};
        byte[] dy = {2, -2, 2, -2};
        for (int i = 0; i < dx.length; i++) {
            int x = currMove.x + dx[i];
            int y = currMove.y + dy[i];
            if (Board.inside(x, y)) {
                byte piece = board.cell[currMove.x][currMove.y];
                byte occupant = board.cell[x][y];
                // An elephant never crosses the river.
                if (canLandOn(occupant) && Board.onOwnHalf(x, red) && legClear(x, y)) {
                    offer(x, y, piece, occupant);
                }
            }
        }
    }

    /** Whether the diagonal step is open: an elephant is blocked by a piece on its own eye. */
    boolean legClear(int x, int y) {
        int dx = x - currMove.x;
        int dy = y - currMove.y;
        if (Math.abs(dx) == 2 && Math.abs(dy) == 2) {
            if (dx > 0) {
                if (dy > 0) {
                    return board.cell[currMove.x + 1][currMove.y + 1] == PieceCode.EMPTY;
                } else {
                    return board.cell[currMove.x + 1][currMove.y - 1] == PieceCode.EMPTY;
                }
            } else {
                if (dy > 0) {
                    return board.cell[currMove.x - 1][currMove.y + 1] == PieceCode.EMPTY;
                } else {
                    return board.cell[currMove.x - 1][currMove.y - 1] == PieceCode.EMPTY;
                }
            }
        }
        return false;
    }
}
