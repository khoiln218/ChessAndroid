package com.ttnt.chinesechess.chess;

public class CPawn extends Piece {

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
            if (Board.inside(x, y)) {
                byte piece = board.cell[currMove.x][currMove.y];
                byte occupant = board.cell[x][y];
                // Red advances down the board, Black up; neither ever steps back, and a soldier
                // only steps sideways once it has crossed the river.
                boolean backward = dx[i] == (red ? -1 : 1);
                boolean sideways = dx[i] == 0;
                if (canLandOn(occupant) && !backward && (!sideways || isOver(x))) {
                    offer(x, y, piece, occupant);
                }
            }
        }
    }

    boolean isOver(int x) {
        return !Board.onOwnHalf(x, red);
    }
}
