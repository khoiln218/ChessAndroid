package com.ttnt.chinesechess.chess;

import android.graphics.Point;
import android.util.Log;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * Negamax with alpha-beta pruning: captures ordered first, a quiescence search at the leaves,
 * and iterative deepening under a clock so a move always comes back in reasonable time.
 *
 * <p>Scores are always from the point of view of the side to move at that node, which is what
 * lets one routine serve both sides: the child's score is negated on the way back up. A side with
 * no legal move has lost - in xiangqi stalemate is a loss too - and is scored {@code -(MATE-ply)},
 * measured in plies from the root so that a mate found sooner beats the same mate found later.
 */
public class AI {

    /** Beyond any score {@link #eval} can produce; the material on a full board sums to ~570. */
    private final static int INF = 1_000_000;
    private final static int MATE = 900_000;
    /** How many plies of captures the quiescence search may chase past the nominal depth. */
    private final static int QUIET_PLIES = 4;
    /** Slack allowed on top of a capture before writing it off, on the scale {@link #eval} uses. */
    private final static int DELTA = 25;
    /**
     * How much an extra ply costs, measured on ART from the opening position - the widest the
     * board ever is: depth 6 finishes in 0.08s, depth 7 in 0.5s, depth 8 in 2.4s. That is about
     * five or six times the previous ply each time, and since the last ply dominates everything
     * before it, about the same multiple of the whole search so far. Past the opening the same
     * plies cost 0.05s, 0.09s and 0.2s, so the opening is the case the budget has to survive.
     * Those figures come from an emulator on desktop silicon; a real phone is some small
     * multiple slower.
     *
     * <p>Each level gets four times the time of the one below it, which is roughly what one
     * more ply costs. The budget is a ceiling rather than a target, though: at the depths the
     * three levels ask for, the search finishes well inside it and stops at {@link #maxDepth}.
     * So what separates the levels is the depth, and the clock only takes the decision back on
     * a position, or a device, slow enough to need it - which is what {@link #BUDGET_CAP} is
     * sized for.
     */
    private final static long BUDGET_STEP = 375L;
    private final static long BUDGET_CAP = 60000L;

    /** Scores this far from {@link #MATE} are mate scores, and are stored relative to the root. */
    private final static int MATE_BOUND = MATE - 1000;
    /** Deepest ply the killer table covers; the search never gets near it. */
    private final static int MAX_PLY = 64;

    /** Transposition table, a power of two so the index is a mask of the hash. */
    private final static int TT_BITS = 17;
    private final static int TT_SIZE = 1 << TT_BITS;
    private final static byte TT_EXACT = 0;
    private final static byte TT_LOWER = 1;
    private final static byte TT_UPPER = 2;

    Board board;
    Board clone;
    private final int maxDepth;
    private final long budgetMs;
    private int nodes;
    private long deadline;
    private boolean aborted;
    /**
     * Keys along the line being searched, plus the ones the game has already stood in. A line
     * that comes back to any of them is going nowhere: scoring it a draw is what stops the search
     * from treating an endless check as a win, or shuffling a piece back and forth for lack of
     * anything better.
     */
    private final long[] path = new long[MAX_PLY + 8];
    private long[] played = new long[0];

    /**
     * One move list per level of the stack, reused. Every node needs somewhere to put its moves,
     * but only one node per level is ever being searched at a time, so the lists - and the arrays
     * behind them, once they have grown to fit - can be handed out again instead of rebuilt. This
     * was the search's largest single source of garbage.
     */
    @SuppressWarnings("unchecked")
    private final ArrayList<State>[] lists = new ArrayList[MAX_PLY + 8];

    private ArrayList<State> listAt(int ply) {
        if (ply < 0 || ply >= lists.length) return new ArrayList<>();
        ArrayList<State> list = lists[ply];
        if (list == null) {
            list = new ArrayList<>(48);
            lists[ply] = list;
        } else {
            list.clear();
        }
        return list;
    }

    /**
     * How many chariots, cannons and horses each side still has, kept in step by {@link #doMove}
     * and {@link #reMove}. Only the null move below reads it, and it needs the answer at nearly
     * every node, which is too often to count the board out each time.
     */
    private int heavyRed;
    private int heavyBlack;

