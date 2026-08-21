package com.ttnt.chinesschess.game;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.ttnt.chinesschess.chess.Board;
import com.ttnt.chinesschess.chess.State;
import com.ttnt.chinesschess.chess._AI;
import com.ttnt.chinesschess.graph.Graphics;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressLint("ViewConstructor")
public class ChineseChessGame extends View {
    _AI ai;
    Graphics graph;
    public Board board;
    public boolean isGameOver;
    public boolean turn;

    /** Single thread so background work stays serialized, like AsyncTask#execute() used to be. */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Listener listener;
    /**
     * Ends of the move that was actually played last. Board#prevMove doubles as "the square the
     * player is holding", so it cannot be used for this - picking a piece up would wipe the mark.
     */
    private Point lastFrom;
    private Point lastTo;

    /** Lets the activity drive the two side panels: whose clock runs, and when it stops. */
    public interface Listener {
        /** A new turn begins; {@code computerToMove} tells which side panel owns the clock. */
        void onTurnStarted(boolean computerToMove);

        /** The AI started or finished searching, so its panel can show the sweeping ring. */
        void onThinkingChanged(boolean thinking);

        /** The game ended; no clock runs any more. */
        void onGameOver(boolean playerWon);
    }

    public ChineseChessGame(Context context, int level, boolean turn) {
        super(context);
        this.turn = turn;
        graph = new Graphics(getResources());
        board = new Board(!turn);
        ai = new _AI(board, level);
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /**
     * Starts the first turn. Kept out of the constructor so the activity can attach its listener -
     * and restore a saved board - before either side is put on the clock.
     */
    public void start() {
        if (board.move) {
            // Restored game: the saved board still knows which move ended the last session.
            showLastMove(board.prevMove, board.currMove);
        }
        beginTurn();
    }

    /** Marks the two ends of the move just played, whichever side played it. */
    public void showLastMove(Point from, Point to) {
        lastFrom = new Point(from);
        lastTo = new Point(to);
        invalidate();
    }

    public void clearLastMove() {
        lastFrom = null;
        lastTo = null;
        invalidate();
    }

    private void beginTurn() {
        if (listener != null) {
            listener.onTurnStarted(board.RED);
        }
        if (board.RED) {
            computer();
        }
    }

    /** Called by the activity when a side runs out of time: whoever is to move loses. */
    public void loseByTimeout() {
        if (isGameOver) return;
        isGameOver = true;
        boolean playerWon = board.RED;
        Toast.makeText(getContext(),
                playerWon ? "Computer ran out of time. You Win!" : "Time is up. You Lose!",
                Toast.LENGTH_SHORT).show();
        if (listener != null) {
            listener.onThinkingChanged(false);
            listener.onGameOver(playerWon);
        }
    }

    /**
     * The artwork is a 10:11 box - the 9 x 10 grid plus a cell of margin. Reporting that instead
     * of swallowing every spare pixel lets the layout park the two player cards right against it.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = width * (Graphics.ROW + 1) / (Graphics.COL + 1);
        if (MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            height = Math.min(height, MeasureSpec.getSize(heightMeasureSpec));
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        graph.setSize(w, h);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        graph.drawBanCo(canvas);
        if (lastFrom != null) {
            graph.drawLastMove(canvas, lastFrom, lastTo);
        }
        graph.drawQuanCo(canvas, board.cell);
        if (board.isCheckSelect(board.prevMove.x, board.prevMove.y) && board.select) {
            graph.drawSelect(canvas, board.prevMove);
            graph.drawAllPossibleMove(canvas, board.allMove(board.prevMove), board.cell);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mainHandler.removeCallbacksAndMessages(null);
        executor.shutdownNow();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            if (!isGameOver && !board.RED) {
                // floor, not truncation: the board is centred, so taps above/left of it give a
                // negative offset that must not round back into row/column 0.
                int x = (int) Math.floor((event.getY() - Graphics.UP + Graphics.CELL_SIZE / 2f)
                        / Graphics.CELL_SIZE);
                int y = (int) Math.floor((event.getX() - Graphics.LEFT + Graphics.CELL_SIZE / 2f)
                        / Graphics.CELL_SIZE);
                if (x < 0 || x >= Graphics.ROW || y < 0 || y >= Graphics.COL)
                    return false;

                if (board.isCheckSelect(x, y)) {
                    select(x, y);
                    Log.e("touch", "select");
                } else if (board.isCheckMove(x, y)) {
                    move(x, y);
                    Log.e("touch", "move");
                } else {
                    Log.e("touch", "blank");
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    public void select(int x, int y) {
        board.select(x, y);
        invalidate();
    }

    public void move(int x, int y) {
        lastFrom = new Point(board.prevMove);
        lastTo = new Point(x, y);
        board.moveTo(x, y);
        invalidate();
        switchPlayer();
    }

    public void switchPlayer() {
        executor.execute(() -> {
            final boolean result = board.isGameOver(board.RED) || board.isGameOver(!board.RED);
            mainHandler.post(() -> {
                isGameOver = result;
                if (isGameOver) {
                    showGameOver();
                    if (listener != null) {
                        listener.onGameOver(board.RED);
                    }
                } else {
                    beginTurn();
                }
            });
        });
    }

    private void computer() {
        if (listener != null) {
            listener.onThinkingChanged(true);
        }
        executor.execute(() -> {
            final State pos = ai.generateMove(board.RED);
            mainHandler.post(() -> {
                if (listener != null) {
                    listener.onThinkingChanged(false);
                }
                // The clock may have run out while the search was still running.
                if (isGameOver) return;
                board.prevMove = pos.prev;
                board.currMove = pos.curr;
                move(pos.curr.x, pos.curr.y);
            });
        });
    }

    void showGameOver() {
        if (board.RED) {
            Toast.makeText(getContext(), "You Win!", Toast.LENGTH_SHORT)
                    .show();
        } else {
            Toast.makeText(getContext(), "You Lose!", Toast.LENGTH_SHORT)
                    .show();
        }
    }
}
