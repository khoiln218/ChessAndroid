package com.ttnt.chinesechess.chess;

import android.graphics.Point;

import java.util.ArrayList;

/**
 * One kind of piece, standing on one square, able to list where it may go. The board makes one
 * of these per kind and walks it around rather than making a fresh one per square: at the depths
 * the search reaches, a piece object per square per position is millions of objects a second, and
 * on a phone that cost lands on the collector rather than on the search.
 *
 * <p>Every kind funnels its moves through {@link #offer}, which is where the two things the
 * search wants to vary live - whether quiet moves are wanted at all, and whether a move is tested
 * for leaving one's own general in check before it is kept.
 */
public abstract class Piece {

    final Point currMove;
    Board board;
    public boolean red;
    ArrayList<State> allPossibleMove;

    /** Skip quiet moves. The quiescence search resolves captures and wants nothing else. */
    boolean capturesOnly;
    /**
     * Whether a move is only kept once it is shown to leave one's own general safe. The search
     * turns this off and repeats the test on the moves it actually plays: at a node that cuts
     * after one or two moves, the rest are never played and never need to be tested at all.
     */
    boolean legalOnly = true;

    public Piece(Board board, Point currMove) {
        this.board = board;
        this.currMove = new Point(currMove);
        this.red = board.cell[currMove.x][currMove.y] > 14;
        allPossibleMove = null;
    }

    /** An instance with no square yet, to be pointed at one by {@link #at}. */
    Piece(Board board) {
        this.board = board;
        this.currMove = new Point();
    }

    /** Re-points this piece at another square, so one instance serves the whole board. */
    void at(int x, int y) {
        currMove.x = x;
        currMove.y = y;
        red = board.cell[x][y] > 14;
    }

    /** Appends this piece's moves to {@link #allPossibleMove}, under the flags as they stand. */
    abstract void generate();

    public ArrayList<State> findAllPossibleMoves() {
        allPossibleMove = new ArrayList<>();
        capturesOnly = false;
        legalOnly = true;
        generate();
        return allPossibleMove;
    }

    /**
     * Keeps one move the piece's own rules have already allowed. {@code captured} is what stands
     * on the target square, read before the move is tried, so the state built here does not
     * depend on the board being left in any particular way.
     */
    void offer(int x, int y, byte piece, byte captured) {
        if (capturesOnly && captured == 0) return;
        if (legalOnly) {
            doMove(x, y);
            boolean safe = board.kingSafe(red);
            reMove(x, y, captured);
            if (!safe) return;
        }
        allPossibleMove.add(new State(currMove, x, y, piece, captured));
    }

    public boolean checkMove(int x, int y) {
        try {
            allPossibleMove = findAllPossibleMoves();
            int n = allPossibleMove.size();
            for (int i = 0; i < n; i++) {
                Point pos = allPossibleMove.get(i).to;
                if (pos.x == x && pos.y == y) {
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR checkMove, Piece: " + e);
        }
        return false;
    }


    protected void doMove(int x, int y) {
        board.cell[x][y] = board.cell[currMove.x][currMove.y];
        board.cell[currMove.x][currMove.y] = 0;
    }

    protected void reMove(int x, int y, byte value) {
        board.cell[currMove.x][currMove.y] = board.cell[x][y];
        board.cell[x][y] = value;
    }
}