    /** Zobrist key of {@link #clone}, kept in step by {@link #doMove} and {@link #reMove}. */
    private long hash;
    private final long[] ttKey = new long[TT_SIZE];
    private final int[] ttScore = new int[TT_SIZE];
    private final int[] ttMove = new int[TT_SIZE];
    private final byte[] ttDepth = new byte[TT_SIZE];
    private final byte[] ttFlag = new byte[TT_SIZE];

    /**
     * Quiet moves that caused a cutoff. A move that refuted one line usually refutes its
     * siblings, so trying them early buys cutoffs that plain capture ordering cannot: without
     * this every quiet move is tied and they get searched in whatever order they were generated.
     */
    private final int[][] killers = new int[MAX_PLY][2];
    /** How often a quiet move from one square to another has caused a cutoff, anywhere. */
    private final int[][] history = new int[90][90];

    /**
     * The lobby's three levels reach here as 2, 3 and 4 - easy, hard and very hard - and become
     * searches four, six and eight plies deep. Two plies a level rather than one, because a
     * single ply changes how the machine plays less than it sounds like it should: one side's
     * reply is only half an exchange, and it takes the pair to see the exchange through.
     */
    public AI(Board b, int level) {
        this.board = b;
        int lv = Math.max(1, level);
        // A level of 0 would never run an iteration, leaving no move to play.
        maxDepth = lv * 2;
        budgetMs = Math.min(BUDGET_CAP, BUDGET_STEP << (2 * (lv - 1)));
    }

    /**
     * What each piece is worth when ordering moves. Only their ratios matter here - the score the
     * search actually works with comes from the position tables in the piece classes.
     */
    private static int pieceValue(byte value) {
        return switch (value) {
            case 12, 19 -> 900;   // rook
            case 13, 20 -> 450;   // cannon
            case 11, 18 -> 400;   // knight
            case 10, 17 -> 220;   // elephant
            case 9, 16 -> 200;    // advisor
            case 14, 21 -> 100;   // pawn
            case 8, 15 -> 10000;  // king
            default -> 0;
        };
    }

    /**
     * Most valuable victim, least valuable attacker: try the captures that win the most material
     * first, so a cutoff usually comes from the first move or two and the rest go unsearched.
     * Without this the pruning has almost nothing to work with and the search is close to a plain
     * minimax.
     */
    private static final Comparator<State> BY_CAPTURE =
            (a, b) -> captureScore(b) - captureScore(a);

    /**
     * What a piece is worth on the scale the position tables use - a rook is 90 there, not 900 -
     * so it can be compared against a score straight out of {@link #eval}.
     */
    private static int evalValue(byte value) {
        return switch (value) {
            case 12, 19 -> 90;
            case 13, 20 -> 50;
            case 11, 18 -> 40;
            case 10, 17 -> 25;
            case 9, 16 -> 20;
            case 14, 21 -> 14;
            default -> 0;
        };
    }

    private static int captureScore(State move) {
        if (move.captured == 0) return 0;
        return pieceValue(move.captured) * 10 - pieceValue(move.piece);
    }

    /**
     * What every piece is worth on every square, flattened once at class load: the value, the
     * square and the side are all folded into one lookup, already signed the way {@link #eval}
     * counts it - red positive. Reading a leaf's score is then one array access per occupied
     * square, where before it was a switch, a table lookup and a {@link Point} allocated and
     * thrown away for each of the ninety squares.
     */
    private static final int[][] PIECE_SQUARE = new int[22][Board.ROW * Board.COL];

    static {
        table((byte) 8, (byte) 15, CKing.KING_TABLE);
        table((byte) 9, (byte) 16, CBishop.BISHOP_TABLE);
        table((byte) 10, (byte) 17, CElephant.ELEPHANT_TABLE);
        table((byte) 11, (byte) 18, CKnight.KNIGHT_TABLE);
        table((byte) 12, (byte) 19, CRook.ROOK_TABLE);
        table((byte) 13, (byte) 20, CCannon.CANNON_TABLE);
        table((byte) 14, (byte) 21, CPawn.PAWN_TABLE);
    }

    /**
     * Fills in one kind of piece for both sides. The tables are written from black's side of the
     * board, so red reads the same table upside down and mirrored, and counts the other way.
     */
    private static void table(byte black, byte red, int[][] values) {
        for (int x = 0; x < Board.ROW; x++) {
            for (int y = 0; y < Board.COL; y++) {
                int square = x * Board.COL + y;
                PIECE_SQUARE[black][square] = -values[x][y];
                PIECE_SQUARE[red][square] = values[Board.ROW - 1 - x][Board.COL - 1 - y];
            }
        }
    }

