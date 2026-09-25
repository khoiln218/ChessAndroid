package com.ttnt.chinesechess.ai.optimize;

import com.ttnt.chinesechess.ai.GameState;
import com.ttnt.chinesechess.ai.engine.Evaluation;
import com.ttnt.chinesechess.chess.Board;
import com.ttnt.chinesechess.chess.Move;
import com.ttnt.chinesechess.chess.Rules;

import java.util.List;

/**
 * The position {@link EnhancedAlphaBeta} is searching, as a {@link GameState}: an
 * {@link OptimizedBoard}, how far below the root it stands, and what the search asks of it on top
 * of the five questions - the key, repetition, pseudo-legal moves, captures, passing. The board
 * keeps the key, the piece count and the line of positions in step; this counts the plies and
 * scores what it sees.
 */
public final class OptimizedChessState implements GameState<Move> {

    private final OptimizedBoard board;
    private final Evaluation evaluation = new Evaluation();
    /** Moves played since the root, so a mate found sooner scores better than one found later. */
    private int ply;

    /** {@link #isTerminal} costs a move-generation pass; the answer is kept until a move. */
    private int version;
    private int terminalVersion = -1;
    private boolean terminal;

    /** The position on {@code position}, with {@code redToMove} to move, and the game behind it. */
    public OptimizedChessState(Board position, boolean redToMove) {
        board = new OptimizedBoard(position, redToMove);
    }

    // ===== GameState =====

    /** Every legal move of the side to move, in board order; the search orders them itself. */
    @Override
    public List<Move> moves() {
        return board.collectAt(ply, false, true);
    }

    @Override
    public void play(Move move) {
        ply++;
        version++;
        board.makeMove(move, ply);
    }

    @Override
    public void undo(Move move) {
        board.unmakeMove(move);
        ply--;
        version++;
    }

    /** No legal move is the end: mate if the general is attacked, and a loss in xiangqi if not. */
    @Override
    public boolean isTerminal() {
        if (terminalVersion != version) {
            terminal = !Rules.hasLegalMove(board, board.redToMove);
            terminalVersion = version;
        }
        return terminal;
    }

    /**
     * A lost position scores the worse the sooner it comes, so the winner goes for the shortest
     * mate.
     */
    @Override
    public int evaluate() {
        if (isTerminal()) return -(WIN - ply);
        return staticEval();
    }

    // ===== What the optimized search asks on top =====

    /** Moves played since the root. */
    public int ply() {
        return ply;
    }

    /** Zobrist key of the position and the side to move. */
    public long key() {
        return board.key;
    }

    /** {@link #evaluate}, without asking first whether the game is over here. */
    public int staticEval() {
        return evaluation.score(board, board.redToMove);
    }

    /** Legal captures of the side to move, for the quiescence search. */
    public List<Move> captures() {
        return board.collectAt(ply, true, true);
    }

    /**
     * Every move of the side to move, some of which may leave its own general attacked. Whether
     * one does is settled by {@link #lastMoveLegal}, on the moves that actually get played - at a
     * node that cuts after one or two of them, the rest never need the test, and that test was
     * the single most expensive thing in the search.
     */
    public List<Move> pseudoMoves() {
        return board.collectAt(ply, false, false);
    }

    /** Whether the move just played left the side that made it safe. */
    public boolean lastMoveLegal() {
        return Rules.kingSafe(board, !board.redToMove);
    }

    /** Whether the position here has stood before with the same side to move. */
    public boolean repeated() {
        return board.repeated(ply);
    }

    /**
     * Whether passing is a fair test here. A side in check has no turn to give away. A side down
     * to its general, guards and soldiers may be in zugzwang, where every move it has makes its
     * position worse and passing would be better than anything legal - the one case where the
     * null-move argument is false.
     */
    public boolean canPass() {
        return board.hasHeavy(board.redToMove) && Rules.kingSafe(board, board.redToMove);
    }

    /** Hands the turn over without moving. */
    public void pass() {
        ply++;
        version++;
        board.pass(ply);
    }

    public void unpass() {
        board.unpass();
        ply--;
        version++;
    }
}
