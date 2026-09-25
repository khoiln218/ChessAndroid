package com.ttnt.chinesechess.chess;

import java.util.ArrayList;
import java.util.List;

/**
 * The rules of xiangqi, apart from the board they are played on. The board only holds where the
 * pieces stand and what has happened; this is what may be done there and what it amounts to:
 * <ul>
 *   <li>moving - where a piece may go, and that no move may leave one's own general attacked
 *       ({@link #kingSafe}, {@link #collect}, {@link #allMoves});</li>
 *   <li>the end of the game - no legal move loses ({@link #hasLost}), and a position that keeps
 *       coming back is answered for by whoever forced it ({@link #judgeRepetition}).</li>
 * </ul>
 * How each kind of piece moves is in its own class ({@link CKing}, {@link CRook}, ...).
 *
 * <p>Throughout, a side is a boolean, and {@code true} is Red.
 */
public final class Rules {

    /** How a repetition is judged. In xiangqi it is not automatically a draw. */
    public enum Repeat {
        NONE, DRAW, RED_LOSES, BLACK_LOSES
    }

    private Rules() {
    }

    // ===== Moving =====

    /** Whether the piece on {@code (x, y)} belongs to the side to move, so it may be picked up. */
    public static boolean canSelect(Board board, int x, int y) {
        byte value = board.getValue(x, y);
        return PieceCode.belongsTo(value, board.redToMove);
    }

    /** Whether the piece picked up may go to {@code (x, y)}. */
    public static boolean canMoveTo(Board board, int x, int y) {
        if (!board.select) return false;
        Piece piece = pieceAt(board, board.prevMove.x, board.prevMove.y);
        return piece != null && piece.checkMove(x, y);
    }

    /** The piece on {@code (x, y)}, knowing how it moves, or null for an empty square. */
    public static Piece pieceAt(Board board, int x, int y) {
        byte value = board.getValue(x, y);
        if (value == PieceCode.EMPTY) return null;
        Point at = new Point(x, y);
        return switch (PieceCode.kind(value)) {
            case PieceCode.KING -> new CKing(board, at);
            case PieceCode.ADVISOR -> new CBishop(board, at);
            case PieceCode.ELEPHANT -> new CElephant(board, at);
            case PieceCode.KNIGHT -> new CKnight(board, at);
            case PieceCode.ROOK -> new CRook(board, at);
            case PieceCode.CANNON -> new CCannon(board, at);
            case PieceCode.PAWN -> new CPawn(board, at);
            default -> null;
        };
    }

    /** Every legal move of the piece on {@code pos}. */
    public static ArrayList<Move> movesFrom(Board board, Point pos) {
        Piece piece = pieceAt(board, pos.x, pos.y);
        return piece == null ? new ArrayList<>() : piece.findAllPossibleMoves();
    }

    private final static int[] RAY_X = {1, -1, 0, 0};
    private final static int[] RAY_Y = {0, 0, 1, -1};
    private final static int[] HORSE_X = {1, 1, -1, -1, 2, 2, -2, -2};
    private final static int[] HORSE_Y = {2, -2, 2, -2, 1, -1, 1, -1};

