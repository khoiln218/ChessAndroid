package com.quanpk.chinesschess;

import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.quanpk.chinesschess.chess.Board;
import com.quanpk.chinesschess.chess.State;
import com.quanpk.chinesschess.game.ChinessChessNew;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class Game extends Activity {
    ChinessChessNew game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game);

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int height = displaymetrics.heightPixels;
        int width = displaymetrics.widthPixels;

        int turnGame = getIntent().getExtras().getInt("turn_game");
        int levelGame = getIntent().getExtras().getInt("level_game");
        if (turnGame == 2) {
            game = new ChinessChessNew(this, levelGame, true, width, height);
            loadGame();
        } else {
            game = new ChinessChessNew(this, levelGame, turnGame == 0, width, height);
        }

        LinearLayout layout = (LinearLayout) findViewById(R.id.layout);
        layout.addView(game);
        findViewById(R.id.back_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Game.this.finish();
            }
        });
        findViewById(R.id.undo_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                undoGame();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            saveGame();
        } catch (Exception e) {
        }
    }

    private void saveGame() {
        FileOutputStream f = null;
        try {
            f = openFileOutput("chess.sav", MODE_PRIVATE);
            f.write(game.isGameOver ? 1 : 0);
            if (!game.isGameOver) {
                for (int i = 0; i < Board.ROW; i++) {
                    for (int j = 0; j < Board.COL; j++) {
                        f.write(game.board.cell[i][j]);
                    }
                }
                f.write(game.turn ? 1 : 0);
                f.write(game.board.currMove.x);
                f.write(game.board.currMove.y);
                f.write(game.board.prevMove.x);
                f.write(game.board.prevMove.y);
                f.write(game.board.select ? 1 : 0);
                f.write(game.board.move ? 1 : 0);
                f.write(game.board.RED ? 1 : 0);
            }
            f.flush();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Save fail", Toast.LENGTH_SHORT).show();
        } finally {
            try {
                if (f != null) {
                    f.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void loadGame() {
        FileInputStream f = null;
        try {
            f = openFileInput("chess.sav");
            InputStreamReader reader = new InputStreamReader(f);
            game.isGameOver = reader.read() == 1;
            if (!game.isGameOver) {
                for (int i = 0; i < Board.ROW; i++) {
                    for (int j = 0; j < Board.COL; j++) {
                        game.board.cell[i][j] = (byte) reader.read();
                    }
                }
                game.turn = reader.read() == 1;
                game.board.currMove = new Point(reader.read(), reader.read());
                game.board.prevMove = new Point(reader.read(), reader.read());
                game.board.select = reader.read() == 1;
                game.board.move = reader.read() == 1;
                game.board.RED = reader.read() == 1;
            } else
                game.isGameOver = false;
            reader.close();
        } catch (IOException e) {
            Toast.makeText(this, "Not found", Toast.LENGTH_SHORT).show();
        } finally {
            try {
                if (f != null) {
                    f.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void undoGame() {
        if (game.board.listUndo.size() > 1) {
            //undo your move
            State pos = game.board.listUndo.get(game.board.listUndo.size() - 1);
            game.board.listUndo.remove(game.board.listUndo.size() - 1);
            game.board.prevMove = pos.prev;
            game.board.currMove = pos.curr;
            game.board.cell[pos.prev.x][pos.prev.y] = pos.value1;
            game.board.cell[pos.curr.x][pos.curr.y] = pos.value2;
            //undo computer
            pos = game.board.listUndo.get(game.board.listUndo.size() - 1);
            game.board.listUndo.remove(game.board.listUndo.size() - 1);
            game.board.prevMove = pos.prev;
            game.board.currMove = pos.curr;
            game.board.cell[pos.prev.x][pos.prev.y] = pos.value1;
            game.board.cell[pos.curr.x][pos.curr.y] = pos.value2;
            //reset result
            game.isGameOver = false;
            game.invalidate();
        }
    }
}