    /** Live pieces of each side, indexed advisor, elephant, knight, rook, cannon, pawn. */
    private final int[] countRed = new int[6];
    private final int[] countBlack = new int[6];

    /**
     * The score of the position on {@link #clone}, from {@code side}'s point of view. One pass
     * over the board does both halves of it: the piece-square total, and the census the two
     * bonuses below need. It used to be four passes - one to score, two to count, and the
     * development check - and this is the single most-run routine in the search, once at every
     * leaf the quiescence search settles on.
     */
    int eval(boolean side) {
        java.util.Arrays.fill(countRed, 0);
        java.util.Arrays.fill(countBlack, 0);
        int s = 0;
        byte[][] cell = clone.cell;
        for (int x = 0; x < Board.ROW; x++) {
            byte[] row = cell[x];
            int square = x * Board.COL;
            for (int y = 0; y < Board.COL; y++) {
                byte v = row[y];
                if (v == 0) continue;
                s += PIECE_SQUARE[v][square + y];
                // Kings are not counted: neither bonus asks after a piece that cannot be lost.
                if (v >= 9 && v <= 14) countBlack[v - 9]++;
                else if (v >= 16) countRed[v - 16]++;
            }
        }

        if (!side) s = -s;
        int[] mine = side ? countRed : countBlack;
        int[] theirs = side ? countBlack : countRed;
        // A side short of advisors or elephants is far more exposed to cannons and knights, so
        // those pieces are worth holding on to when the opponent has lost its guards.
        s += attackBonus(mine, theirs);
        s -= attackBonus(theirs, mine);
        // A rook still sitting in the corner behind its own knight is doing nothing.
        s += development(side);
        s -= development(!side);
        return s;
    }

    /** What {@code mine}'s attackers gain from the guards {@code theirs} has already lost. */
    private static int attackBonus(int[] mine, int[] theirs) {
        int s = 0;
        if (theirs[0] < 2) s += 2 * mine[4] + mine[2];   // cannons and knights against few advisors
        if (theirs[1] < 2) s += 2 * mine[4] + mine[3];   // cannons and rooks against few elephants
        return s;
    }

    /** Penalty for a rook that has not left its starting corner, boxed in by its own knight. */
    private int development(boolean side) {
        int row = side ? 0 : 9;
        byte rook = side ? (byte) 19 : (byte) 12;
        byte knight = side ? (byte) 18 : (byte) 11;
        int s = 0;
        if (clone.cell[row][0] == rook && clone.cell[row][1] == knight) s -= 5;
        if (clone.cell[row][8] == rook && clone.cell[row][7] == knight) s -= 5;
        return s;
    }

    void doMove(State state) {
        clone.cell[state.to.x][state.to.y] = state.piece;
        clone.cell[state.from.x][state.from.y] = 0;
        if (isHeavy(state.captured)) {
            if (state.captured > 14) heavyRed--;
            else heavyBlack--;
        }
        toggle(state);
    }

    void reMove(State state) {
        clone.cell[state.to.x][state.to.y] = state.captured;
        clone.cell[state.from.x][state.from.y] = state.piece;
        if (isHeavy(state.captured)) {
            if (state.captured > 14) heavyRed++;
            else heavyBlack++;
        }
        toggle(state);
    }

    /** Chariot, horse or cannon - the pieces that can still create a threat out of nothing. */
    private static boolean isHeavy(byte value) {
        return (value >= 11 && value <= 13) || (value >= 18 && value <= 20);
    }

    /** Xor is its own inverse, so making and unmaking a move run the same four operations. */
    private void toggle(State state) {
        int from = state.from.x * Board.COL + state.from.y;
        int to = state.to.x * Board.COL + state.to.y;
        hash ^= Board.ZOBRIST[from][state.piece];
        hash ^= Board.ZOBRIST[to][state.piece];
        if (state.captured != 0) hash ^= Board.ZOBRIST[to][state.captured];
        hash ^= Board.ZOBRIST_SIDE;
    }

    private static int code(State move) {
        return ((move.from.x * Board.COL + move.from.y) << 8)
                | (move.to.x * Board.COL + move.to.y);
    }

    /** Move scores, one array per level of the stack, grown to fit and reused like the lists. */
    private final int[][] scores = new int[MAX_PLY + 8][];

