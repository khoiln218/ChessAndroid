package com.ttnt.chinesechess.theme;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

import com.ttnt.chinesechess.R;
import com.ttnt.chinesechess.chess.Move;
import com.ttnt.chinesechess.chess.PieceCode;
import com.ttnt.chinesechess.chess.Point;

import java.util.ArrayList;

/**
 * Draws the board and the pieces. Both are drawn rather than loaded - the pieces by
 * {@link PieceArt}, the board here - wood, frame, grid, palaces, river and position marks - and painted once into
 * {@link #boardCache} whenever the size changes, so a repaint is a single blit.
 *
 * <p>Drawing it rather than scaling a picture of it is what lets the margin be chosen freely: the
 * frame is placed one disc radius outside the grid, so it runs flush with the edge of a piece
 * standing on an edge file rather than under it. The colours and line weights below are the ones the old
 * board artwork was drawn with - the {@link BoardTheme#MOC} palette - so the wooden board still
 * looks like itself, and the other four materials are palettes of the same shape.
 */
public class Graphics {

    public static int CELL_SIZE;
    /**
     * Half the width a piece is drawn at, in cells. Only {@link #PIECE_ART} of that box is the
     * disc itself, the rest being transparent padding, so the box runs wider than a cell and two
     * pieces on adjacent intersections still clear each other - up to 0.68, where the discs meet.
     */
    private final static float PIECE_RATIO = 0.64f;
    /** Fraction of the piece artwork the disc fills - its 500pt disc in a 680pt design. */
    static final float PIECE_ART = 500f / 680f;
    /** Fraction of its own artwork each marker fills, measured off the four images. */
    private final static float SELECT_ART = 0.956f;
    private final static float CAPTURE_ART = 0.656f;
    private final static float DOT_ART = 0.367f;
    private final static float LAST_ART = 0.889f;
    /**
     * How wide each marker is drawn, as a fraction of the piece disc. At 1 a marker lands exactly
     * on the rim of a piece - and, on an edge file, exactly on the frame - so 1 is where a marker
     * has to stop if it must never touch the frame. The selection ring is deliberately past that:
     * it reads as a halo around the piece rather than an outline on it, and the price is that on
     * the four edge lines it crosses the frame while that piece is held.
     *
     * <p>The last-move square may sit at 1 and still be seen under a piece, because its corners
     * reach past a round disc; a round marker at 1 would be hidden by the piece instead, which is
     * why the ring is the one thing drawn over the pieces rather than under them.
     */
    private final static float SELECT_SPAN = 1.13f;
    private final static float CAPTURE_SPAN = 1.10f;
    private final static float DOT_SPAN = 0.42f;
    private final static float LAST_SPAN = 1.00f;
    /** Wood kept outside the frame line, in cells: how far the board runs past its own frame. */
    private final static float OUTER_RATIO = 0.18f;
    /** Margin between the outer edge of the board and the first line, in pixels. */
    static int BORDER;
    public final static int ROW = 10;
    public final static int COL = 9;
    public static int LEFT;
    public static int UP;
    /** Half the width a piece is drawn at. */
    static int SIZE;
    /** Half the width of each marker's box; the mark inside it is narrower by its own ART. */
    static int SELECT_SIZE;
    static int CAPTURE_SIZE;
    static int DOT_SIZE;
    static int LAST_SIZE;
    /** The widest of the four, which is all a repaint has to allow room for. */
    static int MARK_SIZE;

    /** Line weights and the position-mark bracket, all in cells. */
    private final static float GRID_STROKE = 4f / 144f;
    private final static float FRAME_STROKE = 7f / 144f;
    private final static float FRAME_CORNER = 12f / 144f;
    /** How far the bracket sits from its intersection, and how long each of its two arms is. */
    private final static float BRACKET_GAP = 10.5f / 144f;
    private final static float BRACKET_ARM = 12f / 144f;
    /** Text size of the river characters, and how far down the band they are centred. */
    private final static float RIVER_TEXT = 0.62f;

