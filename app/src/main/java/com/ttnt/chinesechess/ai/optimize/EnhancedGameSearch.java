package com.ttnt.chinesechess.ai.optimize;

import com.ttnt.chinesechess.ai.AlphaBeta;
import com.ttnt.chinesechess.ai.GameSearch;
import com.ttnt.chinesechess.ai.GameState;
import com.ttnt.chinesechess.ai.engine.Evaluation;
import com.ttnt.chinesechess.chess.Board;
import com.ttnt.chinesechess.chess.Move;
import com.ttnt.chinesechess.chess.PieceCode;

import java.util.ArrayList;
import java.util.List;

/**
 * The optimized counterpart of {@link GameSearch}: a {@link GameSearch.Problem} with alpha-beta's
 * cutoff ({@link AlphaBeta#CUTOFF}), searched by the same negamax loop, with what makes it
 * affordable at depth added at each step of the loop:
 * <ul>
 *   <li>iterative deepening under a clock, so a move always comes back in reasonable time
 *       ({@link #search});</li>
 *   <li>before a node - a transposition table, repetition scored as a draw, and null-move
 *       pruning, which runs the same loop on the position with the turn handed over
 *       ({@link #known});</li>
 *   <li>its moves - the table's move first, then captures most valuable victim first, then
 *       killer and history moves; generated without the legality test, which only the moves
 *       actually played then pay for ({@link #actions}, {@link #legal});</li>
 *   <li>at a leaf - a quiescence search rather than the bare evaluation ({@link #leafValue});</li>
 *   <li>on the way back up - killer and history moves, and the table ({@link #cutoffBy},
 *       {@link #searched}).</li>
 * </ul>
 * {@link EnhancedAlphaBeta#problem} is the problem to hand it, the way {@link AlphaBeta#problem}
 * is for {@link GameSearch}. One instance searches one position; what outlives it - the
 * transposition table - is {@link Memory}, which {@link EnhancedAlphaBeta} keeps.
 */
public abstract class EnhancedGameSearch extends GameSearch.Problem<Move> {

    private final static int WIN = GameState.WIN;
    /** Scores this far from {@link #WIN} are mate scores, and are stored relative to the root. */
    private final static int WIN_BOUND = WIN - 1000;
    /** Null move: the least depth left that makes it worth trying, and how much it saves. */
    private final static int NULL_MOVE_MIN_DEPTH = 3;
    private final static int NULL_MOVE_DEEP = 6;
    private final static int NULL_MOVE_REDUCTION = 2;
    private final static int NULL_MOVE_DEEP_REDUCTION = 3;
    /** How many positions go by between looks at the clock, less one - a power of two less one. */
    private final static int CLOCK_CHECK_MASK = 255;
    /** Start another ply only while under this fraction of the budget has gone: 1/8. */
    private final static int DEEPEN_FRACTION = 8;
    /** Move ordering, best first: the table's move, captures, killers, then history. */
    private final static int HASH_MOVE_SCORE = 1 << 26;
    private final static int CAPTURE_SCORE = 1 << 22;
    private final static int KILLER_SCORE = 1 << 21;
    private final static int HISTORY_CAP = 1 << 20;
    /** How much more a capture's victim counts than its attacker in the ordering. */
    private final static int VICTIM_WEIGHT = 16;
    /** A move is packed as its two squares, {@code from << SQUARE_BITS | to}. */
    private final static int SQUARE_BITS = 8;
    private final static int SQUARE_MASK = (1 << SQUARE_BITS) - 1;
    /** How many plies of captures the quiescence search may chase past the nominal depth. */
    private final static int QUIET_PLIES = 4;
    /** Slack allowed on top of a capture before writing it off, on the scale of the evaluation. */
    private final static int DELTA = 25;
    /** Deepest ply the killer table covers; the search never gets near it. */
    private final static int MAX_PLY = 64;
    /** Transposition table, a power of two so the index is a mask of the key. */
    private final static int TT_SIZE = 1 << 17;
    private final static byte TT_EXACT = 0;
    private final static byte TT_LOWER = 1;
    private final static byte TT_UPPER = 2;

    /** What outlives one search: the transposition table, kept from one move to the next. */
    static final class Memory {
        final long[] ttKey = new long[TT_SIZE];
        final int[] ttScore = new int[TT_SIZE];
        final int[] ttMove = new int[TT_SIZE];
        final byte[] ttDepth = new byte[TT_SIZE];
        final byte[] ttFlag = new byte[TT_SIZE];
    }

    private final OptimizedChessState position;
    /** The most the search may take - a ceiling rather than a target. */
    private final long budgetMs;
    private final Memory memory;