    /**
     * Scores every move once: best move first, then winning captures, then the quiet moves that
     * have refuted something before. The list is left in the order it was generated - what comes
     * next is picked out of it one at a time by {@link #promote}.
     */
    private int[] scoreMoves(ArrayList<State> moves, int wanted, int ply) {
        int n = moves.size();
        int[] score;
        if (ply < 0 || ply >= scores.length) {
            score = new int[n];
        } else {
            score = scores[ply];
            if (score == null || score.length < n) {
                score = new int[Math.max(n, 48)];
                scores[ply] = score;
            }
        }
        for (int i = 0; i < n; i++) {
            score[i] = moveScore(moves.get(i), wanted, ply);
        }
        return score;
    }

    /**
     * Brings the best of the moves not yet searched into position {@code i}, leaving the rest in
     * the order they were generated. Sorting the whole list up front costs the same whether the
     * node searches every move or cuts on the first one - and a well-ordered search cuts on the
     * first one most of the time, so all but a move or two of that sorting was thrown away.
     */
    private static void promote(ArrayList<State> moves, int[] score, int i) {
        int n = moves.size();
        int bestAt = i;
        // Strictly greater, so equal scores keep the order they arrived in.
        for (int j = i + 1; j < n; j++) {
            if (score[j] > score[bestAt]) bestAt = j;
        }
        if (bestAt == i) return;
        State move = moves.get(bestAt);
        int sc = score[bestAt];
        for (int j = bestAt; j > i; j--) {
            moves.set(j, moves.get(j - 1));
            score[j] = score[j - 1];
        }
        moves.set(i, move);
        score[i] = sc;
    }

    private int moveScore(State move, int wanted, int ply) {
        int c = code(move);
        if (c == wanted) return 1 << 26;
        if (move.captured != 0) {
            return (1 << 22) + pieceValue(move.captured) * 16 - pieceValue(move.piece);
        }
        if (ply < MAX_PLY) {
            if (c == killers[ply][0]) return (1 << 21) + 1;
            if (c == killers[ply][1]) return 1 << 21;
        }
        return history[c >> 8][c & 0xFF];
    }

    /** Remembers a quiet move that caused a cutoff, so its siblings are tried after it. */
    private void remember(State move, int depth, int ply) {
        if (move.captured != 0) return;
        int c = code(move);
        if (ply < MAX_PLY && killers[ply][0] != c) {
            killers[ply][1] = killers[ply][0];
            killers[ply][0] = c;
        }
        int h = history[c >> 8][c & 0xFF] + depth * depth;
        history[c >> 8][c & 0xFF] = Math.min(h, 1 << 20);
    }

    /** Mate scores are stored counted from the position, not from the root that found them. */
    private static int toTT(int score, int ply) {
        if (score >= MATE_BOUND) return score + ply;
        if (score <= -MATE_BOUND) return score - ply;
        return score;
    }

    private static int fromTT(int score, int ply) {
        if (score >= MATE_BOUND) return score - ply;
        if (score <= -MATE_BOUND) return score + ply;
        return score;
    }

