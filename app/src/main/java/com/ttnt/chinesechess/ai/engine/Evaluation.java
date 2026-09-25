package com.ttnt.chinesechess.ai.engine;

import com.ttnt.chinesechess.chess.Board;
import com.ttnt.chinesechess.chess.Move;
import com.ttnt.chinesechess.chess.PieceCode;

import java.util.Arrays;
import java.util.Comparator;

/**
 * The static evaluation of a xiangqi position, and the piece values move ordering is done by.
 * Kept apart from any one search so that every search plays to the same idea of what a position
 * is worth: the optimized search and the plain ones {@link ChessState} feeds disagree only on
 * how far they look, never on what they see when they get there.
 *
 * <p>An instance holds scratch arrays, so it belongs to one search thread.
 */
public final class Evaluation {

    /**
     * What each piece is worth when ordering moves. Only their ratios matter here - the score the
     * search actually works with comes from the position tables in the piece classes.
     */
    public static int pieceValue(byte value) {
        if (value == PieceCode.EMPTY) return 0;
        return switch (PieceCode.kind(value)) {
            case PieceCode.ROOK -> 900;
            case PieceCode.CANNON -> 450;
            case PieceCode.KNIGHT -> 400;
            case PieceCode.ELEPHANT -> 220;
            case PieceCode.ADVISOR -> 200;
            case PieceCode.PAWN -> 100;
            case PieceCode.KING -> 10000;
            default -> 0;
        };
    }

    /**
     * What a piece is worth on the scale the position tables use - a rook is 90 there, not 900 -
     * so it can be compared against a score straight out of {@link #score}.
     */
    public static int evalValue(byte value) {
        if (value == PieceCode.EMPTY) return 0;
        return switch (PieceCode.kind(value)) {
            case PieceCode.ROOK -> 90;
            case PieceCode.CANNON -> 50;
            case PieceCode.KNIGHT -> 40;
            case PieceCode.ELEPHANT -> 25;
            case PieceCode.ADVISOR -> 20;
            case PieceCode.PAWN -> 14;
            default -> 0;
        };
    }

    /**
     * Most valuable victim, least valuable attacker: try the captures that win the most material
     * first, so a cutoff usually comes from the first move or two and the rest go unsearched.
     * Without this the pruning has almost nothing to work with and the search is close to a plain
     * minimax.
     */
    public static final Comparator<Move> BY_CAPTURE =
            (a, b) -> captureScore(b) - captureScore(a);

    /** How much more the victim counts than the attacker, so the victim decides the order. */
    private static final int VICTIM_WEIGHT = 10;

    static int captureScore(Move move) {
        if (move.captured == PieceCode.EMPTY) return 0;
        return pieceValue(move.captured) * VICTIM_WEIGHT - pieceValue(move.piece);
    }

