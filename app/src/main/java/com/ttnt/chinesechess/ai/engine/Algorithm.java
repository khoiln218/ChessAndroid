package com.ttnt.chinesechess.ai.engine;

import com.ttnt.chinesechess.chess.Board;

/**
 * Which search plays the machine's side. The lobby's three levels arrive as 2, 3 and 4, and each
 * algorithm turns them into as many plies as it can search in a few seconds on a phone.
 */
public enum Algorithm {

    /**
     * The optimized search: four, six and eight plies - two a level rather than one, because one
     * side's reply is only half an exchange, and it takes the pair to see the exchange through.
     */
    OPTIMIZED,

    /**
     * Plain minimax: two, three and four plies, like negamax - the same tree, searched with a
     * MAX and a MIN function instead of one negated one.
     */
    MINIMAX,

    /**
     * Plain negamax: two, three and four plies. Four is already some three million positions
     * from the opening - one more would be forty times that.
     */
    NEGAMAX,

    /**
     * Plain alpha-beta: four, five and six plies. Pruning with captures ordered first reaches
     * six plies in fewer positions than negamax spends on four.
     */
    ALPHA_BETA;

    /** An engine playing this algorithm on {@code board} at {@code level}. */
    public Engine create(Board board, int level) {
        int lv = Math.max(1, level);
        return switch (this) {
            case OPTIMIZED -> Engine.optimized(board, lv * 2, budget(lv));
            case MINIMAX -> Engine.minimax(board, lv);
            case NEGAMAX -> Engine.negamax(board, lv);
            case ALPHA_BETA -> Engine.alphaBeta(board, lv + 2);
        };
    }

    /**
     * How long the optimized search may take at a level. An extra ply costs, measured on ART from
     * the opening position - the widest the board ever is - depth 6 in 0.08s, depth 7 in 0.5s,
     * depth 8 in 2.4s: about five or six times the previous ply each time. Past the opening the
     * same plies cost 0.05s, 0.09s and 0.2s, so the opening is the case the budget has to survive.
     * Those figures come from an emulator on desktop silicon; a real phone is some small multiple
     * slower.
     *
     * <p>Each level gets four times the time of the one below it, which is roughly what one more
     * ply costs. The budget is a ceiling rather than a target: at the depths the three levels ask
     * for, the search finishes well inside it. So what separates the levels is the depth, and the
     * clock only takes the decision back on a position, or a device, slow enough to need it -
     * which is what {@link #BUDGET_CAP} is sized for.
     */
    private static long budget(int level) {
        return Math.min(BUDGET_CAP, BUDGET_STEP << (2 * (level - 1)));
    }

    private static final long BUDGET_STEP = 375L;
    private static final long BUDGET_CAP = 60000L;
}