    /**
     * Quiet moves that caused a cutoff. A move that refuted one line usually refutes its
     * siblings, so trying them early buys cutoffs that plain capture ordering cannot: without
     * this every quiet move is tied and they get searched in whatever order they were generated.
     */
    private final int[][] killers = new int[MAX_PLY][2];
    /** How often a quiet move from one square to another has caused a cutoff, anywhere. */
    private final int[][] history = new int[Board.SQUARES][Board.SQUARES];
    /** The table's move for the node being searched at each ply, found by {@link #known}. */
    private final int[] wanted = new int[MAX_PLY + 1];
    /**
     * Plies taken off the line down to each ply by null moves above it. The loop counts depth
     * from the root; a null move searches what is below it that much shallower.
     */
    private final int[] reduced = new int[MAX_PLY + 2];
    /** The reduction the null move about to be searched carries down to its child. */
    private int pendingReduction;

    /** The depth of the iteration under way; {@link #depth} is the deepest it may go. */
    private int limit;
    private List<Move> rootMoves;
    private long nodes;
    private long deadline;
    private boolean aborted;
    private int reached;

    protected EnhancedGameSearch(OptimizedChessState position, int depth, long budgetMs,
                                 Memory memory) {
        super(position, Math.max(1, depth), AlphaBeta.CUTOFF);
        this.position = position;
        this.budgetMs = budgetMs;
        this.memory = memory;
    }

    /** Positions visited by the search, quiescence included. */
    @Override
    public long visited() {
        return nodes;
    }

    /** The deepest iteration the search finished. */
    public int reached() {
        return reached;
    }

    /** Searches {@code problem}: the counterpart of {@link GameSearch#search}. */
    public static GameSearch.Node<Move> search(EnhancedGameSearch problem) {
        return problem.deepen();
    }

    // ===== Iterative deepening =====

    /**
     * Deepens one ply at a time up to {@link #depth} and keeps the answer from the last
     * iteration that finished. Each pass also re-orders the root so the previous best is tried
     * first, which is where most of the pruning at the next depth comes from - so the repeated
     * shallow searches more than pay for themselves.
     *
     * @return the root, with the value of the position and the best move below it
     */
    private GameSearch.Node<Move> deepen() {
        deadline = System.currentTimeMillis() + budgetMs;
        long started = System.currentTimeMillis();

        GameSearch.Node<Move> result = new GameSearch.Node<>(null, 0);
        rootMoves = new ArrayList<>(position.moves());
        if (rootMoves.isEmpty()) {
            result.value = -WIN;
            return result;
        }
        rootMoves.sort(Evaluation.BY_CAPTURE);
        // Something legal to play even if the very first iteration does not finish.
        result.best = new GameSearch.Node<>(rootMoves.get(0), 1);

        for (int d = 1; d <= depth; d++) {
            if (d > 1 && !worthDeepening(started)) break;
            aborted = false;
            limit = d;
            GameSearch.Node<Move> root = new GameSearch.Node<>(null, 0);
            negamax(root, -GameSearch.INF, GameSearch.INF);
            if (aborted) break;
            result = root;
            reached = d;
            Move found = root.bestAction();
            rootMoves.remove(found);
            rootMoves.add(0, found);
        }
        return result;
    }

    /**
     * Whether there is time for another ply. The next one costs several times everything spent
     * so far, so this only lets one start while under an eighth of the budget has gone - a
     * deliberate margin, because a ply begun and abandoned spends the rest of the clock on a
     * result the root throws away, and finishing one ply lower is the better trade.
     */
    private boolean worthDeepening(long started) {
        return (System.currentTimeMillis() - started) * DEEPEN_FRACTION < budgetMs;
    }

    // ===== The alpha-beta loop =====

    /**
     * negamax(n) = max over the children c of n of -negamax(c), cut once alpha reaches beta. A
     * child sees the window from the other side: [-beta, -alpha]. The loop of
     * {@link GameSearch}, with each step below where an optimization steps in.
     */
    private int negamax(GameSearch.Node<Move> node, int alpha, int beta) {
        Integer known = known(node, alpha, beta);
        if (known != null) {
            node.value = known;
            return known;
        }
        if (isLeaf(node)) {
            node.value = leafValue(node, alpha, beta);
            return node.value;
        }

        OptimizedChessState state = position;
        int alpha0 = alpha;
        int best = -GameSearch.INF;
        for (Move a : actions(node)) {
            state.play(a);
            if (!legal()) {
                state.undo(a);
                continue;
            }
            GameSearch.Node<Move> child = new GameSearch.Node<>(a, node.depth + 1);
            int value = -negamax(child, -beta, -alpha);
            state.undo(a);

            if (value > best) {                 // equal values keep the move met first
                best = value;
                node.best = child;
            }
            if (best > alpha) alpha = best;
            if (cutoff.cut(alpha, beta)) {
                cutoffBy(node, a);
                break;
            }
        }
        node.value = best;
        searched(node, alpha0, beta);
        return best;
    }

