package com.ttnt.chinesechess.chess;

public class CKnight extends Piece {

    public CKnight(Board board, Point currMove) {
        super(board, currMove);
    }

    CKnight(Board board) {
        super(board);
    }

    @Override
    void generate() {
        int[] dx = {1, 1, 2, 2, -1, -1, -2, -2};
        int[] dy = {2, -2, 1, -1, 2, -2, 1, -1};
        for (int i = 0; i < dx.length; i++) {
            int x = currMove.x + dx[i];
            int y = currMove.y + dy[i];
            if (Board.inside(x, y)) {
                byte piece = board.cell[currMove.x][currMove.y];
                byte occupant = board.cell[x][y];
                if (canLandOn(occupant) && checkProject(x, y)) {
                    offer(x, y, piece, occupant);
                }
            }
        }
    }

    /** Whether a horse standing here could reach {@code target}: its leg must be clear. */
    private boolean checkProject(int x, int y) {
        int dx = x - currMove.x;
        int dy = y - currMove.y;
        int absX = Math.abs(dx);
        int absY = Math.abs(dy);
        if ((absX == 1 && absY == 2) || (absX == 2 && absY == 1)) {
            if (absX == 1) {
                if (dy > 0) {
                    return board.cell[currMove.x][currMove.y + 1] == PieceCode.EMPTY;
                } else {
                    return board.cell[currMove.x][currMove.y - 1] == PieceCode.EMPTY;
                }
            } else {
                if (dx > 0) {
                    return board.cell[currMove.x + 1][currMove.y] == PieceCode.EMPTY;
                } else {
                    return board.cell[currMove.x - 1][currMove.y] == PieceCode.EMPTY;
                }
            }
        }
        return false;
    }
}
