package com.ttnt.chinesschess.game;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import com.ttnt.chinesschess.chess.Board;
import com.ttnt.chinesschess.chess.State;
import com.ttnt.chinesschess.Settings;
import com.ttnt.chinesschess.chess.AI;
import com.ttnt.chinesschess.graph.BoardTheme;
import com.ttnt.chinesschess.graph.Graphics;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressLint("ViewConstructor")
public class ChineseChessGame extends View {
    /**
     * Replaced when the strength is changed mid-game, which is read on the search thread, so the
     * two threads have to agree on which one they are looking at.
     */
    volatile AI ai;
    Graphics graph;
    public Board board;
    public boolean isGameOver;
    public boolean turn;

    /** Single thread so background work stays serialized, like AsyncTask#execute() used to be. */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Listener listener;
    /**
     * Bumped by every new game. Work already queued for the previous game carries the old value
     * and is dropped when it lands - a search can still be running when a game ends on time.
     */
    private int generation;
    /**
     * Ends of the move that was actually played last. Board#prevMove doubles as "the square the
     * player is holding", so it cannot be used for this - picking a piece up would wipe the mark.
     */
    private Point lastFrom;
    private Point lastTo;

    /** How long a piece takes to slide from one intersection to the next one. */
    private static final long MOVE_ANIM_MS = 220L;
    private long animStart;
    private byte animPiece;
    /** Region the running slide can touch - the only part that gets repainted per frame. */
    private final Rect animDirty = new Rect();

    /** Why a game ended, which is what the result card has to explain. */
    public enum End {
        CHECKMATE, TIMEOUT, PERPETUAL_CHECK, PERPETUAL_CHASE, REPETITION_DRAW
    }

    /** Lets the activity drive the two side panels: whose clock runs, and when it stops. */
    public interface Listener {
        /** A new turn begins; {@code computerToMove} tells which side panel owns the clock. */
        void onTurnStarted(boolean computerToMove);

        /** The AI started or finished searching, so its panel can show the sweeping ring. */
        void onThinkingChanged(boolean thinking);

        /** The game ended; no clock runs anymore. {@code how} tells why, and for a draw nobody won. */
        void onGameOver(boolean playerWon, End how);
    }

