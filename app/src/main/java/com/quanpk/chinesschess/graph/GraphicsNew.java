package com.quanpk.chinesschess.graph;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;

import com.quanpk.chinesschess.R;
import com.quanpk.chinesschess.chess.State;

import java.util.ArrayList;

public class GraphicsNew {

    public static int CELL_SIZE;
    public static int BORDER;
    public final static int ROW = 10;
    public final static int COL = 9;
    public static int LEFT;
    public static int UP;
    public static int RIGHT;
    public static int DOWN;
    public static int SIZE;
    protected Bitmap imageLogo;
    protected Bitmap imageba;
    protected Bitmap imagebb;
    protected Bitmap imagebc;
    protected Bitmap imagebk;
    protected Bitmap imagebn;
    protected Bitmap imagebp;
    protected Bitmap imagebr;
    protected Bitmap imagera;
    protected Bitmap imagerb;
    protected Bitmap imagerc;
    protected Bitmap imagerk;
    protected Bitmap imagern;
    protected Bitmap imagerp;
    protected Bitmap imagerr;
    protected Bitmap imagesel;
    protected Bitmap imageclr;
    protected Bitmap imageban;
    protected Paint paint;

    public GraphicsNew(Resources res, int width, int height) {
        paint = new Paint();
        paint.setStrokeWidth(2);
        imageLogo = BitmapFactory.decodeResource(res, R.drawable.logo);
        imageba = BitmapFactory.decodeResource(res, R.drawable.ba);
        imagebb = BitmapFactory.decodeResource(res, R.drawable.bb);
        imagebc = BitmapFactory.decodeResource(res, R.drawable.bc);
        imagebk = BitmapFactory.decodeResource(res, R.drawable.bk);
        imagebn = BitmapFactory.decodeResource(res, R.drawable.bn);
        imagebp = BitmapFactory.decodeResource(res, R.drawable.bp);
        imagebr = BitmapFactory.decodeResource(res, R.drawable.br);
        imagera = BitmapFactory.decodeResource(res, R.drawable.ra);
        imagerb = BitmapFactory.decodeResource(res, R.drawable.rb);
        imagerc = BitmapFactory.decodeResource(res, R.drawable.rc);
        imagerk = BitmapFactory.decodeResource(res, R.drawable.rk);
        imagern = BitmapFactory.decodeResource(res, R.drawable.rn);
        imagerp = BitmapFactory.decodeResource(res, R.drawable.rp);
        imagerr = BitmapFactory.decodeResource(res, R.drawable.rr);
        imagesel = BitmapFactory.decodeResource(res, R.drawable.sel);
        imageclr = BitmapFactory.decodeResource(res, R.drawable.clr);
        imageban = BitmapFactory.decodeResource(res, R.drawable.banco);

        CELL_SIZE = width / COL;
        BORDER = (CELL_SIZE + width % COL) / 2;
        LEFT = BORDER;
        UP = BORDER;
        RIGHT = LEFT + (COL - 1) * CELL_SIZE;
        DOWN = UP + (ROW - 1) * CELL_SIZE;
        SIZE = res.getDimensionPixelOffset(R.dimen.padding);
    }

    public void drawBanCo(Canvas canvas) {
        drawMain(canvas);
        drawBorder(canvas);
        drawX(canvas);
        drawPlus(canvas);
    }

    private void drawPlus(Canvas canvas) {
        drawPlus(canvas, 1, 2, 0);
        drawPlus(canvas, 1, 7, 0);
        drawPlus(canvas, 7, 7, 0);
        drawPlus(canvas, 7, 2, 0);
        for (int i = 1; i < 4; i++) {
            drawPlus(canvas, i * 2, 3, 0);
            drawPlus(canvas, i * 2, 6, 0);
        }
        drawPlus(canvas, 0, 3, 1);
        drawPlus(canvas, 0, 6, 1);
        drawPlus(canvas, 8, 3, 2);
        drawPlus(canvas, 8, 6, 2);
    }

    private void drawMain(Canvas canvas) {
        paint.setColor(Color.rgb(153, 204, 51));
        canvas.drawBitmap(imageban, null, new Rect(LEFT - BORDER, UP - BORDER,
                RIGHT + BORDER, DOWN + BORDER), paint);
        paint.setColor(Color.rgb(51, 51, 51));
        for (int i = 0; i < ROW; i++)
            canvas.drawLine(LEFT, (i) * CELL_SIZE + UP, RIGHT, (i) * CELL_SIZE
                    + UP, paint);
        for (int i = 1; i < COL - 1; i++) {
            canvas.drawLine((i) * CELL_SIZE + BORDER, UP, (i) * CELL_SIZE
                    + BORDER, UP + 4 * CELL_SIZE, paint);
            canvas.drawLine((i) * CELL_SIZE + BORDER, UP + 5 * CELL_SIZE, (i)
                    * CELL_SIZE + BORDER, DOWN, paint);
        }

    }

