package com.ttnt.chinesschess.graph;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;

import com.ttnt.chinesschess.R;
import com.ttnt.chinesschess.chess.State;

import java.util.ArrayList;

/**
 * Draws the board from the artwork in drawable-nodpi. The board image already carries the grid,
 * both palaces, the river and the frame, and it is laid out on a 10 x 11 cell box: one cell of
 * margin around the 9 x 10 grid of intersections, exactly as the art expects.
 */
public class Graphics {

    public static int CELL_SIZE;
    /** Margin between the outer edge of the artwork and the first line: one full cell. */
    public static int BORDER;
    public final static int ROW = 10;
    public final static int COL = 9;
    public static int LEFT;
    public static int UP;
    public static int RIGHT;
    public static int DOWN;
    /** Half the width a piece is drawn at. */
    public static int SIZE;

    private final Bitmap imageBoard;
    /** Indexed by {@code cell value - 8}: black tuong..tot, then red tuong..tot. */
    private final Bitmap[] pieces;
    private final Bitmap imageSelected;
    private final Bitmap imageMoveDot;
    private final Bitmap imageCapture;
    private final Bitmap imageLastMove;
    private final Paint paint;

    private final Rect panelBounds = new Rect();
    private final RectF panelRect = new RectF();
    private final Path panelPath = new Path();
    private final Rect cellRect = new Rect();
    /** Maps the artwork onto the panel; with a dirty rect set only that slice gets rasterised. */
    private final Matrix boardMatrix = new Matrix();
    private final Rect clipBounds = new Rect();

    public Graphics(Resources res) {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setFilterBitmap(true);
        imageBoard = BitmapFactory.decodeResource(res, R.drawable.board_bg);
        pieces = new Bitmap[]{
                BitmapFactory.decodeResource(res, R.drawable.black_tuong),
                BitmapFactory.decodeResource(res, R.drawable.black_si),
                BitmapFactory.decodeResource(res, R.drawable.black_tuong_voi),
                BitmapFactory.decodeResource(res, R.drawable.black_ma),
                BitmapFactory.decodeResource(res, R.drawable.black_xe),
                BitmapFactory.decodeResource(res, R.drawable.black_phao),
                BitmapFactory.decodeResource(res, R.drawable.black_tot),
                BitmapFactory.decodeResource(res, R.drawable.red_tuong),
                BitmapFactory.decodeResource(res, R.drawable.red_si),
                BitmapFactory.decodeResource(res, R.drawable.red_tuong_voi),
                BitmapFactory.decodeResource(res, R.drawable.red_ma),
                BitmapFactory.decodeResource(res, R.drawable.red_xe),
                BitmapFactory.decodeResource(res, R.drawable.red_phao),
                BitmapFactory.decodeResource(res, R.drawable.red_tot)};
        imageSelected = BitmapFactory.decodeResource(res, R.drawable.hl_selected);
        imageMoveDot = BitmapFactory.decodeResource(res, R.drawable.hl_move_dot);
        imageCapture = BitmapFactory.decodeResource(res, R.drawable.hl_capture);
        imageLastMove = BitmapFactory.decodeResource(res, R.drawable.hl_last_move);
    }

    /**
     * Fits the board inside a view of {@code width} x {@code height} pixels and centres it on both
     * axes: the cell size is limited by whichever axis is tighter, then the leftover space is split
     * evenly on each side.
     */
    public void setSize(int width, int height) {
        CELL_SIZE = Math.min(width / (COL + 1), height / (ROW + 1));
        BORDER = CELL_SIZE;
        SIZE = Math.round(CELL_SIZE * 0.47f);

        int boardWidth = (COL + 1) * CELL_SIZE;
        int boardHeight = (ROW + 1) * CELL_SIZE;
        int left = (width - boardWidth) / 2;
        int top = (height - boardHeight) / 2;

        LEFT = left + BORDER;
        UP = top + BORDER;
        RIGHT = LEFT + (COL - 1) * CELL_SIZE;
        DOWN = UP + (ROW - 1) * CELL_SIZE;

        panelBounds.set(left, top, left + boardWidth, top + boardHeight);
        panelRect.set(panelBounds);
        float radius = CELL_SIZE / 2.5f;
        panelPath.reset();
        panelPath.addRoundRect(panelRect, radius, radius, Path.Direction.CW);
        boardMatrix.setRectToRect(
                new RectF(0, 0, imageBoard.getWidth(), imageBoard.getHeight()),
                panelRect, Matrix.ScaleToFit.FILL);
    }

    /** The area a slide from {@code from} to {@code to} can touch, markers and swell included. */
    public void moveBounds(Point from, Point to, Rect out) {
        int pad = Math.round(SIZE * 1.2f) + 2;
        int x1 = from.y * CELL_SIZE + LEFT;
        int y1 = from.x * CELL_SIZE + UP;
        int x2 = to.y * CELL_SIZE + LEFT;
        int y2 = to.x * CELL_SIZE + UP;
        out.set(Math.min(x1, x2) - pad, Math.min(y1, y2) - pad,
                Math.max(x1, x2) + pad, Math.max(y1, y2) + pad);
    }

    public void drawBanCo(Canvas canvas) {
        if (!takeClip(canvas) || !Rect.intersects(clipBounds, panelBounds)) return;
        canvas.save();
        canvas.clipPath(panelPath);
        // Same matrix whatever the dirty rect is, so a partial repaint lands pixel-exact on top
        // of what is already on screen - no seam along the edge of the repainted area.
        canvas.drawBitmap(imageBoard, boardMatrix, paint);
        canvas.restore();
    }

    /** Faint squares on the two ends of the previous move; drawn under the pieces. */
    public void drawLastMove(Canvas canvas, Point prev, Point curr) {
        takeClip(canvas);
        drawIfVisible(canvas, imageLastMove, cellRect(prev));
        drawIfVisible(canvas, imageLastMove, cellRect(curr));
    }

    /** {@code skip} is the square a piece is currently sliding out of, or null. */
    public void drawQuanCo(Canvas canvas, byte[][] cell, Point skip) {
        takeClip(canvas);
        for (int i = 0; i < ROW; i++)
            for (int j = 0; j < COL; j++) {
                byte piece = cell[i][j];
                if (piece == 0 || (skip != null && skip.x == i && skip.y == j)) continue;
                drawIfVisible(canvas, pieces[piece - 8], pieceRect(i, j));
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
        canvas.drawBitmap(pieces[piece - 8], null, cellRect, paint);
    }

    public void drawSelect(Canvas canvas, Point pos) {
        takeClip(canvas);
        drawIfVisible(canvas, imageSelected, cellRect(pos));
    }

    /** A green dot on empty targets, a red capture frame where an enemy piece can be taken. */
    public void drawAllPossibleMove(Canvas canvas, ArrayList<State> posibleMove, byte[][] cell) {
        takeClip(canvas);
        for (State state : posibleMove) {
            Point target = state.curr;
            Bitmap marker = cell[target.x][target.y] == 0 ? imageMoveDot : imageCapture;
            drawIfVisible(canvas, marker, cellRect(target));
        }
    }

    /** One whole cell, centred on the intersection - the size the markers are drawn for. */
    private Rect cellRect(Point p) {
        int half = CELL_SIZE / 2;
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
