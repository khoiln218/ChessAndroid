package com.ttnt.chinesechess.ai.engine;

import android.util.Log;

import com.ttnt.chinesechess.ai.AlphaBeta;
import com.ttnt.chinesechess.ai.GameSearch;
import com.ttnt.chinesechess.ai.Minimax;
import com.ttnt.chinesechess.ai.Negamax;
import com.ttnt.chinesechess.ai.optimize.EnhancedAlphaBeta;
import com.ttnt.chinesechess.ai.optimize.EnhancedGameSearch;
import com.ttnt.chinesechess.chess.Board;
import com.ttnt.chinesechess.chess.Move;

import java.util.function.Function;

/** Plays the machine's side with minimax, negamax, alpha-beta or the optimized search. */
public final class Engine {

    /** What one search found: the move, what it is worth, how deep it got and what it cost. */
    private record Result(Move move, int score, int depth, long nodes) {
    }

    private final String name;
    private final int depth;
    /** Whose turn it is -> what the search found. */
    private final Function<Boolean, Result> search;

    private Engine(String name, int depth, Function<Boolean, Result> search) {
        this.name = name;
        this.depth = depth;
        this.search = search;
    }

    public static Engine minimax(Board board, int depth) {
        return new Engine("Minimax", depth, red ->
                run(Minimax.problem(new ChessState(board, red), depth), Minimax::search, depth));
    }

    public static Engine negamax(Board board, int depth) {
        return new Engine("Negamax", depth, red ->
                run(Negamax.problem(new ChessState(board, red), depth), GameSearch::search, depth));
    }

    public static Engine alphaBeta(Board board, int depth) {
        return new Engine("Alpha-Beta", depth, red ->
                run(AlphaBeta.problem(new ChessState(board, red), depth), GameSearch::search, depth));
    }

    public static Engine optimized(Board board, int depth, long budgetMs) {
        return new Engine("Optimized", depth, red -> {
            EnhancedAlphaBeta problem = EnhancedAlphaBeta.problem(board, red, depth, budgetMs);
            GameSearch.Node<Move> root = EnhancedGameSearch.search(problem);
            return new Result(root.bestAction(), root.value, problem.reached(), problem.visited());
        });
    }

    /** {@code search(problem)}, the way the textbook calls it. */
    private static Result run(GameSearch.Problem<Move> problem,
            Function<GameSearch.Problem<Move>, GameSearch.Node<Move>> search, int depth) {
        GameSearch.Node<Move> root = search.apply(problem);
        return new Result(root.bestAction(), root.value, depth, problem.visited());
    }

    /** The move {@code side} should play, red if {@code true}; null if it has none. */
    public Move generateMove(boolean side) {
        long started = System.currentTimeMillis();
        Result result = search.apply(side);
        Log.d("AI", name + " depth " + result.depth() + "/" + depth
                + "  score " + result.score() + "  nodes " + result.nodes() + "  "
                + (System.currentTimeMillis() - started) + "ms");
        return result.move();
    }
}
