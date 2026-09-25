package com.ttnt.chinesechess.ai.engine;

import com.ttnt.chinesechess.ai.GameState;
import com.ttnt.chinesechess.chess.Board;
import com.ttnt.chinesechess.chess.Move;
import com.ttnt.chinesechess.chess.PieceCode;
import com.ttnt.chinesechess.chess.Rules;

import java.util.ArrayList;
import java.util.List;

/**
 * Xiangqi put in the terms the plain algorithms in {@code ai} understand: the board, the side to
 * move, and the five questions of {@link GameState}. Everything that is xiangqi - how pieces move,
 * when a general is in check, what a position is worth, that having no move at all loses - is
 * answered here, and none of it leaks into the algorithms.
 *
 * <p>It searches a copy of the board, so the game's own board is never touched.
 */
public final class ChessState implements GameState<Move> {

    /** The score of a lost position, far beyond anything {@link Evaluation} can produce. */
    public static final int MATE = GameState.WIN;

    private final Board board;
    private final Evaluation evaluation = new Evaluation();
    /** The side to move, red if {@code true}. */
    private boolean side;
    /** Moves played since the root, so a mate found sooner scores better than one found later. */
    private int ply;

    /**
     * {@link #isTerminal} costs a move-generation pass, and at a leaf the algorithm asks it and
     * then {@link #evaluate} asks it again. The answer is kept until the position changes.
     */
    private int version;
    private int terminalVersion = -1;
    private boolean terminal;

    public ChessState(Board position, boolean redToMove) {
        this.board = new Board(position);
        this.side = redToMove;
    }

    @Override
    public List<Move> moves() {
        ArrayList<Move> moves = new ArrayList<>(48);
        Rules.collect(board, side, false, true, moves);
        // Captures first, most valuable victim first: it changes nothing about the score a search
        // returns, only how much alpha-beta gets to cut. A stable sort, so the quiet moves, all
        // tied at zero, keep the order they were generated in.
        moves.sort(Evaluation.BY_CAPTURE);
        return moves;
    }

    @Override
    public void play(Move move) {
        board.cell[move.to.x][move.to.y] = move.piece;
        board.cell[move.from.x][move.from.y] = PieceCode.EMPTY;
        side = !side;
        ply++;
        version++;
    }

    @Override
    public void undo(Move move) {
        board.cell[move.to.x][move.to.y] = move.captured;
        board.cell[move.from.x][move.from.y] = move.piece;
        side = !side;
        ply--;
        version++;
    }

    /** No legal move is the end: mate if the general is attacked, and a loss in xiangqi if not. */
    @Override
    public boolean isTerminal() {
        if (terminalVersion != version) {
            terminal = !Rules.hasLegalMove(board, side);
            terminalVersion = version;
        }
        return terminal;
    }

    /**
     * The side to move has lost if the game is over here, and the sooner the worse: counting the
     * plies from the root is what makes the winner go for the shortest mate rather than any mate.
     */
    @Override
    public int evaluate() {
        if (isTerminal()) return -(MATE - ply);
        return evaluation.score(board, side);
    }
}
