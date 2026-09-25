package com.ttnt.chinesechess.chess;

import java.util.ArrayList;

/**
 * Where the pieces stand, whose turn it is, and what has happened so far - the position and
 * nothing else. What may be done from it, and what it amounts to, is {@link Rules}.
 */
public class Board {

    public final static int ROW = 10;
    public final static int COL = 9;
    /** How many squares the board has. */
    public final static int SQUARES = ROW * COL;

    /** The last row of Red's half; the river runs between it and the next. */
    public final static int RED_HALF_LAST_ROW = 4;
    /** The palace: the middle three columns of the three rows at each side's end. */
    public final static int PALACE_FIRST_COL = 3;
    public final static int PALACE_LAST_COL = 5;
    public final static int PALACE_ROWS = 3;

    /** The opening position. Red sets out from row 0, Black from the last row. */
    public final static byte[][] cellStartup = startingPosition();

    private static byte[][] startingPosition() {
        final int[] backRank = {PieceCode.ROOK, PieceCode.KNIGHT, PieceCode.ELEPHANT,
                PieceCode.ADVISOR, PieceCode.KING, PieceCode.ADVISOR, PieceCode.ELEPHANT,
                PieceCode.KNIGHT, PieceCode.ROOK};
        final int cannonRow = 2;
        final int pawnRow = 3;
        byte[][] cell = new byte[ROW][COL];
        for (boolean red : new boolean[]{true, false}) {
            int home = homeRow(red);
            int ahead = red ? 1 : -1;
            for (int y = 0; y < COL; y++) {
                cell[home][y] = PieceCode.of(backRank[y], red);
            }
            byte cannon = PieceCode.of(PieceCode.CANNON, red);
            cell[home + cannonRow * ahead][1] = cannon;
            cell[home + cannonRow * ahead][COL - 2] = cannon;
            for (int y = 0; y < COL; y += 2) {
                cell[home + pawnRow * ahead][y] = PieceCode.of(PieceCode.PAWN, red);
            }
        }
        return cell;
    }

    /** Whether {@code (x, y)} is on the board. */
    public static boolean inside(int x, int y) {
        return x >= 0 && x < ROW && y >= 0 && y < COL;
    }

    /** The row {@code red}'s side sets out from - red if {@code true}. */
    public static int homeRow(boolean red) {
        return red ? 0 : ROW - 1;
    }

    /** Whether row {@code x} is on {@code red}'s own side of the river. */
    public static boolean onOwnHalf(int x, boolean red) {
        return red ? x <= RED_HALF_LAST_ROW : x > RED_HALF_LAST_ROW;
    }

    /** Whether {@code (x, y)} is inside {@code red}'s palace. */
    public static boolean inPalace(int x, int y, boolean red) {
        if (y < PALACE_FIRST_COL || y > PALACE_LAST_COL) return false;
        return red ? x >= 0 && x < PALACE_ROWS : x >= ROW - PALACE_ROWS && x < ROW;
    }

    /**
     * Every position the game has stood in, oldest first; the last one is the position now. What
     * the record means for the game - a repetition, and who answers for it - is {@link Rules}.
     */
    public final ArrayList<MoveRecord> history = new ArrayList<>();

    public byte[][] cell;
    public Point currMove;
    public Point prevMove;
    public boolean select;
    public boolean move;
    /**
     * Whose turn it is. Named apart from the {@code side} the methods below take: those name the
     * colour a question is about, which is not always the colour to move. Throughout the class a
     * side is a boolean, and {@code true} is Red.
     */
    public boolean redToMove;
    public ArrayList<Move> listUndo;

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
        this.redToMove = side;
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
        // A copy is what the search works on; it carries the position but not the game record.
        this.redToMove = b.redToMove;
    }

    public void setBoard(byte[][] board) {
        cell = new byte[ROW][COL];
        for (int i = 0; i < ROW; i++) {
            System.arraycopy(board[i], 0, cell[i], 0, COL);
        }
    }

    /** The squares as they stand, row by row, for the record. */
    private byte[] snapshot() {
        byte[] squares = new byte[ROW * COL];
        for (int x = 0; x < ROW; x++) {
            System.arraycopy(cell[x], 0, squares, x * COL, COL);
        }
        return squares;
    }

    /**
     * Starts the record over at the position as it stands. Needed whenever
     * the squares are filled in from outside - restoring a saved game does exactly that, and the
     * record would otherwise describe a position that is no longer on the board.
     */
    public void resyncHistory() {
        restartHistory();
    }

    /** Starts the record over at the position as it stands. */
    private void restartHistory() {
        history.clear();
        history.add(new MoveRecord(snapshot(), !redToMove, false, false));
    }

    public byte getValue(int x, int y) {
        return inside(x, y) ? cell[x][y] : PieceCode.EMPTY;
    }

    /**
     * One piece object per kind, walked around the board by {@link Rules#collect} instead of a
     * fresh one built for every square of every position the search looks at. Each holds the
     * board it was made for, so they live here; made on first use, so a board the search never
     * touches never pays for them.
     */
    private Piece[] movers;

    Piece mover(byte value) {
        if (movers == null) {
            movers = new Piece[]{new CKing(this), new CBishop(this), new CElephant(this),
                    new CKnight(this), new CRook(this), new CCannon(this), new CPawn(this)};
        }
        return movers[PieceCode.kind(value)];
    }

    /** Somewhere for {@link Rules#hasLegalMove} to put the one move it is looking for. */
    final ArrayList<Move> firstFound = new ArrayList<>(1);

    public void select(int x, int y) {
        prevMove = new Point(x, y);
        select = true;
        move = false;
    }

    public void moveTo(int x, int y) {
        currMove = new Point(x, y);
        byte piece = getValue(prevMove.x, prevMove.y);
        byte captured = getValue(currMove.x, currMove.y);
        Move played = new Move(prevMove, currMove, piece, captured);
        listUndo.add(played);
        cell[x][y] = piece;
        cell[prevMove.x][prevMove.y] = 0;
        select = false;
        move = true;

        boolean mover = PieceCode.isRed(piece);
        history.add(new MoveRecord(snapshot(), mover,
                Rules.givesCheck(this, mover), Rules.isChasing(this, mover)));

        switchPlayer();
    }

    /** Takes the last move back off the record; the caller has already restored the squares. */
    public void undo() {
        if (history.size() > 1) {
            history.remove(history.size() - 1);
        }
    }

    public void switchPlayer() {
        redToMove = !redToMove;
    }
}