    /**
     * {@code side} is the side to move at this node - and {@code allMoves} takes the other one,
     * which is why it is handed {@code !side}. {@code ply} counts down from the root, and is what
     * mate scores are measured in.
     */
    public int alphaBeta(int depth, boolean side, int alpha, int beta, int ply) {
        nodes++;
        if (outOfTime()) return 0;
        if (ply > 0 && repeats(ply)) return 0;
        if (depth <= 0) return quiesce(side, alpha, beta, ply);

        // The same position is reached by many different orders of the same moves. If one of
        // those orders has already been searched at least this deep, its answer stands.
        int slot = (int) (hash & (TT_SIZE - 1));
        int wanted = 0;
        if (ttKey[slot] == hash) {
            wanted = ttMove[slot];
            if (ttDepth[slot] >= depth) {
                int stored = fromTT(ttScore[slot], ply);
                byte flag = ttFlag[slot];
                if (flag == TT_EXACT
                        || (flag == TT_LOWER && stored >= beta)
                        || (flag == TT_UPPER && stored <= alpha)) {
                    return stored;
                }
            }
        }

        // Null move: hand the turn straight back and see whether the position is still good
        // enough to cut. If doing nothing already beats beta, doing something will too, and the
        // whole subtree can go unsearched - which is where most of the saving at the deeper
        // levels comes from.
        //
        // Three things have to hold first. A side in check has no turn to give away. A side down
        // to its general, guards and soldiers may be in zugzwang, where every move it has makes
        // its position worse and passing would be better than anything legal - the one case
        // where the argument above is false. And the test only pays for itself on a node deep
        // enough to have a real subtree under it.
        if (depth >= 3 && beta - alpha == 1 && (side ? heavyRed : heavyBlack) > 0
                && clone.kingSafe(side)) {
            int reduction = depth > 6 ? 3 : 2;
            hash ^= Board.ZOBRIST_SIDE;
            int value = -alphaBeta(depth - 1 - reduction, !side, -beta, -beta + 1, ply + 1);
            hash ^= Board.ZOBRIST_SIDE;
            // A mate found beyond a move nobody may play is not a mate. Cutting on the bound is
            // sound - claiming the mate score is not.
            if (value >= beta && !aborted) return value >= MATE_BOUND ? beta : value;
        }

        // Pseudo-legal: whether a move leaves its own general in check is settled below, on the
        // moves that actually get played. At a node that cuts after one or two of them, the rest
        // never need the test - and that test was the single most expensive thing in the search.
        ArrayList<State> moves = listAt(ply);
        clone.collect(side, false, false, moves);

        int[] score = scoreMoves(moves, wanted, ply);
        int alphaOrig = alpha;
        int best = -INF;
        State bestHere = null;
        boolean first = true;
        boolean any = false;
        for (int i = 0; i < moves.size(); i++) {
            promote(moves, score, i);
            State m = moves.get(i);
            doMove(m);
            if (!clone.kingSafe(side)) {
                reMove(m);
                continue;
            }
            any = true;
            int value;
            if (first) {
                value = -alphaBeta(depth - 1, !side, -beta, -alpha, ply + 1);
            } else {
                // Ordering means the first move is usually best, so the rest only have to be
                // shown to be worse - a null window does that for a fraction of the work, and
                // only the rare move that beats it gets searched again properly.
                value = -alphaBeta(depth - 1, !side, -alpha - 1, -alpha, ply + 1);
                if (value > alpha && value < beta) {
                    value = -alphaBeta(depth - 1, !side, -beta, -alpha, ply + 1);
                }
            }
            reMove(m);
            first = false;

            if (value > best) {
                best = value;
                bestHere = m;
            }
            if (best > alpha) alpha = best;
            if (alpha >= beta) {
                remember(m, depth, ply);
                break;
            }
        }
        // Not one move was legal: mated, or stalemated, which xiangqi also scores as a loss.
        if (!any) return -(MATE - ply);

        if (!aborted) {
            byte flag = best <= alphaOrig ? TT_UPPER : (best >= beta ? TT_LOWER : TT_EXACT);
            if (ttKey[slot] != hash || ttDepth[slot] <= depth) {
                ttKey[slot] = hash;
                ttScore[slot] = toTT(best, ply);
                ttDepth[slot] = (byte) depth;
                ttFlag[slot] = flag;
                ttMove[slot] = bestHere == null ? 0 : code(bestHere);
            }
        }
        return best;
    }

    /**
     * Plays out the captures still hanging at a leaf before scoring it. Without this the search
     * stops mid-exchange and reads a piece it is about to lose back as a piece won - it would
     * grab a defended rook on the last ply and never see the recapture.
     */
    private int quiesce(boolean side, int alpha, int beta, int ply) {
        nodes++;
        if (outOfTime()) return 0;
        // Standing pat: the side to move is not obliged to capture, so its score is a floor.
        int stand = eval(side);
        if (stand >= beta) return stand;
        if (stand > alpha) alpha = stand;
        if (ply >= maxDepth + QUIET_PLIES) return stand;

        if (!clone.hasLegalMove(side)) return -(MATE - ply);
        // Only the captures are ever played here, and at a leaf they are a handful of the forty
        // or so moves a position has - so listing the rest, and proving each of them legal, was
        // most of the work done at the most-visited nodes in the search.
        ArrayList<State> moves = listAt(ply);
        clone.collect(side, true, true, moves);
        moves.sort(BY_CAPTURE);

        int best = stand;
        for (State m : moves) {
            // Even winning this piece outright would not reach alpha, and neither will anything
            // behind it, since the captures are ordered by what they win.
            if (stand + evalValue(m.captured) + DELTA < alpha) break;
            doMove(m);
            int value = -quiesce(!side, -beta, -alpha, ply + 1);
            reMove(m);

            if (value > best) best = value;
            if (best > alpha) alpha = best;
            if (alpha >= beta) break;
        }
        return best;
    }

