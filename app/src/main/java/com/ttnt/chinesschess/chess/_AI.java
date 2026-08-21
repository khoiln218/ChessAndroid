package com.ttnt.chinesschess.chess;

import android.graphics.Point;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
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
public class _AI {

    /** Beyond any score {@link #eval} can produce; the material on a full board sums to ~570. */
    private final static int INF = 1_000_000;
    private final static int MATE = 900_000;
    /** How many plies of captures the quiescence search may chase past the nominal depth. */
    private final static int QUIET_PLIES = 4;
    /** Slack allowed on top of a capture before writing it off, on the scale {@link #eval} uses. */
    private final static int DELTA = 25;
    /**
     * How much an extra ply costs, measured from the opening position: depth 3 finishes in 0.29s,
     * depth 4 in 3.9s, depth 5 in 28s - about fourteen times the previous ply each time, which is
     * also roughly a dozen times everything spent up to that point.
     *
     * <p>That ratio is what separates the levels. A single shared budget makes every level stop
     * at the same ply, because the reachable depth is decided by the clock and not by the level;
     * so each level gets four times the time of the one below it, which buys it about one ply.
     */
    private final static long BUDGET_STEP = 375L;
    private final static long BUDGET_CAP = 15000L;

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

    public _AI(Board b, int level) {
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
        if (move.value2 == 0) return 0;
        return pieceValue(move.value2) * 10 - pieceValue(move.value1);
    }

    /**
     * Two things the opening tables cannot say on their own. Both are small - single digits on a
     * scale where a rook is 90 - so they nudge between otherwise equal moves rather than steering
     * the search.
     */
    int bonus(boolean side) {
        int[] mine = count(side);
        int[] theirs = count(!side);

        int s = 0;
        // A side short of advisors or elephants is far more exposed to cannons and knights, so
        // those pieces are worth holding on to when the opponent has lost its guards.
        s += attackBonus(mine, theirs);
        s -= attackBonus(theirs, mine);
        // A rook still sitting in the corner behind its own knight is doing nothing.
        s += development(side);
        s -= development(!side);
        return s;
    }

    /** Live pieces of one side, indexed advisor, elephant, knight, rook, cannon, pawn. */
    private int[] count(boolean red) {
        int[] n = new int[6];
        for (int x = 0; x < Board.ROW; x++) {
            for (int y = 0; y < Board.COL; y++) {
                byte v = clone.cell[x][y];
                if (v == 0 || (v > 14) != red) continue;
                switch (v) {
                    case 9, 16 -> n[0]++;
                    case 10, 17 -> n[1]++;
                    case 11, 18 -> n[2]++;
                    case 12, 19 -> n[3]++;
                    case 13, 20 -> n[4]++;
                    case 14, 21 -> n[5]++;
                }
            }
        }
        return n;
    }

    /** What {@code mine}'s attackers gain from the guards {@code theirs} has already lost. */
    private static int attackBonus(int[] mine, int[] theirs) {
        int s = 0;
        if (theirs[0] < 2) s += 2 * mine[4] + mine[2];   // cannons and knights against few advisors
        if (theirs[1] < 2) s += 2 * mine[4] + mine[3];   // cannons and rooks against few elephants
        return s;
    }

    /** Penalty for a rook that has not left its starting corner, boxed in by its own knight. */
    private int development(boolean red) {
        int row = red ? 0 : 9;
        byte rook = red ? (byte) 19 : (byte) 12;
        byte knight = red ? (byte) 18 : (byte) 11;
        int s = 0;
        if (clone.cell[row][0] == rook && clone.cell[row][1] == knight) s -= 5;
        if (clone.cell[row][8] == rook && clone.cell[row][7] == knight) s -= 5;
        return s;
    }

    int eval(boolean side) {
        int s = 0;
        for (int x = 0; x <= 9; x++) {
            for (int y = 0; y <= 8; y++) {
                byte value = clone.getValue(x, y);
                switch (value) {
                    case 8:
                        s -= CKing.getPositionValue(new Point(x, y), false);
                        break;
                    case 15:
                        s += CKing.getPositionValue(new Point(x, y), true);
                        break;
                    case 9:
                        s -= CBishop.getPositionValue(new Point(x, y), false);
                        break;
                    case 16:
                        s += CBishop.getPositionValue(new Point(x, y), true);
                        break;
                    case 10:
                        s -= CElephant.getPositionValue(new Point(x, y), false);
                        break;
                    case 17:
                        s += CElephant.getPositionValue(new Point(x, y), true);
                        break;
                    case 11:
                        s -= CKnight.getPositionValue(new Point(x, y), false);
                        break;
                    case 18:
                        s += CKnight.getPositionValue(new Point(x, y), true);
                        break;
                    case 12:
                        s -= CRook.getPositionValue(new Point(x, y), false);
                        break;
                    case 19:
                        s += CRook.getPositionValue(new Point(x, y), true);
                        break;
                    case 13:
                        s -= CCannon.getPositionValue(new Point(x, y), false);
                        break;
                    case 20:
                        s += CCannon.getPositionValue(new Point(x, y), true);
                        break;
                    case 14:
                        s -= CPawn.getPositionValue(new Point(x, y), false);
                        break;
                    case 21:
                        s += CPawn.getPositionValue(new Point(x, y), true);
                        break;
                }
            }
        }
        if (!side) {
            s = -s;
        }
        return s + bonus(side);
    }