    /*
     * Position tables, one per kind of piece, written from black's side of the board: what the
     * piece is worth on each square. Only the evaluation reads them, so they live here rather
     * than with the rules of how each piece moves.
     */
    private static final int[][] KING_TABLE = {
            {0, 0, 0, 0, 0, 0, 0, 0, 0}, /* KING */
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 1, 1, 1, 0, 0, 0},
            {0, 0, 0, 15, 15, 15, 0, 0, 0},
            {0, 0, 0, 30, 35, 30, 0, 0, 0}
    };

    private static final int[][] BISHOP_TABLE = {
            {0, 0, 0, 0, 0, 0, 0, 0, 0}, /* BISHOP */
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 19, 0, 19, 0, 0, 0},
            {0, 0, 0, 0, 22, 0, 0, 0, 0},
            {0, 0, 0, 20, 0, 20, 0, 0, 0}
    };

    private static final int[][] ELEPHANT_TABLE = {
            {0, 0, 0, 0, 0, 0, 0, 0, 0}, /* ELEPHANT */
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 22, 0, 0, 0, 22, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {23, 0, 0, 0, 28, 0, 0, 0, 23},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 25, 0, 0, 0, 25, 0, 0}
    };

    private static final int[][] KNIGHT_TABLE = {
            {40, 40, 40, 40, 40, 40, 40, 40, 40}, /* KNIGHT */
            {40, 41, 42, 42, 42, 42, 42, 41, 40},
            {40, 43, 44, 44, 44, 44, 44, 43, 40},
            {40, 43, 44, 44, 44, 44, 44, 43, 40},
            {40, 43, 44, 44, 44, 44, 44, 43, 40},
            {40, 43, 44, 44, 44, 44, 44, 43, 40},
            {40, 42, 43, 43, 43, 43, 43, 42, 40},
            {40, 42, 43, 40, 40, 40, 43, 42, 40},
            {40, 41, 42, 40, 20, 40, 42, 41, 40},
            {40, 35, 40, 40, 40, 40, 40, 35, 40}
    };

    private static final int[][] ROOK_TABLE = {
            {90, 90, 90, 90, 90, 90, 90, 90, 90}, /* ROOK */
            {90, 92, 91, 91, 90, 91, 91, 92, 90},
            {90, 91, 90, 90, 90, 90, 90, 91, 90},
            {90, 91, 90, 91, 90, 91, 90, 91, 90},
            {90, 93, 90, 91, 90, 91, 90, 93, 90},
            {90, 94, 90, 94, 90, 94, 90, 94, 90},
            {90, 91, 90, 91, 90, 91, 90, 91, 90},
            {90, 92, 90, 91, 90, 91, 90, 92, 90},
            {91, 92, 90, 93, 90, 93, 90, 92, 91},
            {89, 92, 90, 90, 90, 90, 90, 92, 89}
    };

    private static final int[][] CANNON_TABLE = {
            {50, 50, 50, 50, 50, 50, 50, 50, 50}, /* CANNON */
            {50, 51, 50, 50, 50, 50, 50, 51, 50},
            {50, 51, 50, 50, 50, 50, 50, 51, 50},
            {50, 51, 50, 50, 50, 50, 50, 51, 50},
            {50, 51, 50, 50, 50, 50, 50, 51, 50},
            {50, 51, 51, 51, 51, 51, 51, 51, 50},
            {50, 51, 50, 50, 50, 50, 50, 51, 50},
            {50, 51, 53, 53, 55, 53, 53, 51, 50},
            {50, 50, 50, 50, 50, 50, 50, 50, 50},
            {50, 50, 50, 50, 50, 50, 50, 50, 50}
    };

    private static final int[][] PAWN_TABLE = {
            {11, 12, 13, 14, 14, 14, 13, 12, 11}, /* PAWN*/
            {20, 21, 21, 23, 22, 23, 21, 21, 20},
            {20, 21, 21, 23, 23, 23, 21, 21, 20},
            {20, 21, 21, 22, 22, 22, 21, 21, 20},
            {20, 20, 20, 20, 20, 20, 20, 20, 20},
            {10, 0, 13, 0, 10, 0, 13, 0, 10},
            {10, 0, 12, 0, 15, 0, 12, 0, 10},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0}
    };

    /**
     * What every piece is worth on every square, flattened once at class load: the value, the
     * square and the side are all folded into one lookup, already signed the way {@link #score}
     * counts it - red positive. Reading a leaf's score is then one array access per occupied
     * square, where before it was a switch, a table lookup and a {@code Point}
     * allocated and thrown away for each of the ninety squares.
     */
    private static final int[][] PIECE_SQUARE = new int[PieceCode.COUNT][Board.SQUARES];

    static {
        table(PieceCode.KING, KING_TABLE);
        table(PieceCode.ADVISOR, BISHOP_TABLE);
        table(PieceCode.ELEPHANT, ELEPHANT_TABLE);
        table(PieceCode.KNIGHT, KNIGHT_TABLE);
        table(PieceCode.ROOK, ROOK_TABLE);
        table(PieceCode.CANNON, CANNON_TABLE);
        table(PieceCode.PAWN, PAWN_TABLE);
    }

    /**
     * Fills in one kind of piece for both sides. The tables are written from black's side of the
     * board, so red reads the same table upside down and mirrored, and counts the other way.
     */
    private static void table(int kind, int[][] values) {
        byte black = PieceCode.of(kind, false);
        byte red = PieceCode.of(kind, true);
        for (int x = 0; x < Board.ROW; x++) {
            for (int y = 0; y < Board.COL; y++) {
                int square = x * Board.COL + y;
                PIECE_SQUARE[black][square] = -values[x][y];
                PIECE_SQUARE[red][square] = values[Board.ROW - 1 - x][Board.COL - 1 - y];
            }
        }
    }

    /** Each side starts with this many advisors, and as many elephants. */
    private static final int GUARDS = 2;
    /** What a rook still boxed into its corner by its own knight costs its side. */
    private static final int UNDEVELOPED_ROOK = 5;

    /** Live pieces of each side, indexed by kind; the generals are not counted. */
    private final int[] countRed = new int[PieceCode.KINDS];
    private final int[] countBlack = new int[PieceCode.KINDS];

    /**
     * The score of the position on {@code board}, from {@code side}'s point of view. One pass
     * over the board does both halves of it: the piece-square total, and the census the two
     * bonuses below need. It used to be four passes - one to score, two to count, and the
     * development check - and this is the single most-run routine in the search, once at every
     * leaf the search settles on.
     */
    public int score(Board board, boolean side) {
        Arrays.fill(countRed, 0);
        Arrays.fill(countBlack, 0);
        int s = 0;
        byte[][] cell = board.cell;
        for (int x = 0; x < Board.ROW; x++) {
            byte[] row = cell[x];
            int square = x * Board.COL;
            for (int y = 0; y < Board.COL; y++) {
                byte v = row[y];
                if (v == PieceCode.EMPTY) continue;
                s += PIECE_SQUARE[v][square + y];
                // Kings are not counted: neither bonus asks after a piece that cannot be lost.
                int kind = PieceCode.kind(v);
                if (kind == PieceCode.KING) continue;
                if (PieceCode.isRed(v)) countRed[kind]++;
                else countBlack[kind]++;
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
        s += development(board, side);
        s -= development(board, !side);
        return s;
    }

    /** What {@code mine}'s attackers gain from the guards {@code theirs} has already lost. */
    private static int attackBonus(int[] mine, int[] theirs) {
        int s = 0;
        // Cannons and knights against few advisors, cannons and rooks against few elephants.
        if (theirs[PieceCode.ADVISOR] < GUARDS) {
            s += 2 * mine[PieceCode.CANNON] + mine[PieceCode.KNIGHT];
        }
        if (theirs[PieceCode.ELEPHANT] < GUARDS) {
            s += 2 * mine[PieceCode.CANNON] + mine[PieceCode.ROOK];
        }
        return s;
    }

    /** Penalty for a rook that has not left its starting corner, boxed in by its own knight. */
    private static int development(Board board, boolean side) {
        byte[] home = board.cell[Board.homeRow(side)];
        byte rook = PieceCode.of(PieceCode.ROOK, side);
        byte knight = PieceCode.of(PieceCode.KNIGHT, side);
        int last = Board.COL - 1;
        int s = 0;
        if (home[0] == rook && home[1] == knight) s -= UNDEVELOPED_ROOK;
        if (home[last] == rook && home[last - 1] == knight) s -= UNDEVELOPED_ROOK;
        return s;
    }
}