    /**
     * True once the budget is spent. The score returned from here on is meaningless, but the root
     * throws the whole unfinished iteration away, so it never reaches the move that gets played.
     */
    private boolean outOfTime() {
        if (aborted) return true;
        if ((nodes & 255) == 0 && System.currentTimeMillis() > deadline) aborted = true;
        return aborted;
    }

    /**
     * Deepens one ply at a time and keeps the answer from the last iteration that finished. Each
     * pass also re-orders the root so the previous best is tried first, which is where most of
     * the pruning at the next depth comes from - so the repeated shallow searches more than pay
     * for themselves.
     */
    public State generateMove(boolean side) {
        clone = new Board(board);
        nodes = 0;
        deadline = System.currentTimeMillis() + budgetMs;
        hash = zobristOf(side);
        heavyRed = 0;
        heavyBlack = 0;
        for (int x = 0; x < Board.ROW; x++) {
            for (int y = 0; y < Board.COL; y++) {
                byte v = clone.cell[x][y];
                if (!isHeavy(v)) continue;
                if (v > 14) heavyRed++;
                else heavyBlack++;
            }
        }
        path[0] = hash;
        played = new long[Math.max(0, board.history.size() - 1)];
        for (int i = 0; i < played.length; i++) {
            played[i] = board.history.get(i).key;
        }
        for (int[] row : history) java.util.Arrays.fill(row, 0);
        for (int[] pair : killers) {
            pair[0] = 0;
            pair[1] = 0;
        }
        long started = System.currentTimeMillis();

        ArrayList<State> rootMoves = new ArrayList<>();
        clone.collect(side, false, true, rootMoves);
        if (rootMoves.isEmpty()) return null;
        rootMoves.sort(BY_CAPTURE);
        // Something legal to play even if the very first iteration does not finish.
        State bestMove = rootMoves.get(0);

        int reached = 0;
        for (int depth = 1; depth <= maxDepth; depth++) {
            if (depth > 1 && !worthDeepening(started)) break;
            aborted = false;
            State found = searchRoot(rootMoves, depth, side);
            if (aborted) break;
            bestMove = found;
            reached = depth;
            rootMoves.remove(found);
            rootMoves.add(0, found);
        }
        Log.d("AI", "level depth " + reached + "/" + maxDepth + "  budget " + budgetMs
                + "ms  nodes " + nodes + "  " + (System.currentTimeMillis() - started) + "ms");
        return bestMove;
    }

    /**
     * Whether there is time for another ply. The next one costs about five or six times
     * everything spent so far, so this only lets one start while under an eighth of the budget
     * has gone - a deliberate margin, because a ply begun and abandoned spends the rest of the
     * clock on a result the root throws away, and finishing one ply lower is the better trade.
     */
    private boolean worthDeepening(long started) {
        return (System.currentTimeMillis() - started) * 8 < budgetMs;
    }

    /** The key of the position on {@link #clone}, read the slow way, once per search. */
    private long zobristOf(boolean side) {
        long key = side ? Board.ZOBRIST_SIDE : 0L;
        for (int x = 0; x < Board.ROW; x++) {
            for (int y = 0; y < Board.COL; y++) {
                byte v = clone.cell[x][y];
                if (v != 0) key ^= Board.ZOBRIST[x * Board.COL + y][v];
            }
        }
        return key;
    }

    /** Whether the position at {@code ply} has already stood on the board, here or in the game. */
    private boolean repeats(int ply) {
        path[ply] = hash;
        for (int i = ply - 2; i >= 0; i -= 2) {
            if (path[i] == hash) return true;
        }
        for (long key : played) {
            if (key == hash) return true;
        }
        return false;
    }

    private State searchRoot(ArrayList<State> moves, int depth, boolean side) {
        int alpha = -INF;
        State best = moves.get(0);
        for (State m : moves) {
            doMove(m);
            int value = -alphaBeta(depth - 1, !side, -INF, -alpha, 1);
            reMove(m);
            if (aborted) return best;
            if (value > alpha) {
                alpha = value;
                best = m;
            }
        }
        return best;
    }
}