    /** Files carrying a cannon's position mark, and the row each side's cannons start on. */
    private final static int[] CANNON_COL = {1, 7};
    private final static int[] CANNON_ROW = {2, 7};
    private final static int[] PAWN_ROW = {3, 6};

    /** Kept so the pieces can be redrawn when the size or the theme changes. */
    private final Resources res;
    /**
     * Indexed by {@code cell value - PieceCode.BLACK_KING}: black general..soldier, then red
     * general..soldier.
     */
    private Bitmap[] pieces;
    private final Bitmap imageSelected;
    private final Bitmap imageMoveDot;
    private final Bitmap imageCapture;
    private final Bitmap imageLastMove;
    /** The four river characters, one per glyph, in the order they are written. */
    private final String[] riverWords;
    /** Plain bitmap paint, used for the markers - those are the same whatever the theme is. */
    private final Paint paint;

    /** The palette the board is painted with; the pieces are the same whichever it is. */
    private BoardTheme theme;
    /** The whole board, painted once per size change: repainting it is one blit. */
    private Bitmap boardCache;
    private final Rect panelBounds = new Rect();
    private final Rect cellRect = new Rect();
    private final Rect clipBounds = new Rect();

    public Graphics(Resources res, BoardTheme theme) {
        this.res = res;
        this.theme = theme;
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFilterBitmap(true);
        imageSelected = BitmapFactory.decodeResource(res, R.drawable.hl_selected);
        imageMoveDot = BitmapFactory.decodeResource(res, R.drawable.hl_move_dot);
        imageCapture = BitmapFactory.decodeResource(res, R.drawable.hl_capture);
        imageLastMove = BitmapFactory.decodeResource(res, R.drawable.hl_last_move);
        String words = res.getString(R.string.river_words);
        riverWords = new String[words.length()];
        for (int i = 0; i < riverWords.length; i++) {
            riverWords[i] = String.valueOf(words.charAt(i));
        }
    }

    /**
     * Repaints the board and recolours the pieces for another theme. Nothing but the colours
     * changes - not the artwork, not a single measurement - so there is no re-measure.
     */
    public void setTheme(BoardTheme theme) {
        if (this.theme == theme) return;
        this.theme = theme;
        renderPieces();
        if (boardCache != null) {
            renderBoard(new Canvas(boardCache), boardCache.getWidth(), boardCache.getHeight());
        }
    }

    /** Redraws the fourteen pieces at the current size, in the current theme's colours. */
    private void renderPieces() {
        if (CELL_SIZE <= 0) return;
        if (pieces != null) {
            for (Bitmap piece : pieces) piece.recycle();
        }
        pieces = PieceArt.renderAll(res, theme, SIZE * 2);
    }

    /** Radius of the disc a piece actually shows, in cells: its box less the padding around it. */
    private static float discCells() {
        return PIECE_RATIO * PIECE_ART;
    }

    /**
     * Margin from the edge of the board to the first line of the grid, in cells. The wood between
     * the grid and the frame is exactly one disc wide, so the frame runs flush with the edge of a
     * piece standing on an edge file - not under it, and with no gap to spare either.
     */
    private static float borderCells() {
        return OUTER_RATIO + FRAME_STROKE + discCells();
    }

    /** The board box measured in cells: the grid plus the margin kept on each side. */
    public static float boxCellsWide() {
        return (COL - 1) + 2 * borderCells();
    }

    public static float boxCellsHigh() {
        return (ROW - 1) + 2 * borderCells();
    }