    /** Plies still to search below {@code node}, null-move reductions taken off. */
    private int remaining(GameSearch.Node<Move> node) {
        int ply = node.depth;
        return limit - ply - (ply <= MAX_PLY + 1 ? reduced[ply] : 0);
    }

    /** Out of depth, or the game is over. */
    private boolean isLeaf(GameSearch.Node<Move> node) {
        return remaining(node) <= 0 || position.isTerminal();
    }

    /**
     * Out of time, a repetition, or a position the table already knows the answer to at least
     * this deep - the same position is reached by many different orders of the same moves, and
     * one of them may have been searched already. Null if the node has to be searched.
     */
    private Integer known(GameSearch.Node<Move> node, int alpha, int beta) {
        nodes++;
        int ply = node.depth;
        if (ply <= MAX_PLY + 1) {
            reduced[ply] = ply == 0 ? 0 : reduced[ply - 1] + pendingReduction;
        }
        pendingReduction = 0;
        if (outOfTime()) return 0;
        if (ply == 0) return null;
        if (position.repeated()) return 0;
        int remaining = remaining(node);
        if (remaining <= 0) return null;
        Integer stored = probe(ply, remaining, alpha, beta);
        if (stored != null) return stored;

        // Null move: hand the turn straight back and see whether the position is still good
        // enough to cut. If doing nothing already beats beta, doing something will too, and the
        // whole subtree can go unsearched - which is where most of the saving at the deeper
        // levels comes from. Not twice in a row (a node reached by a pass has no action), and
        // only on a node deep enough to have a real subtree under it.
        if (remaining >= NULL_MOVE_MIN_DEPTH && node.action != null && position.canPass()) {
            int reduction = remaining > NULL_MOVE_DEEP
                    ? NULL_MOVE_DEEP_REDUCTION : NULL_MOVE_REDUCTION;
            position.pass();
            pendingReduction = reduction;
            int value = -negamax(new GameSearch.Node<>(null, ply + 1), -beta, -beta + 1);
            position.unpass();
            // A mate found beyond a move nobody may play is not a mate. Cutting on the bound is
            // sound - claiming the mate score is not.
            if (value >= beta && !aborted) return value >= WIN_BOUND ? beta : value;
        }
        return null;
    }

    /** What the table knows about the position here, if it is enough to settle it. */
    private Integer probe(int ply, int remaining, int alpha, int beta) {
        Memory m = memory;
        long key = position.key();
        int slot = (int) (key & (TT_SIZE - 1));
        if (ply <= MAX_PLY) wanted[ply] = 0;
        if (m.ttKey[slot] != key) return null;
        if (ply <= MAX_PLY) wanted[ply] = m.ttMove[slot];
        if (m.ttDepth[slot] < remaining) return null;
        int stored = fromTT(m.ttScore[slot], ply);
        byte flag = m.ttFlag[slot];
        if (flag == TT_EXACT
                || (flag == TT_LOWER && stored >= beta)
                || (flag == TT_UPPER && stored <= alpha)) {
            return stored;
        }
        return null;
    }

    /** A lost position is scored as such; otherwise the captures still hanging are played out. */
    private int leafValue(GameSearch.Node<Move> node, int alpha, int beta) {
        if (remaining(node) > 0) return position.evaluate();   // the game is over here
        return quiesce(alpha, beta);
    }

    /**
     * The previous iteration's best first at the root; elsewhere the table's move, then winning
     * captures, then the quiet moves that have refuted something before.
     */
    private List<Move> actions(GameSearch.Node<Move> node) {
        if (node.depth == 0) return rootMoves;
        List<Move> moves = position.pseudoMoves();
        int ply = node.depth;
        int want = ply <= MAX_PLY ? wanted[ply] : 0;
        int n = moves.size();
        int[] score = new int[n];
        for (int i = 0; i < n; i++) score[i] = moveScore(moves.get(i), want, ply);
        // Insertion sort, stable: equal scores keep the order the moves were generated in.
        for (int i = 1; i < n; i++) {
            Move m = moves.get(i);
            int s = score[i];
            int j = i - 1;
            while (j >= 0 && score[j] < s) {
                moves.set(j + 1, moves.get(j));
                score[j + 1] = score[j];
                j--;
            }
            moves.set(j + 1, m);
            score[j + 1] = s;
        }
        return moves;
    }

