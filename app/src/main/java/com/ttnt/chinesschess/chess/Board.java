package com.ttnt.chinesschess.chess;

import android.graphics.Point;

import java.util.ArrayList;

public final class Board {

    public final static int ROW = 10;
    public final static int COL = 9;

    public final static byte[][] cellStartup = {
            {19, 18, 17, 16, 15, 16, 17, 18, 19},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 20, 0, 0, 0, 0, 0, 20, 0},
            {21, 0, 21, 0, 21, 0, 21, 0, 21},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {14, 0, 14, 0, 14, 0, 14, 0, 14},
            {0, 13, 0, 0, 0, 0, 0, 13, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 0},
            {12, 11, 10, 9, 8, 9, 10, 11, 12}};

    /**
     * One random number per piece per square, plus one for the side to move. Xor-ing them
     * together identifies a position; xor-ing a piece out of one square and into another updates
     * that identity in a few operations instead of re-reading the whole board. The search uses
     * them for its transposition table, the game for spotting a position it has seen before.
     */
    public final static long[][] ZOBRIST = new long[90][22];
    public final static long ZOBRIST_SIDE;

    static {
        // Fixed seed: the same game always hashes the same way, which makes a bug reproducible.
        java.util.Random rand = new java.util.Random(0x9E3779B97F4A7C15L);
        for (int square = 0; square < 90; square++) {
            for (int piece = 0; piece < 22; piece++) {
                ZOBRIST[square][piece] = rand.nextLong();
            }
        }
        ZOBRIST_SIDE = rand.nextLong();
    }

    /**
     * A move already played, and what it did. Kept for every move of the game, because judging a
     * repetition means looking back at how the position was reached, not just that it repeated.
     */
    public static final class Ply {
        /** The position after the move, whose turn it is included. */
        public final long key;
        public final boolean redMoved;
        /** Whether the move left the other side in check. */
        public final boolean gaveCheck;
        /** Whether it threatened to take an undefended enemy piece other than the general. */
        public final boolean chasing;

        Ply(long key, boolean redMoved, boolean gaveCheck, boolean chasing) {
            this.key = key;
            this.redMoved = redMoved;
            this.gaveCheck = gaveCheck;
            this.chasing = chasing;
        }
    }

    /** How a repetition is judged. In xiangqi it is not automatically a draw. */
    public enum Repeat {
        NONE, DRAW, RED_LOSES, BLACK_LOSES
    }

    /** Every position the game has stood in, oldest first; the last one is the position now. */
    public final ArrayList<Ply> history = new ArrayList<>();
    /** Key of the position as it stands, kept in step by {@link #moveTo} and {@link #undo}. */
    public long key;

    public byte[][] cell;
    public Point currMove;
    public Point prevMove;
    public boolean select;
    public boolean move;
    public boolean RED;
    public ArrayList<State> listUndo;

    public Board(boolean side) {
        listUndo = new ArrayList<>();
        reset(side);
    }

    /** Back to the opening position, in place, so everything holding this board stays valid. */
    public void reset(boolean side) {
        listUndo.clear();
        setBoard(cellStartup);
        currMove = new Point(-1, -1);
        prevMove = new Point(-1, -1);
        this.select = false;
        this.move = false;
        this.RED = side;
        restartHistory();
    }

    /**
     * {@link #movers} is deliberately left out: each of those pieces holds the board it was made
     * for, so handing them to a copy would have them generating moves against the original. The
     * copy makes its own on first use.
     */
    @SuppressWarnings("CopyConstructorMissesField")
    public Board(Board b) {
        listUndo = new ArrayList<>();
        setBoard(b.cell);
        this.currMove = new Point(b.currMove);
        this.prevMove = new Point(b.prevMove);
        this.select = b.select;
        this.move = b.move;
        this.RED = b.RED;
        // A copy is what the search works on; it carries the position but not the game record.
        key = computeKey();
    }

    public void setBoard(byte[][] board) {
        cell = new byte[ROW][COL];
        for (int i = 0; i < ROW; i++) {
            System.arraycopy(board[i], 0, cell[i], 0, COL);
        }
    }

    /** Reads the key of the position as it stands, the slow way - only when the board is set. */
    public long computeKey() {
        long k = RED ? ZOBRIST_SIDE : 0L;
        for (int x = 0; x < ROW; x++) {
            for (int y = 0; y < COL; y++) {
                byte v = cell[x][y];
                if (v != 0) k ^= ZOBRIST[x * COL + y][v];
            }
        }
        return k;
    }

    /**
     * Re-reads the key and starts the record over at the position as it stands. Needed whenever
     * the squares are filled in from outside - restoring a saved game does exactly that, and the
     * record would otherwise describe a position that is no longer on the board.
     */
    public void resyncHistory() {
        restartHistory();
    }

    /** Starts the record over at the position as it stands. */
    private void restartHistory() {
        key = computeKey();
        history.clear();
        history.add(new Ply(key, !RED, false, false));
    }

    public byte getValue(int x, int y) {
        if (x < 0 || x >= ROW || y < 0 || y >= COL)
            return 0;
        return cell[x][y];
    }

    public boolean isCheckSelect(int x, int y) {
        byte value = getValue(x, y);
        if (RED) return value > 14;
        else return value >= 8 && value <= 14;
    }

    public boolean isCheckMove(int x, int y) {
        if (!select) return false;
        Piece piece = getPiece(prevMove.x, prevMove.y);
        return piece != null && piece.checkMove(x, y);
    }

    public Piece getPiece(int x, int y) {
        Piece piece;
        byte value = getValue(x, y);
        piece = switch (value) {
            case 8, 15 -> new CKing(this, new Point(x, y));
            case 9, 16 -> new CBishop(this, new Point(x, y));
            case 10, 17 -> new CElephant(this, new Point(x, y));
            case 11, 18 -> new CKnight(this, new Point(x, y));
            case 12, 19 -> new CRook(this, new Point(x, y));
            case 13, 20 -> new CCannon(this, new Point(x, y));
            case 14, 21 -> new CPawn(this, new Point(x, y));
            default -> null;
        };
        return piece;
    }

    /** Piece kinds, as the offset of a value within its own side's block. */
    private final static int KIND_KING = 0;
    private final static int KIND_KNIGHT = 3;
    private final static int KIND_ROOK = 4;
    private final static int KIND_CANNON = 5;
    private final static int KIND_PAWN = 6;

    private final static int[] RAY_X = {1, -1, 0, 0};
    private final static int[] RAY_Y = {0, 0, 1, -1};
    private final static int[] HORSE_X = {1, 1, -1, -1, 2, 2, -2, -2};
    private final static int[] HORSE_Y = {2, -2, 2, -2, 1, -1, 1, -1};

    /**
     * Whether {@code red}'s general is safe where it stands - the test every generated move has
     * to pass, and by far the most-run code in the app.
     *
     * <p>Rather than asking each enemy piece in turn whether it reaches the general, this looks
     * outward from the general itself: four rays cover chariots, cannons and the two generals
     * facing each other down a file, then the eight squares a horse could stand on and the three
     * a soldier could. Advisors and elephants are skipped - neither can leave its own half of the
     * board, so neither can ever attack a general. Nothing is allocated on the way.
     */
    public boolean kingSafe(boolean red) {
        byte general = red ? (byte) 15 : (byte) 8;
        int kx = -1;
        int ky = -1;
        // A general never leaves its palace, so nine squares are enough to find it.
        int firstRow = red ? 0 : 7;
        for (int x = firstRow; x < firstRow + 3 && kx < 0; x++) {
            for (int y = 3; y <= 5; y++) {
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
            while (inside(x, y) && cell[x][y] == 0) {
                x += dx;
                y += dy;
            }
            if (!inside(x, y)) continue;
            byte first = cell[x][y];
            if (isEnemy(first, red)) {
                int kind = kind(first);
                if (kind == KIND_ROOK) return false;
                // The generals may not face each other with nothing in between.
                if (kind == KIND_KING && dy == 0) return false;
            }
            // Past that screen, a cannon on the same line is firing over it.
            do {
                x += dx;
                y += dy;
            } while (inside(x, y) && cell[x][y] == 0);
            if (inside(x, y) && isEnemy(cell[x][y], red) && kind(cell[x][y]) == KIND_CANNON) {
                return false;
            }
        }

        for (int h = 0; h < 8; h++) {
            int x = kx + HORSE_X[h];
            int y = ky + HORSE_Y[h];
            if (!inside(x, y)) continue;
            byte value = cell[x][y];
            if (!isEnemy(value, red) || kind(value) != KIND_KNIGHT) continue;
            // A horse is blocked by whatever stands beside it, on the long leg of its move.
            int lx = x;
            int ly = y;
            if (Math.abs(kx - x) == 1) ly += ky > y ? 1 : -1;
            else lx += kx > x ? 1 : -1;
            if (cell[lx][ly] == 0) return false;
        }

        // A soldier attacks the square ahead of it, and to either side. Which way is "ahead"
        // depends on its colour: red starts at the top of the board and moves down.
        byte above = getValue(kx - 1, ky);
        if (isEnemy(above, red) && kind(above) == KIND_PAWN && above > 14) return false;
        byte below = getValue(kx + 1, ky);
        if (isEnemy(below, red) && kind(below) == KIND_PAWN && below <= 14) return false;
        byte left = getValue(kx, ky - 1);
        if (isEnemy(left, red) && kind(left) == KIND_PAWN) return false;
        byte right = getValue(kx, ky + 1);
        return !isEnemy(right, red) || kind(right) != KIND_PAWN;
    }

    private static boolean inside(int x, int y) {
        return x >= 0 && x < ROW && y >= 0 && y < COL;
    }

    private static boolean isEnemy(byte value, boolean red) {
        return value != 0 && (value > 14) != red;
    }

    private static int kind(byte value) {
        return value > 14 ? value - 15 : value - 8;
    }

    public ArrayList<Point> findPieces(boolean _RED) {
        ArrayList<Point> allPiece = new ArrayList<>();
        allPiece.add(0, new Point(-1, -1));
        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COL; j++) {
                byte val = cell[i][j];
                if (val >= 8 && val <= 21) {
                    if ((_RED && val == 15) || (!_RED && val == 8)) {
                        allPiece.remove(0);
                        allPiece.add(0, new Point(i, j));
                    }
                    if ((_RED && val <= 14) || (!_RED && val > 14)) {
                        allPiece.add(new Point(i, j));
                    }
                }
            }
        }
        return allPiece;
    }

    public ArrayList<State> allMove(Point pos) {
        Piece piece = getPiece(pos.x, pos.y);
        return piece == null ? new ArrayList<>() : piece.findAllPossibleMoves();
    }

    /**
     * One piece object per kind, walked around the board by {@link #collect} instead of a fresh
     * one built for every square of every position the search looks at. Made on first use, so a
     * board the search never touches never pays for them.
     */
    private Piece[] movers;

    private Piece mover(byte value) {
        if (movers == null) {
            movers = new Piece[]{new CKing(this), new CBishop(this), new CElephant(this),
                    new CKnight(this), new CRook(this), new CCannon(this), new CPawn(this)};
        }
        return movers[kind(value)];
    }

    /**
     * Appends every move {@code red} has to {@code out}, in the order the board is read - down
     * the rows, left to right. The search leans on that order being fixed: it picks moves out of
     * the list by score, and moves of equal score are tried in the order they arrived here.
     *
     * @param capturesOnly leave out the quiet moves, for the quiescence search
     * @param legalOnly    keep only moves that leave one's own general safe. Off, the list is
     *                     pseudo-legal and the caller must retest each move it actually plays.
     */
    void collect(boolean red, boolean capturesOnly, boolean legalOnly, ArrayList<State> out) {
        for (int x = 0; x < ROW; x++) {
            byte[] row = cell[x];
            for (int y = 0; y < COL; y++) {
                byte value = row[y];
                if (value < 8 || (value > 14) != red) continue;
                Piece piece = mover(value);
                piece.allPossibleMove = out;
                piece.capturesOnly = capturesOnly;
                piece.legalOnly = legalOnly;
                piece.at(x, y);
                piece.generate();
            }
        }
    }

    /** Somewhere for {@link #hasLegalMove} to put the one move it is looking for. */
    private final ArrayList<State> firstFound = new ArrayList<>(1);

    /**
     * Whether {@code red} has any legal move at all - the mate and stalemate test, both of which
     * xiangqi scores as a loss. It stops at the first one it finds, which is almost always on
     * the first piece it looks at, so it costs a fraction of listing every move to count them.
     */
    boolean hasLegalMove(boolean red) {
        ArrayList<State> one = firstFound;
        one.clear();
        for (int x = 0; x < ROW; x++) {
            byte[] row = cell[x];
            for (int y = 0; y < COL; y++) {
                byte value = row[y];
                if (value < 8 || (value > 14) != red) continue;
                Piece piece = mover(value);
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

    /** Every legal move of the side {@code _RED} is <em>not</em> playing. */
    public ArrayList<State> allMoves(boolean _RED) {
        ArrayList<State> arrMoves = new ArrayList<>();
        collect(!_RED, false, true, arrMoves);
        return arrMoves;
    }

    public boolean isGameOver(boolean _RED) {
        ArrayList<Point> allPiece = findPieces(_RED);
        for (int i = 1; i < allPiece.size(); i++) {
            Point pos = allPiece.get(i);
            ArrayList<State> arrMoves;
            byte val = cell[pos.x][pos.y];
            arrMoves = switch (val) {
                case 8, 15 -> {
                    CKing king = new CKing(this, pos);
                    yield king.findAllPossibleMoves();
                }
                case 9, 16 -> {
                    CBishop bishop = new CBishop(this, pos);
                    yield bishop.findAllPossibleMoves();
                }
                case 10, 17 -> {
                    CElephant elephant = new CElephant(this, pos);
                    yield elephant.findAllPossibleMoves();
                }
                case 11, 18 -> {
                    CKnight knight = new CKnight(this, pos);
                    yield knight.findAllPossibleMoves();
                }
                case 12, 19 -> {
                    CRook rook = new CRook(this, pos);
                    yield rook.findAllPossibleMoves();
                }
                case 13, 20 -> {
                    CCannon cannon = new CCannon(this, pos);
                    yield cannon.findAllPossibleMoves();
                }
                case 14, 21 -> {
                    CPawn pawn = new CPawn(this, pos);
                    yield pawn.findAllPossibleMoves();
                }
                default -> null;
            };
            if (arrMoves != null && !arrMoves.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public void select(int x, int y) {
        prevMove = new Point(x, y);
        select = true;
        move = false;
    }

    public void moveTo(int x, int y) {
        currMove = new Point(x, y);
        byte val1 = getValue(prevMove.x, prevMove.y);
        byte val2 = getValue(currMove.x, currMove.y);
        State played = new State(prevMove, currMove, val1, val2);
        listUndo.add(played);
        cell[x][y] = val1;
        cell[prevMove.x][prevMove.y] = 0;
        select = false;
        move = true;

        boolean mover = val1 > 14;
        key ^= ZOBRIST[played.prev.x * COL + played.prev.y][val1];
        key ^= ZOBRIST[played.curr.x * COL + played.curr.y][val1];
        if (val2 != 0) key ^= ZOBRIST[played.curr.x * COL + played.curr.y][val2];
        key ^= ZOBRIST_SIDE;
        history.add(new Ply(key, mover, !kingSafe(!mover), isChasing(mover)));

        switchPlayer();
    }

    /**
     * Judges the position as it stands against the record behind it.
     *
     * <p>A repeated position is not a draw in xiangqi the way it is in chess. Whoever forced the
     * repetition has to answer for it: a side that checked on every one of its moves through the
     * cycle is giving perpetual check and loses, and so does one that spent the whole cycle
     * hounding an undefended piece. If both sides were doing it, or neither was, it is a draw.
     */
    public Repeat judgeRepetition() {
        if (history.size() < 3) return Repeat.NONE;
        long now = history.get(history.size() - 1).key;

        // The cycle runs back to the previous time this same position stood on the board.
        int seen = 0;
        int from = -1;
        for (int i = history.size() - 2; i >= 0; i--) {
            if (history.get(i).key == now) {
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
            Ply ply = history.get(i);
            if (ply.redMoved) {
                redMoves++;
                if (ply.gaveCheck) redChecks++;
                if (ply.chasing) redChases++;
            } else {
                blackMoves++;
                if (ply.gaveCheck) blackChecks++;
                if (ply.chasing) blackChases++;
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

    /** Takes the last move back off the record; the caller has already restored the squares. */
    public void undo() {
        if (history.size() > 1) {
            history.remove(history.size() - 1);
            key = history.get(history.size() - 1).key;
        }
    }

    /**
     * Whether {@code mover} is threatening to take an enemy piece that nothing defends. Chasing
     * one of those over and over is what the rule against endless harassment is about; a piece
     * that is defended is being offered a trade, not hunted.
     */
    public boolean isChasing(boolean mover) {
        ArrayList<State> moves = allMoves(!mover);
        for (State m : moves) {
            if (m.value2 == 0) continue;
            if (kind(m.value2) == KIND_KING) continue;
            if (!defended(m, mover)) return true;
        }
        return false;
    }

    /** Whether the other side could take back on the square {@code capture} lands on. */
    private boolean defended(State capture, boolean mover) {
        byte from = cell[capture.prev.x][capture.prev.y];
        byte to = cell[capture.curr.x][capture.curr.y];
        cell[capture.curr.x][capture.curr.y] = from;
        cell[capture.prev.x][capture.prev.y] = 0;

        boolean answered = false;
        for (State reply : allMoves(mover)) {
            if (reply.curr.x == capture.curr.x && reply.curr.y == capture.curr.y) {
                answered = true;
                break;
            }
        }

        cell[capture.prev.x][capture.prev.y] = from;
        cell[capture.curr.x][capture.curr.y] = to;
        return answered;
    }

    public void switchPlayer() {
        RED = !RED;
    }
}
