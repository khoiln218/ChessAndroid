package com.ttnt.chinesechess.ai.optimize;

import com.ttnt.chinesechess.chess.Board;
import com.ttnt.chinesechess.chess.Move;
import com.ttnt.chinesechess.chess.MoveRecord;
import com.ttnt.chinesechess.chess.PieceCode;
import com.ttnt.chinesechess.chess.Rules;

import java.util.ArrayList;
import java.util.Random;

/**
 * The board {@link EnhancedAlphaBeta} plays on: a {@link Board} that keeps in step, with every
 * move made and taken back, what the search asks about at nearly every node - the Zobrist
 * {@link #key}, how many attacking pieces each side has left, and the line of positions since the
 * start of the game. Worked out afresh at each node they would cost more than the search itself;
 * kept here, the plain algorithms never pay for them. What the search makes of the board - the
 * side to move's score, whether the game is over - is {@link OptimizedChessState}.
 */
public final class OptimizedBoard extends Board {

    /** Deepest line {@link #repeated} and the reused move lists cover; searches stay far short. */
    static final int MAX_PLY = 72;

    // ===== Zobrist hashing =====

    /**
     * One random number per piece per square, plus one for the side to move. Xor-ing them
     * together identifies a position; xor-ing a piece out of one square and into another updates
     * that identity in a few operations instead of re-reading the whole board. The search uses
     * them for its transposition table and for spotting a line that goes round in a circle.
     */
    private static final long[][] ZOBRIST = new long[SQUARES][PieceCode.COUNT];
    private static final long ZOBRIST_SIDE;

    static {
        // Fixed seed: the same game always hashes the same way, which makes a bug reproducible.
        Random rand = new Random(0x9E3779B97F4A7C15L);
        for (int square = 0; square < ZOBRIST.length; square++) {
            for (int piece = 0; piece < PieceCode.COUNT; piece++) {
                ZOBRIST[square][piece] = rand.nextLong();
            }
        }
        ZOBRIST_SIDE = rand.nextLong();
    }

    /** Zobrist key of the board and the side to move, kept in step by every move and pass. */
    public long key;
    /**
     * How many chariots, cannons and horses each side still has. A side down to its general,
     * guards and soldiers may be in zugzwang, which is what the null move has to stay clear of.
     */
    private int heavyRed;
    private int heavyBlack;

    /**
     * Keys along the line being searched, plus the ones the game has already stood in. A line
     * that comes back to any of them is going nowhere: scoring it a draw is what stops the search
     * from treating an endless check as a win, or shuffling a piece back and forth for lack of
     * anything better.
     */
    private final long[] path = new long[MAX_PLY + 1];
    private final long[] played;

    /**
     * One move list per level of the stack, reused. Every node needs somewhere to put its moves,
     * but only one node per level is ever being searched at a time, so the lists - and the arrays
     * behind them, once they have grown to fit - can be handed out again instead of rebuilt.
     */
    @SuppressWarnings("unchecked")
    private final ArrayList<Move>[] lists = new ArrayList[MAX_PLY + 1];

    /**
     * A copy of {@code position} with {@code redToMove} to move, and the game's record behind
     * it.
     */
    public OptimizedBoard(Board position, boolean redToMove) {
        super(position);
        this.redToMove = redToMove;
        long k = redToMove ? ZOBRIST_SIDE : 0L;
        for (int x = 0; x < ROW; x++) {
            for (int y = 0; y < COL; y++) {
                byte v = cell[x][y];
                if (v != PieceCode.EMPTY) k ^= ZOBRIST[x * COL + y][v];
                if (!isHeavy(v)) continue;
                if (PieceCode.isRed(v)) heavyRed++;
                else heavyBlack++;
            }
        }
        key = k;
        path[0] = key;
        // The last entry of the record is the position on the board now, which is the root.
        played = new long[Math.max(0, position.history.size() - 1)];
        for (int i = 0; i < played.length; i++) {
            played[i] = keyOf(position.history.get(i));
        }
    }

    /** Plays {@code move}, which leads to the position {@code ply} moves from the root. */
    public void makeMove(Move move, int ply) {
        cell[move.to.x][move.to.y] = move.piece;
        cell[move.from.x][move.from.y] = PieceCode.EMPTY;
        if (isHeavy(move.captured)) {
            if (PieceCode.isRed(move.captured)) heavyRed--;
            else heavyBlack--;
        }
        toggle(move);
        if (ply <= MAX_PLY) path[ply] = key;
    }

    public void unmakeMove(Move move) {
        cell[move.to.x][move.to.y] = move.captured;
        cell[move.from.x][move.from.y] = move.piece;
        if (isHeavy(move.captured)) {
            if (PieceCode.isRed(move.captured)) heavyRed++;
            else heavyBlack++;
        }
        toggle(move);
    }

    /** Hands the turn over without moving, to the position {@code ply} moves from the root. */
    public void pass(int ply) {
        key ^= ZOBRIST_SIDE;
        redToMove = !redToMove;
        if (ply <= MAX_PLY) path[ply] = key;
    }

    public void unpass() {
        key ^= ZOBRIST_SIDE;
        redToMove = !redToMove;
    }

    /** Whether the position {@code ply} moves from the root has stood before, same side to move. */
    public boolean repeated(int ply) {
        for (int i = Math.min(ply, MAX_PLY) - 2; i >= 0; i -= 2) {
            if (path[i] == key) return true;
        }
        for (long k : played) {
            if (k == key) return true;
        }
        return false;
    }

    /** Whether {@code side} still has a chariot, cannon or horse. */
    public boolean hasHeavy(boolean side) {
        return (side ? heavyRed : heavyBlack) > 0;
    }

    /**
     * {@link Rules#collect} for the side to move into the list kept for {@code ply}, emptied
     * first. The list is only good until the next call for the same ply.
     */
    public ArrayList<Move> collectAt(int ply, boolean capturesOnly, boolean legalOnly) {
        ArrayList<Move> list;
        if (ply < 0 || ply >= lists.length) {
            list = new ArrayList<>();
        } else {
            list = lists[ply];
            if (list == null) {
                list = new ArrayList<>(48);
                lists[ply] = list;
            } else {
                list.clear();
            }
        }
        Rules.collect(this, redToMove, capturesOnly, legalOnly, list);
        return list;
    }

    /** Chariot, horse or cannon - the pieces that can still create a threat out of nothing. */
    private static boolean isHeavy(byte value) {
        if (value == PieceCode.EMPTY) return false;
        int kind = PieceCode.kind(value);
        return kind == PieceCode.KNIGHT || kind == PieceCode.ROOK || kind == PieceCode.CANNON;
    }

    /**
     * Xor is its own inverse, so making and unmaking a move run the same four operations - and
     * the side to move changes either way.
     */
    private void toggle(Move move) {
        int from = move.from.x * COL + move.from.y;
        int to = move.to.x * COL + move.to.y;
        key ^= ZOBRIST[from][move.piece];
        key ^= ZOBRIST[to][move.piece];
        if (move.captured != PieceCode.EMPTY) key ^= ZOBRIST[to][move.captured];
        key ^= ZOBRIST_SIDE;
        redToMove = !redToMove;
    }

    /** The key of a position the game has already stood in, read off its record. */
    private static long keyOf(MoveRecord record) {
        long k = record.redToMove() ? ZOBRIST_SIDE : 0L;
        for (int x = 0; x < ROW; x++) {
            for (int y = 0; y < COL; y++) {
                byte v = record.at(x, y);
                if (v != PieceCode.EMPTY) k ^= ZOBRIST[x * COL + y][v];
            }
        }
        return k;
    }
}