    /**
     * Fits the board inside a view of {@code width} x {@code height} pixels and centres it on both
     * axes: the cell size is limited by whichever axis is tighter, then the leftover space is split
     * evenly on each side.
     */
    public void setSize(int width, int height) {
        if (width <= 0 || height <= 0) return;
        CELL_SIZE = (int) Math.min(width / boxCellsWide(), height / boxCellsHigh());
        BORDER = Math.round(CELL_SIZE * borderCells());
        SIZE = Math.round(CELL_SIZE * PIECE_RATIO);
        // Sized off the disc rather than off the piece box: it is the mark inside each artwork
        // that has to land on the rim of the piece, and stay inside the frame with it.
        SELECT_SIZE = markSize(SELECT_SPAN, SELECT_ART);
        CAPTURE_SIZE = markSize(CAPTURE_SPAN, CAPTURE_ART);
        DOT_SIZE = markSize(DOT_SPAN, DOT_ART);
        LAST_SIZE = markSize(LAST_SPAN, LAST_ART);
        MARK_SIZE = Math.max(Math.max(SELECT_SIZE, CAPTURE_SIZE), Math.max(DOT_SIZE, LAST_SIZE));

        int boardWidth = (COL - 1) * CELL_SIZE + 2 * BORDER;
        int boardHeight = (ROW - 1) * CELL_SIZE + 2 * BORDER;
        int left = (width - boardWidth) / 2;
        int top = (height - boardHeight) / 2;

        LEFT = left + BORDER;
        UP = top + BORDER;
        panelBounds.set(left, top, left + boardWidth, top + boardHeight);

        renderPieces();

        if (boardCache != null) boardCache.recycle();
        boardCache = Bitmap.createBitmap(boardWidth, boardHeight, Bitmap.Config.ARGB_8888);
        renderBoard(new Canvas(boardCache), boardWidth, boardHeight);
    }

    /** Half-width of a marker's box: its artwork scaled so the mark inside spans {@code span}. */
    private static int markSize(float span, float art) {
        return Math.round(CELL_SIZE * discCells() * span / art);
    }

    /** Paints the board into {@code canvas}, whose origin is the top left of the board itself. */
    private void renderBoard(Canvas canvas, int width, int height) {
        // A theme change paints over a board that is already there, so clear it first: the round
        // corners would otherwise keep a rim of the palette being replaced.
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        Paint wood = new Paint(Paint.ANTI_ALIAS_FLAG);
        wood.setShader(new LinearGradient(0, 0, width, height,
                theme.woodLight, theme.woodDark, Shader.TileMode.CLAMP));
        float panelCorner = BORDER * 0.6f;
        canvas.drawRoundRect(new RectF(0, 0, width, height), panelCorner, panelCorner, wood);

        // The border stands proud of the field: lit along the top left edge of the board, in
        // shadow along the bottom right, which is what makes it read as a raised rim.
        float bevel = BORDER * 0.30f;
        Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeWidth(bevel);
        edge.setShader(new LinearGradient(0, 0, width, height,
                0x70FFFFFF, 0x66000000, Shader.TileMode.CLAMP));
        float bevelCorner = Math.max(panelCorner - bevel / 2f, 0f);
        canvas.drawRoundRect(new RectF(bevel / 2f, bevel / 2f, width - bevel / 2f,
                height - bevel / 2f), bevelCorner, bevelCorner, edge);

        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setColor(theme.line);
        line.setStyle(Paint.Style.STROKE);

        // The frame, its stroke sitting just outside the wood that separates it from the grid.
        float frameStroke = FRAME_STROKE * CELL_SIZE;
        float inset = discCells() * CELL_SIZE + frameStroke / 2f;
        float corner = FRAME_CORNER * CELL_SIZE;
        RectF frame = new RectF(x(0) - inset, y(0) - inset, x(COL - 1) + inset,
                y(ROW - 1) + inset);
        line.setStrokeWidth(frameStroke);
        canvas.drawRoundRect(frame, corner, corner, line);

        // The field is sunk below that rim, so the border casts onto it all the way round.
        canvas.save();
        Path field = new Path();
        field.addRoundRect(frame, corner, corner, Path.Direction.CW);
        canvas.clipPath(field);
        Paint recess = new Paint(Paint.ANTI_ALIAS_FLAG);
        recess.setStyle(Paint.Style.STROKE);
        recess.setStrokeWidth(CELL_SIZE * 0.30f);
        recess.setColor(0x38000000);
        recess.setMaskFilter(new BlurMaskFilter(CELL_SIZE * 0.16f, BlurMaskFilter.Blur.NORMAL));
        canvas.drawRoundRect(frame, corner, corner, recess);
        canvas.restore();

        line.setStrokeWidth(GRID_STROKE * CELL_SIZE);
        for (int r = 0; r < ROW; r++) {
            canvas.drawLine(x(0), y(r), x(COL - 1), y(r), line);
        }
        // Only the two outer files cross the river; the rest stop on either bank.
        canvas.drawLine(x(0), y(0), x(0), y(ROW - 1), line);
        canvas.drawLine(x(COL - 1), y(0), x(COL - 1), y(ROW - 1), line);
        for (int c = 1; c < COL - 1; c++) {
            canvas.drawLine(x(c), y(0), x(c), y(4), line);
            canvas.drawLine(x(c), y(5), x(c), y(ROW - 1), line);
        }

        for (int r : new int[]{0, 7}) {
            canvas.drawLine(x(3), y(r), x(5), y(r + 2), line);
            canvas.drawLine(x(5), y(r), x(3), y(r + 2), line);
        }

        for (int r : CANNON_ROW) {
            for (int c : CANNON_COL) drawBracket(canvas, line, r, c);
        }
        for (int r : PAWN_ROW) {
            for (int c = 0; c < COL; c += 2) drawBracket(canvas, line, r, c);
        }

        drawRiver(canvas);
    }