    /** The root's moves are legal already; the rest are only tested once played. */
    private boolean legal() {
        return position.ply() == 1 || position.lastMoveLegal();
    }

    /** Remembers a quiet move that caused a cutoff, so its siblings are tried after it. */
    private void cutoffBy(GameSearch.Node<Move> node, Move move) {
        if (move.captured != PieceCode.EMPTY) return;
        int ply = node.depth;
        int c = code(move);
        if (ply < MAX_PLY && killers[ply][0] != c) {
            killers[ply][1] = killers[ply][0];
            killers[ply][0] = c;
        }
        int remaining = remaining(node);
        int h = history[c >> SQUARE_BITS][c & SQUARE_MASK] + remaining * remaining;
        history[c >> SQUARE_BITS][c & SQUARE_MASK] = Math.min(h, HISTORY_CAP);
    }

    /** Stores what the node came to, unless the clock ran out part way through it. */
    private void searched(GameSearch.Node<Move> node, int alpha0, int beta) {
        if (aborted || node.depth == 0) return;
        Memory m = memory;
        int remaining = remaining(node);
        int best = node.value;
        long key = position.key();
        int slot = (int) (key & (TT_SIZE - 1));
        if (m.ttKey[slot] == key && m.ttDepth[slot] > remaining) return;
        m.ttKey[slot] = key;
        m.ttScore[slot] = toTT(best, node.depth);
        m.ttDepth[slot] = (byte) remaining;
        m.ttFlag[slot] = best <= alpha0 ? TT_UPPER : (best >= beta ? TT_LOWER : TT_EXACT);
        m.ttMove[slot] = node.best == null ? 0 : code(node.best.action);
    }

    // ===== Quiescence search =====

    /**
     * Plays out the captures still hanging at a leaf before scoring it. Without this the search
     * stops mid-exchange and reads a piece it is about to lose back as a piece won - it would
     * grab a defended rook on the last ply and never see the recapture.
     */
    private int quiesce(int alpha, int beta) {
        nodes++;
        if (outOfTime()) return 0;
        OptimizedChessState state = position;
        // Standing pat: the side to move is not obliged to capture, so its score is a floor.
        int stand = state.staticEval();
        if (stand >= beta) return stand;
        if (stand > alpha) alpha = stand;
        if (state.ply() >= depth + QUIET_PLIES) return stand;
        if (state.isTerminal()) return -(WIN - state.ply());

        List<Move> moves = state.captures();
        moves.sort(Evaluation.BY_CAPTURE);
        int best = stand;
        for (Move m : moves) {
            // Even winning this piece outright would not reach alpha, and neither will anything
            // behind it, since the captures are ordered by what they win.
            if (stand + Evaluation.evalValue(m.captured) + DELTA < alpha) break;
            state.play(m);
            int value = -quiesce(-beta, -alpha);
            state.undo(m);

            if (value > best) best = value;
            if (best > alpha) alpha = best;
            if (alpha >= beta) break;
        }
        return best;
    }

    // ===== Helpers =====

    /**
     * True once the budget is spent. The score returned from here on is meaningless, but the root
     * throws the whole unfinished iteration away, so it never reaches the move that gets played.
     */
    private boolean outOfTime() {
        if (aborted) return true;
        if ((nodes & CLOCK_CHECK_MASK) == 0 && System.currentTimeMillis() > deadline) {
            aborted = true;
        }
        return aborted;
    }

    /** The two squares, each 0 to 89, packed into one number. */
    private static int code(Move move) {
        return ((move.from.x * Board.COL + move.from.y) << SQUARE_BITS)
                | (move.to.x * Board.COL + move.to.y);
    }

    private int moveScore(Move move, int wanted, int ply) {
        int c = code(move);
        if (c == wanted) return HASH_MOVE_SCORE;
        if (move.captured != PieceCode.EMPTY) {
            return CAPTURE_SCORE + Evaluation.pieceValue(move.captured) * VICTIM_WEIGHT
                    - Evaluation.pieceValue(move.piece);
        }
        if (ply < MAX_PLY) {
            if (c == killers[ply][0]) return KILLER_SCORE + 1;
            if (c == killers[ply][1]) return KILLER_SCORE;
        }
        return history[c >> SQUARE_BITS][c & SQUARE_MASK];
    }

    /** Mate scores are stored counted from the position, not from the root that found them. */
    private static int toTT(int score, int ply) {
        if (score >= WIN_BOUND) return score + ply;
        if (score <= -WIN_BOUND) return score - ply;
        return score;
    }

    private static int fromTT(int score, int ply) {
        if (score >= WIN_BOUND) return score - ply;
        if (score <= -WIN_BOUND) return score + ply;
        return score;
    }
}