    /**
     * Whether {@code side}'s general is safe where it stands - the test every generated move has
     * to pass, and by far the most-run code in the app.
     *
     * <p>Rather than asking each enemy piece in turn whether it reaches the general, this looks
     * outward from the general itself: four rays cover chariots, cannons and the two generals
     * facing each other down a file, then the eight squares a horse could stand on and the three
     * a soldier could. Advisors and elephants are skipped - neither can leave its own half of the
     * board, so neither can ever attack a general. Nothing is allocated on the way.
     */
    public static boolean kingSafe(Board board, boolean side) {
        byte[][] cell = board.cell;
        byte general = PieceCode.of(PieceCode.KING, side);
        int kx = -1;
        int ky = -1;
        // A general never leaves its palace, so nine squares are enough to find it.
        int firstRow = side ? 0 : Board.ROW - Board.PALACE_ROWS;
        for (int x = firstRow; x < firstRow + Board.PALACE_ROWS && kx < 0; x++) {
            for (int y = Board.PALACE_FIRST_COL; y <= Board.PALACE_LAST_COL; y++) {
                if (cell[x][y] == general) {
                    kx = x;
                    ky = y;
                    break;
                }
            }
        }
        if (kx < 0) return true;

        for (int d = 0; d < 4; d++) {
            int dx = RAY_X[d];
            int dy = RAY_Y[d];
            int x = kx + dx;
            int y = ky + dy;
            while (Board.inside(x, y) && cell[x][y] == PieceCode.EMPTY) {
                x += dx;
                y += dy;
            }
            if (!Board.inside(x, y)) continue;
            byte first = cell[x][y];
            if (isEnemy(first, side)) {
                int kind = kind(first);
                if (kind == PieceCode.ROOK) return false;
                // The generals may not face each other with nothing in between.
                if (kind == PieceCode.KING && dy == 0) return false;
            }
            // Past that screen, a cannon on the same line is firing over it.
            do {
                x += dx;
                y += dy;
            } while (Board.inside(x, y) && cell[x][y] == PieceCode.EMPTY);
            if (Board.inside(x, y) && isEnemy(cell[x][y], side)
                    && kind(cell[x][y]) == PieceCode.CANNON) {
                return false;
            }
        }

        for (int h = 0; h < HORSE_X.length; h++) {
            int x = kx + HORSE_X[h];
            int y = ky + HORSE_Y[h];
            if (!Board.inside(x, y)) continue;
            byte value = cell[x][y];
            if (!isEnemy(value, side) || kind(value) != PieceCode.KNIGHT) continue;
            // A horse is blocked by whatever stands beside it, on the long leg of its move.
            int lx = x;
            int ly = y;
            if (Math.abs(kx - x) == 1) ly += ky > y ? 1 : -1;
            else lx += kx > x ? 1 : -1;
            if (cell[lx][ly] == PieceCode.EMPTY) return false;
        }

        // A soldier attacks the square ahead of it, and to either side. Which way is "ahead"
        // depends on its colour: Red starts at the top of the board and moves down.
        byte above = board.getValue(kx - 1, ky);
        if (isEnemy(above, side) && kind(above) == PieceCode.PAWN && PieceCode.isRed(above)) {
            return false;
        }
        byte below = board.getValue(kx + 1, ky);
        if (isEnemy(below, side) && kind(below) == PieceCode.PAWN && !PieceCode.isRed(below)) {
            return false;
        }
        byte left = board.getValue(kx, ky - 1);
        if (isEnemy(left, side) && kind(left) == PieceCode.PAWN) return false;
        byte right = board.getValue(kx, ky + 1);
        return !isEnemy(right, side) || kind(right) != PieceCode.PAWN;
    }

    private static boolean isEnemy(byte value, boolean side) {
        return value != PieceCode.EMPTY && PieceCode.isRed(value) != side;
    }

    private static int kind(byte value) {
        return PieceCode.kind(value);
    }

    /**
     * Appends every move {@code side} has to {@code out}, in the order the board is read - down
     * the rows, left to right. The search leans on that order being fixed: it picks moves out of
     * the list by score, and moves of equal score are tried in the order they arrived here.
     *
     * @param capturesOnly leave out the quiet moves, for the quiescence search
     * @param legalOnly    keep only moves that leave one's own general safe. Off, the list is
     *                     pseudo-legal and the caller must retest each move it actually plays.
     */
    public static void collect(Board board, boolean side, boolean capturesOnly, boolean legalOnly,
                               ArrayList<Move> out) {
        for (int x = 0; x < Board.ROW; x++) {
            byte[] row = board.cell[x];
            for (int y = 0; y < Board.COL; y++) {
                byte value = row[y];
                if (!PieceCode.belongsTo(value, side)) continue;
                Piece piece = board.mover(value);
                piece.allPossibleMove = out;
                piece.capturesOnly = capturesOnly;
                piece.legalOnly = legalOnly;
                piece.at(x, y);
                piece.generate();
            }
        }
    }

    /**
     * Whether {@code side} has any legal move at all - the mate and stalemate test, both of which
     * xiangqi scores as a loss. It stops at the first one it finds, which is almost always on
     * the first piece it looks at, so it costs a fraction of listing every move to count them.
     */
    public static boolean hasLegalMove(Board board, boolean side) {
        ArrayList<Move> one = board.firstFound;
        one.clear();
        for (int x = 0; x < Board.ROW; x++) {
            byte[] row = board.cell[x];
            for (int y = 0; y < Board.COL; y++) {
                byte value = row[y];
                if (!PieceCode.belongsTo(value, side)) continue;
                Piece piece = board.mover(value);
                piece.allPossibleMove = one;
                piece.capturesOnly = false;
                piece.legalOnly = true;
                piece.at(x, y);
                piece.generate();
                if (!one.isEmpty()) return true;
            }
        }
        return false;
    }