    /**
     * The four corner brackets that mark where the cannons and pawns start. On the two edge files
     * the outward pair is dropped - there is no board on that side to draw it on.
     */
    private void drawBracket(Canvas canvas, Paint paint, int row, int col) {
        float gap = BRACKET_GAP * CELL_SIZE;
        float arm = BRACKET_ARM * CELL_SIZE;
        for (int sx = -1; sx <= 1; sx += 2) {
            if (sx < 0 && col == 0) continue;
            if (sx > 0 && col == COL - 1) continue;
            for (int sy = -1; sy <= 1; sy += 2) {
                float cx = x(col) + sx * gap;
                float cy = y(row) + sy * gap;
                canvas.drawLine(cx, cy, cx + sx * arm, cy, paint);
                canvas.drawLine(cx, cy, cx, cy + sy * arm, paint);
            }
        }
    }

    /** "Chu river, Han border" written across the middle band, a character to a file. */
    private void drawRiver(Canvas canvas) {
        if (riverWords.length < 4) return;
        Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        text.setColor(theme.river);
        text.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
        text.setTextSize(RIVER_TEXT * CELL_SIZE);
        text.setTextAlign(Paint.Align.CENTER);
        float baseline = (y(4) + y(5)) / 2f - (text.descent() + text.ascent()) / 2f;
        int[] on = {1, 2, 6, 7};
        for (int i = 0; i < on.length; i++) {
            canvas.drawText(riverWords[i], x(on[i]), baseline, text);
        }
    }

    /** Board-local pixel of a file, then of a rank: the cache has its own origin. */
    private float x(int col) {
        return BORDER + col * CELL_SIZE;
    }

    private float y(int row) {
        return BORDER + row * CELL_SIZE;
    }

    /** The area a slide from {@code from} to {@code to} can touch, markers and swell included. */
    public void moveBounds(Point from, Point to, Rect out) {
        int pad = Math.max(Math.round(SIZE * 1.2f), MARK_SIZE) + 2;
        int x1 = from.y * CELL_SIZE + LEFT;
        int y1 = from.x * CELL_SIZE + UP;
        int x2 = to.y * CELL_SIZE + LEFT;
        int y2 = to.x * CELL_SIZE + UP;
        out.set(Math.min(x1, x2) - pad, Math.min(y1, y2) - pad,
                Math.max(x1, x2) + pad, Math.max(y1, y2) + pad);
    }