    void doMove(State state) {
        clone.cell[state.curr.x][state.curr.y] = state.value1;
        clone.cell[state.prev.x][state.prev.y] = 0;
        toggle(state);
    }

    void reMove(State state) {
        clone.cell[state.curr.x][state.curr.y] = state.value2;
        clone.cell[state.prev.x][state.prev.y] = state.value1;
        toggle(state);
    }

    /** Xor is its own inverse, so making and unmaking a move run the same four operations. */
    private void toggle(State state) {
        int from = state.prev.x * Board.COL + state.prev.y;
        int to = state.curr.x * Board.COL + state.curr.y;
        hash ^= Board.ZOBRIST[from][state.value1];
        hash ^= Board.ZOBRIST[to][state.value1];
        if (state.value2 != 0) hash ^= Board.ZOBRIST[to][state.value2];
        hash ^= Board.ZOBRIST_SIDE;
    }

    private static int code(State move) {
        return ((move.prev.x * Board.COL + move.prev.y) << 8)
                | (move.curr.x * Board.COL + move.curr.y);
    }

    /**
     * Best move first, then winning captures, then the quiet moves that have refuted something
     * before. The list is short enough that an insertion sort beats allocating for a comparator.
     */
    private void order(ArrayList<State> moves, int wanted, int ply) {
        int n = moves.size();
        int[] score = new int[n];
        for (int i = 0; i < n; i++) {
            score[i] = moveScore(moves.get(i), wanted, ply);
        }
        for (int i = 1; i < n; i++) {
            State move = moves.get(i);
            int sc = score[i];
            int j = i - 1;
            while (j >= 0 && score[j] < sc) {
                moves.set(j + 1, moves.get(j));
                score[j + 1] = score[j];
                j--;
            }
            moves.set(j + 1, move);
            score[j + 1] = sc;
        }
    }

    private int moveScore(State move, int wanted, int ply) {
        int c = code(move);
        if (c == wanted) return 1 << 26;
        if (move.value2 != 0) {
            return (1 << 22) + pieceValue(move.value2) * 16 - pieceValue(move.value1);
        }
        if (ply < MAX_PLY) {
            if (c == killers[ply][0]) return (1 << 21) + 1;
            if (c == killers[ply][1]) return 1 << 21;
        }
        return history[c >> 8][c & 0xFF];
    }

    /** Remembers a quiet move that caused a cutoff, so its siblings are tried after it. */
    private void remember(State move, int depth, int ply) {
        if (move.value2 != 0) return;
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

        ArrayList<State> moves = clone.allMoves(!side);
        // No move at all: mated, or stalemated, which xiangqi also scores as a loss.
        if (moves.isEmpty()) return -(MATE - ply);

        order(moves, wanted, ply);
        int alphaOrig = alpha;
        int best = -INF;
        State bestHere = null;
        boolean first = true;
        for (State m : moves) {
            doMove(m);
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

        ArrayList<State> moves = clone.allMoves(!side);
        if (moves.isEmpty()) return -(MATE - ply);
        Collections.sort(moves, BY_CAPTURE);

        int best = stand;
        for (State m : moves) {
            if (m.value2 == 0) continue;   // quiet move: nothing left to resolve here
            // Even winning this piece outright would not reach alpha, and neither will anything
            // behind it, since the captures are ordered by what they win.
            if (stand + evalValue(m.value2) + DELTA < alpha) break;
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
    public State generateMove(boolean RED) {
        clone = new Board(board);
        nodes = 0;
        deadline = System.currentTimeMillis() + budgetMs;
        hash = zobristOf(RED);
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

        ArrayList<State> rootMoves = clone.allMoves(!RED);
        if (rootMoves.isEmpty()) return null;
        Collections.sort(rootMoves, BY_CAPTURE);
        // Something legal to play even if the very first iteration does not finish.
        State bestMove = rootMoves.get(0);

        int reached = 0;
        for (int depth = 1; depth <= maxDepth; depth++) {
            if (depth > 1 && !worthDeepening(started)) break;
            aborted = false;
            State found = searchRoot(rootMoves, depth, RED);
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
     * Whether there is time for another ply. The next one costs roughly a dozen times everything
     * spent so far, so once an eighth of the budget is gone it cannot finish, and starting it
     * would only burn the rest of the clock on a result the root throws away.
     */
    private boolean worthDeepening(long started) {
        return (System.currentTimeMillis() - started) * 8 < budgetMs;
    }

    /** The key of the position on {@link #clone}, read the slow way, once per search. */
    private long zobristOf(boolean red) {
        long key = red ? Board.ZOBRIST_SIDE : 0L;
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