    public ChineseChessGame(Context context, int level, boolean turn) {
        super(context);
        this.turn = turn;
        graph = new Graphics(getResources(), Settings.theme(context));
        board = new Board(!turn);
        ai = new AI(board, level);
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /** Repaints the board in another palette, mid-game and without disturbing the position. */
    public void setTheme(BoardTheme theme) {
        graph.setTheme(theme);
        invalidate();
    }

    /**
     * Plays at another strength from the next move on. The search already running keeps the
     * engine it started with; stopping it to swap would only throw away work that is about to
     * produce a move anyway.
     */
    public void setLevel(int level) {
        ai = new AI(board, level);
    }

    /**
     * Puts a fresh game on the board and view that are already here - no activity restart, so the
     * artwork, the banner and the decoded bitmaps all stay put. Nothing moves and nothing is
     * accepted until {@link #start}: the caller may have something to show first.
     *
     * @param playerFirst which side opens, re-read rather than remembered, so a turn changed
     *                    part-way through a session takes effect at the next game
     */
    public void newGame(boolean playerFirst) {
        generation++;
        started = false;
        turn = playerFirst;
        board.reset(!turn);
        isGameOver = false;
        clearLastMove();
    }

    /** Whether play has actually begun. A board that is only on show takes no moves. */
    private boolean started;

    /** Whether the game is under way. Before it is, nothing may move and no clock may run. */
    public boolean isStarted() {
        return started;
    }

    public void start() {
        started = true;
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
        animStart = 0;
        invalidate();
    }

    public void clearLastMove() {
        lastFrom = null;
        lastTo = null;
        animStart = 0;
        invalidate();
    }

    /** 0..1 along the current slide, or 1 when nothing is moving. */
    private float animProgress() {
        if (animStart == 0) return 1f;
        float t = (SystemClock.uptimeMillis() - animStart) / (float) MOVE_ANIM_MS;
        if (t >= 1f) {
            animStart = 0;
            return 1f;
        }
        return t * t * (3f - 2f * t);
    }

    private void beginTurn() {
        if (listener != null) {
            listener.onTurnStarted(board.redToMove);
        }
        if (board.redToMove) {
            computer();
        }
    }

    /** Called by the activity when a side runs out of time: whoever is to move loses. */
    public void loseByTimeout() {
        if (isGameOver) return;
        isGameOver = true;
        if (listener != null) {
            listener.onThinkingChanged(false);
            listener.onGameOver(board.redToMove, End.TIMEOUT);
        }
    }

    /**
     * The board is the 9 x 10 grid plus the slice of margin {@link Graphics} keeps on each side.
     * Reporting that instead of swallowing every spare pixel lets the layout park the two player
     * cards right against it.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = Math.round(width * Graphics.boxCellsHigh() / Graphics.boxCellsWide());
        if (MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.UNSPECIFIED) {
            height = Math.min(height, MeasureSpec.getSize(heightMeasureSpec));
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldWidth, int oldHeight) {
        super.onSizeChanged(w, h, oldWidth, oldHeight);
        graph.setSize(w, h);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        graph.drawBoard(canvas);
        if (lastFrom != null) {
            graph.drawLastMove(canvas, lastFrom, lastTo);
        }
        float t = animProgress();
        boolean sliding = t < 1f;
        graph.drawPieces(canvas, board.cell, sliding ? lastTo : null);
        if (sliding) {
            graph.drawMovingPiece(canvas, animPiece, lastFrom, lastTo, t);
            graph.moveBounds(lastFrom, lastTo, animDirty);
            postInvalidateOnAnimation(animDirty.left, animDirty.top,
                    animDirty.right, animDirty.bottom);
        }
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
            if (started && !isGameOver && !board.redToMove) {
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
                } else if (board.isCheckMove(x, y)) {
                    move(x, y);
                } else {
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
        animPiece = board.getValue(lastFrom.x, lastFrom.y);
        animStart = SystemClock.uptimeMillis();
        board.moveTo(x, y);
        // Full repaint once, to wipe the selection ring and the move dots - those sit outside the
        // move's own region. Every frame after this one repaints just the sliding piece's area.
        invalidate();
        switchPlayer();
    }

    public void switchPlayer() {
        final int started = generation;
        executor.execute(() -> {
            final boolean mated = board.hasLost(board.redToMove) || board.hasLost(!board.redToMove);
            // Judged on the board the move just landed on, before either side is put on the clock.
            final Board.Repeat repeat = mated ? Board.Repeat.NONE : board.judgeRepetition();
            mainHandler.post(() -> {
                if (started != generation) return;
                isGameOver = mated || repeat != Board.Repeat.NONE;
                if (!isGameOver) {
                    beginTurn();
                    return;
                }
                if (listener == null) return;
                if (mated) {
                    listener.onGameOver(board.redToMove, End.CHECKMATE);
                    return;
                }
                // The player is the black side, so red losing is the player winning.
                switch (repeat) {
                    case RED_LOSES -> listener.onGameOver(true, endFor(true));
                    case BLACK_LOSES -> listener.onGameOver(false, endFor(false));
                    default -> listener.onGameOver(false, End.REPETITION_DRAW);
                }
            });
        });
    }

    /** Which of the two offences the guilty side committed, for the result card to name. */
    private End endFor(boolean redGuilty) {
        int checks = 0;
        int moves = 0;
        for (int i = board.history.size() - 1; i >= 0 && moves < 6; i--) {
            Board.Ply ply = board.history.get(i);
            if (ply.redMoved != redGuilty) continue;
            moves++;
            if (ply.gaveCheck) checks++;
        }
        return checks == moves ? End.PERPETUAL_CHECK : End.PERPETUAL_CHASE;
    }

    private void computer() {
        if (listener != null) {
            listener.onThinkingChanged(true);
        }
        final int started = generation;
        executor.execute(() -> {
            final State pos = ai.generateMove(board.redToMove);
            mainHandler.post(() -> {
                if (started != generation) return;
                if (listener != null) {
                    listener.onThinkingChanged(false);
                }
                // The clock may have run out while the search was still running.
                if (isGameOver) return;
                board.prevMove = pos.from;
                board.currMove = pos.to;
                move(pos.to.x, pos.to.y);
            });
        });
    }

}
