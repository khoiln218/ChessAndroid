package com.ttnt.chinesechess.chess;

public class CBishop extends Piece {

    public CBishop(Board board, Point currMove) {
        super(board, currMove);
    }

    CBishop(Board board) {
        super(board);
    }

    @Override
    void generate() {
        int[] dx = {1, 1, -1, -1};
        int[] dy = {1, -1, 1, -1};
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