    public void drawBoard(Canvas canvas) {
        if (boardCache == null) return;
        if (!takeClip(canvas) || !Rect.intersects(clipBounds, panelBounds)) return;
        // One blit of an already painted board, so a partial repaint lands pixel-exact on top of
        // what is already on screen - no seam along the edge of the repainted area.
        canvas.drawBitmap(boardCache, panelBounds.left, panelBounds.top, null);
    }

    /** Faint squares on the two ends of the previous move; drawn under the pieces. */
    public void drawLastMove(Canvas canvas, Point prev, Point curr) {
        takeClip(canvas);
        drawIfVisible(canvas, imageLastMove, markRect(prev, LAST_SIZE));
        drawIfVisible(canvas, imageLastMove, markRect(curr, LAST_SIZE));
    }

    /** {@code skip} is the square a piece is currently sliding out of, or null. */
    public void drawPieces(Canvas canvas, byte[][] cell, Point skip) {
        takeClip(canvas);
        for (int i = 0; i < ROW; i++)
            for (int j = 0; j < COL; j++) {
                byte piece = cell[i][j];
                if (piece == PieceCode.EMPTY) continue;
                if (skip != null && skip.x == i && skip.y == j) continue;
                drawIfVisible(canvas, pieces[piece - PieceCode.BLACK_KING], pieceRect(i, j));
            }
    }

    /** Reads the region being repainted; everything outside it is skipped. */
    private boolean takeClip(Canvas canvas) {
        if (canvas.getClipBounds(clipBounds)) return true;
        clipBounds.setEmpty();
        return false;
    }

    private void drawIfVisible(Canvas canvas, Bitmap bitmap, Rect where) {
        if (Rect.intersects(clipBounds, where)) {
            canvas.drawBitmap(bitmap, null, where, paint);
        }
    }

    /**
     * Draws a piece part-way along its move, {@code t} running 0..1. It swells slightly at the
     * halfway point so the move reads as lifting the piece and putting it back down.
     */
    public void drawMovingPiece(Canvas canvas, byte piece, Point from, Point to, float t) {
        float x = (from.y + (to.y - from.y) * t) * CELL_SIZE + LEFT;
        float y = (from.x + (to.x - from.x) * t) * CELL_SIZE + UP;
        float half = SIZE * (1f + 0.12f * (float) Math.sin(Math.PI * t));
        cellRect.set(Math.round(x - half), Math.round(y - half),
                Math.round(x + half), Math.round(y + half));
        canvas.drawBitmap(pieces[piece - PieceCode.BLACK_KING], null, cellRect, paint);
    }

    public void drawSelect(Canvas canvas, Point pos) {
        takeClip(canvas);
        drawIfVisible(canvas, imageSelected, markRect(pos, SELECT_SIZE));
    }

    /** A green dot on empty targets, a red capture frame where an enemy piece can be taken. */
    public void drawAllPossibleMove(Canvas canvas, ArrayList<Move> possibleMove, byte[][] cell) {
        takeClip(canvas);
        for (Move move : possibleMove) {
            Point target = move.to;
            boolean empty = cell[target.x][target.y] == PieceCode.EMPTY;
            drawIfVisible(canvas, empty ? imageMoveDot : imageCapture,
                    markRect(target, empty ? DOT_SIZE : CAPTURE_SIZE));
        }
    }

    /** A marker's box, centred on the intersection. Each marker brings its own half-width. */
    private Rect markRect(Point p, int half) {
        int x = p.y * CELL_SIZE + LEFT;
        int y = p.x * CELL_SIZE + UP;
        cellRect.set(x - half, y - half, x + half, y + half);
        return cellRect;
    }

    private Rect pieceRect(int row, int col) {
        int x = col * CELL_SIZE + LEFT;
        int y = row * CELL_SIZE + UP;
        cellRect.set(x - SIZE, y - SIZE, x + SIZE, y + SIZE);
        return cellRect;
    }
}