    /** Every legal move {@code side} has. */
    public static ArrayList<Move> allMoves(Board board, boolean side) {
        ArrayList<Move> moves = new ArrayList<>();
        collect(board, side, false, true, moves);
        return moves;
    }

    // ===== The end of the game =====

    /**
     * Whether {@code side} has lost where the board stands. It has, if it has no legal move at
     * all: that is mate when its general is under attack and stalemate when it is not, and
     * xiangqi scores the two the same way.
     */
    public static boolean hasLost(Board board, boolean side) {
        return !hasLegalMove(board, side);
    }

    /** Whether the move {@code mover} just made leaves the other side's general attacked. */
    public static boolean givesCheck(Board board, boolean mover) {
        return !kingSafe(board, !mover);
    }

    /**
     * Whether {@code mover} is threatening to take an enemy piece that nothing defends. Chasing
     * one of those over and over is what the rule against endless harassment is about; a piece
     * that is defended is being offered a trade, not hunted.
     */
    public static boolean isChasing(Board board, boolean mover) {
        for (Move m : allMoves(board, mover)) {
            if (m.captured == PieceCode.EMPTY) continue;
            if (kind(m.captured) == PieceCode.KING) continue;
            if (!defended(board, m, mover)) return true;
        }
        return false;
    }

    /** Whether the other side could take back on the square {@code capture} lands on. */
    private static boolean defended(Board board, Move capture, boolean mover) {
        byte[][] cell = board.cell;
        byte from = cell[capture.from.x][capture.from.y];
        byte to = cell[capture.to.x][capture.to.y];
        cell[capture.to.x][capture.to.y] = from;
        cell[capture.from.x][capture.from.y] = PieceCode.EMPTY;

        boolean answered = false;
        for (Move reply : allMoves(board, !mover)) {
            if (reply.to.x == capture.to.x && reply.to.y == capture.to.y) {
                answered = true;
                break;
            }
        }

        cell[capture.from.x][capture.from.y] = from;
        cell[capture.to.x][capture.to.y] = to;
        return answered;
    }

    /**
     * Judges the last position of {@code history} against the record behind it.
     *
     * <p>A repeated position is not a draw in xiangqi the way it is in chess. Whoever forced the
     * repetition has to answer for it: a side that checked on every one of its moves through the
     * cycle is giving perpetual check and loses, and so does one that spent the whole cycle
     * hounding an undefended piece. If both sides were doing it, or neither was, it is a draw.
     */
    public static Repeat judgeRepetition(List<MoveRecord> history) {
        if (history.size() < 3) return Repeat.NONE;
        MoveRecord now = history.get(history.size() - 1);

        // The cycle runs back to the previous time this same position stood on the board.
        int seen = 0;
        int from = -1;
        for (int i = history.size() - 2; i >= 0; i--) {
            if (history.get(i).samePosition(now)) {
                seen++;
                from = i;
                if (seen == 2) break;
            }
        }
        // Twice before plus now is three times, which is when the position has to be answered for.
        if (seen < 2) return Repeat.NONE;

        int redMoves = 0;
        int redChecks = 0;
        int redChases = 0;
        int blackMoves = 0;
        int blackChecks = 0;
        int blackChases = 0;
        for (int i = from + 1; i < history.size(); i++) {
            MoveRecord record = history.get(i);
            if (record.redMoved) {
                redMoves++;
                if (record.gaveCheck) redChecks++;
                if (record.chasing) redChases++;
            } else {
                blackMoves++;
                if (record.gaveCheck) blackChecks++;
                if (record.chasing) blackChases++;
            }
        }
        // "Perpetual" means every move of the cycle, not merely one of them.
        boolean redGuilty = redMoves > 0 && (redChecks == redMoves || redChases == redMoves);
        boolean blackGuilty = blackMoves > 0
                && (blackChecks == blackMoves || blackChases == blackMoves);

        if (redGuilty && !blackGuilty) return Repeat.RED_LOSES;
        if (blackGuilty && !redGuilty) return Repeat.BLACK_LOSES;
        return Repeat.DRAW;
    }
}
