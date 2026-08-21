package com.ttnt.chinesschess;

import android.graphics.Insets;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.WindowInsets;
import android.view.WindowMetrics;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ttnt.chinesschess.chess.Board;
import com.ttnt.chinesschess.chess.State;
import com.ttnt.chinesschess.game.ChinessChessGame;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class Game extends AppCompatActivity {
    ChinessChessGame game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game);

        Point screen = getScreenSize();
        int width = screen.x;

        int turnGame = 1;
        int levelGame = 1;

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            turnGame = extras.getInt("turn_game");
            levelGame = extras.getInt("level_game");
        }

        if (turnGame == 2) {
            game = new ChinessChessGame(this, levelGame, true, width);
            loadGame();
        } else {
            game = new ChinessChessGame(this, levelGame, turnGame == 0, width);
        }

        LinearLayout layout = findViewById(R.id.layout);
        layout.addView(game);
        findViewById(R.id.back_button).setOnClickListener(v -> Game.this.finish());
        findViewById(R.id.undo_button).setOnClickListener(v -> undoGame());
    }

    /**
     * Usable screen size in pixels, excluding the system bars and display cutout so it matches
     * what the old Display#getMetrics() call reported.
     */
    private Point getScreenSize() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics metrics = getWindowManager().getCurrentWindowMetrics();
            Insets insets = metrics.getWindowInsets().getInsetsIgnoringVisibility(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            Rect bounds = metrics.getBounds();
            return new Point(bounds.width() - insets.left - insets.right,
                    bounds.height() - insets.top - insets.bottom);
        }
        return getLegacyScreenSize();
    }

    @SuppressWarnings("deprecation")
    private Point getLegacyScreenSize() {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        return new Point(displaymetrics.widthPixels, displaymetrics.heightPixels);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            saveGame();
        } catch (Exception ignored) {
        }
    }

    private void saveGame() {
        try (FileOutputStream f = openFileOutput("chess.sav", MODE_PRIVATE)) {
            try {
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
                Toast.makeText(this, "Save fail", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException ignored) {
        }
    }

    public void loadGame() {
        try (FileInputStream f = openFileInput("chess.sav")) {
            try {
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
            }
        } catch (IOException ignored) {
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
