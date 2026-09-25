package com.ttnt.chinesechess.ai.optimize;

import com.ttnt.chinesechess.chess.Board;

/**
 * The optimized search's problem: {@link EnhancedGameSearch} set on a position, the way
 * {@code AlphaBeta.problem} sets up the problem for {@code GameSearch}.
 *
 * <pre>
 *   GameSearch.search(AlphaBeta.problem(state, depth))                                  // plain
 *   EnhancedGameSearch.search(EnhancedAlphaBeta.problem(board, side, depth, budgetMs)) // enhanced
 * </pre>
 */
public final class EnhancedAlphaBeta extends EnhancedGameSearch {

    /**
     * The transposition table, kept from one move to the next and from one game to the next.
     * There is one for the whole app, which is safe because only one search runs at a time: the
     * game hands them all to a single background thread.
     */
    private static final Memory MEMORY = new Memory();

    private EnhancedAlphaBeta(OptimizedChessState position, int depth, long budgetMs) {
        super(position, depth, budgetMs, MEMORY);
    }

    /**
     * The position on {@code board}, {@code redToMove} to move, to search at most
     * {@code depth} plies and for at most {@code budgetMs}.
     */
    public static EnhancedAlphaBeta problem(Board board, boolean redToMove, int depth,
                                            long budgetMs) {
        return new EnhancedAlphaBeta(new OptimizedChessState(board, redToMove), depth, budgetMs);
    }
}