    protected void drawX(Canvas canvas) {
        paint.setStrokeWidth(2);
        canvas.drawLine(LEFT + 3 * CELL_SIZE, UP, LEFT + 5 * CELL_SIZE, UP + 2
                * CELL_SIZE, paint);
        canvas.drawLine(LEFT + 5 * CELL_SIZE, UP, LEFT + 3 * CELL_SIZE, UP + 2
                * CELL_SIZE, paint);
        canvas.drawLine(LEFT + 3 * CELL_SIZE, DOWN, LEFT + 5 * CELL_SIZE, DOWN
                - 2 * CELL_SIZE, paint);
        canvas.drawLine(LEFT + 5 * CELL_SIZE, DOWN, LEFT + 3 * CELL_SIZE, DOWN
                - 2 * CELL_SIZE, paint);
    }

    protected void drawBorder(Canvas canvas) {
        int border = 5;
        canvas.drawLine(LEFT, UP, LEFT, DOWN, paint);
        canvas.drawLine(RIGHT, UP, RIGHT, DOWN, paint);
        paint.setStrokeWidth(5);
        canvas.drawLine(LEFT - border, UP - border, RIGHT + border,
                UP - border, paint);
        canvas.drawLine(LEFT - border, DOWN + border, RIGHT + border, DOWN
                + border, paint);

        canvas.drawLine(LEFT - border, UP - border, LEFT - border, DOWN
                + border, paint);
        canvas.drawLine(RIGHT + border, UP - border, RIGHT + border, DOWN
                + border, paint);
        paint.setStrokeWidth(2);
    }

    protected void drawPlus(Canvas canvas, int x, int y, int pos) {
        int border = 3;
        int kc = CELL_SIZE / 4;
        x = x * CELL_SIZE + LEFT;
        y = y * CELL_SIZE + UP;
        if (pos != 1) {
            canvas.drawLine(x - border, y - border, x - kc, y - border, paint);
            canvas.drawLine(x - border, y - border, x - border, y - kc, paint);

            canvas.drawLine(x - border, y + border, x - kc, y + border, paint);
            canvas.drawLine(x - border, y + border, x - border, y + kc, paint);
        }
        if (pos != 2) {
            canvas.drawLine(x + border, y - border, x + kc, y - border, paint);
            canvas.drawLine(x + border, y - border, x + border, y - kc, paint);

            canvas.drawLine(x + border, y + border, x + kc, y + border, paint);
            canvas.drawLine(x + border, y + border, x + border, y + kc, paint);
        }
    }

    public void drawQuanCo(Canvas canvas, byte[][] cell) {
        Bitmap[] pictureBox = {imagebk, imageba, imagebb, imagebn, imagebr,
                imagebc, imagebp, imagerk, imagera, imagerb, imagern, imagerr,
                imagerc, imagerp};
        for (int i = 0; i < ROW; i++)
            for (int j = 0; j < COL; j++) {
                byte piece = cell[i][j];
                if (piece != 0) {
                    canvas.drawBitmap(pictureBox[piece - 8], null,
                            makeRect(new Point(i, j)), new Paint());
                }
            }
    }

    public void drawSelect(Canvas canvas, Point pos) {
        canvas.drawBitmap(imagesel, null, makeRect(pos), new Paint());
    }

    public void drawAllPossibleMove(Canvas canvas, ArrayList<State> posibleMove) {
        for (State state : posibleMove) {
            int x = state.curr.x;
            int y = state.curr.y;
            int c = paint.getColor();
            paint.setColor(Color.BLACK);
            canvas.drawCircle(y * CELL_SIZE + BORDER, x * CELL_SIZE + BORDER, 18, paint);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(y * CELL_SIZE + BORDER, x * CELL_SIZE + BORDER, 14, paint);
            paint.setColor(c);
        }
    }

    private Rect makeRect(Point p) {
        Rect selRect = new Rect();
        selRect.left = p.y * CELL_SIZE + LEFT - SIZE;
        selRect.top = p.x * CELL_SIZE + UP - SIZE;
        selRect.right = p.y * CELL_SIZE + LEFT + SIZE;
        selRect.bottom = p.x * CELL_SIZE + UP + SIZE;
        return selRect;
    }
}