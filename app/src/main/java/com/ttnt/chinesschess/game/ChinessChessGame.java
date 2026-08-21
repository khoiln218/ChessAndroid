package com.ttnt.chinesschess.game;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.ttnt.chinesschess.chess.Board;
import com.ttnt.chinesschess.chess.State;
import com.ttnt.chinesschess.chess._AI;
import com.ttnt.chinesschess.graph.Graphics;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressLint("ViewConstructor")
public class ChinessChessGame extends View {
    _AI ai;
    Graphics graph;
    public Board board;
    public boolean isGameOver;
    public boolean turn;

    /** Single thread so background work stays serialized, like AsyncTask#execute() used to be. */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public ChinessChessGame(Context context, int level, boolean turn, int w) {
        super(context);
        this.turn = turn;
        graph = new Graphics(getResources(), w);
        board = new Board(!turn);
        ai = new _AI(board, level);
        setFocusable(true);
        if (!turn) {
            computer();
        }
        setFocusableInTouchMode(true);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        graph.drawBanCo(canvas);
        graph.drawQuanCo(canvas, board.cell);
        if (board.isCheckSelect(board.prevMove.x, board.prevMove.y) && board.select) {
            graph.drawSelect(canvas, board.prevMove);
            graph.drawAllPossibleMove(canvas, board.allMove(board.prevMove));
        }
        if (board.move) {
            graph.drawSelect(canvas, board.currMove);
            graph.drawSelect(canvas, board.prevMove);
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
                int x = (int) (event.getY() - Graphics.UP + (float) Graphics.CELL_SIZE / 2)
                        / Graphics.CELL_SIZE;
                int y = (int) (event.getX() - Graphics.LEFT + (float) Graphics.CELL_SIZE / 2)
                        / Graphics.CELL_SIZE;
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
                } else if (board.RED) {
                    computer();
                }
            });
        });
    }

    private void computer() {
        final AlertDialog dialog = createThinkingDialog();
        dialog.show();
        executor.execute(() -> {
            final State pos = ai.generateMove(board.RED);
            board.prevMove = pos.prev;
            board.currMove = pos.curr;
            mainHandler.post(() -> {
                move(pos.curr.x, pos.curr.y);
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
            });
        });
    }

    /** Replacement for the deprecated ProgressDialog: a spinner plus a label in an AlertDialog. */
    @SuppressLint("SetTextI18n")
    private AlertDialog createThinkingDialog() {
        Context context = getContext();
        int padding = Math.round(24 * getResources().getDisplayMetrics().density);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setGravity(Gravity.CENTER_VERTICAL);
        content.setPadding(padding, padding, padding, padding);

        content.addView(new ProgressBar(context), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView message = new TextView(context);
        message.setText("Thinking...");
        LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        messageParams.leftMargin = padding;
        content.addView(message, messageParams);

        return new AlertDialog.Builder(context)
                .setView(content)
                .setCancelable(false)
                .create();
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
