package com.ttnt.chinesechess.chess;

public class CKing extends Piece {

    public CKing(Board board, Point currMove) {
        super(board, currMove);
    }

    CKing(Board board) {
        super(board);
    }

    @Override
    void generate() {
        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};
        for (int i = 0; i < dx.length; i++) {
            int x = currMove.x + dx[i];
            int y = currMove.y + dy[i];
            if (Board.inPalace(x, y, red)) {
                byte piece = board.cell[currMove.x][currMove.y];
                byte occupant = board.cell[x][y];
                if (canLandOn(occupant)) {
                    offer(x, y, piece, occupant);
                }
            }
        }
    }
}