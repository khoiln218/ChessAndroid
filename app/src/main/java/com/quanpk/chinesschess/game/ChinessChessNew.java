package com.quanpk.chinesschess.game;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.quanpk.chinesschess.chess.Board;
import com.quanpk.chinesschess.chess.State;
import com.quanpk.chinesschess.chess._AI;
import com.quanpk.chinesschess.graph.GraphicsNew;

public class ChinessChessNew extends View {
    _AI ai;
    GraphicsNew graph;
    public Board board;
    public boolean isGameOver;
    public boolean turn;

    public ChinessChessNew(Context context, int level, boolean turn, int w, int h) {
        super(context);
        this.turn = turn;
        graph = new GraphicsNew(getResources(), w, h);
        board = new Board(!turn);
        ai = new _AI(board, level);
        setFocusable(true);
        if (!turn) {
            computer();
        }
        setFocusableInTouchMode(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
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
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            if (!isGameOver && !board.RED) {
                int x = (int) (event.getY() - GraphicsNew.UP + GraphicsNew.CELL_SIZE / 2)
                        / GraphicsNew.CELL_SIZE;
                int y = (int) (event.getX() - GraphicsNew.LEFT + GraphicsNew.CELL_SIZE / 2)
                        / GraphicsNew.CELL_SIZE;
                if (x < 0 || x >= GraphicsNew.ROW || y < 0 || y >= GraphicsNew.COL)
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
        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... params) {
                return board.isGameOver(board.RED) || board.isGameOver(!board.RED);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                isGameOver = result;
                if (isGameOver) {
                    showGameOver();
                } else if (board.RED) {
                    computer();
                }
            }
        }.execute();
    }

    private void computer() {
        final ProgressDialog dialog = new ProgressDialog(getContext());
        dialog.setCancelable(false);
        dialog.setMessage("Thinking...");
        dialog.show();
        new AsyncTask<Void, Void, State>() {

            @Override
            protected State doInBackground(Void... params) {
                State pos = ai.generateMove(board.RED);
                board.prevMove = pos.prev;
                board.currMove = pos.curr;
                return pos;
            }

            @Override
            protected void onPostExecute(State pos) {
                move(pos.curr.x, pos.curr.y);
                dialog.dismiss();
            }
        }.execute();
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
