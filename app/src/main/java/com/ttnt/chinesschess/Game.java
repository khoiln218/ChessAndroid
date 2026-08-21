package com.ttnt.chinesschess;

import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.ttnt.chinesschess.chess.Board;
import com.ttnt.chinesschess.chess.State;
import com.ttnt.chinesschess.game.ChineseChessGame;
import com.ttnt.chinesschess.graph.BoardTheme;
import com.ttnt.chinesschess.graph.PieceArt;
import com.ttnt.chinesschess.game.TurnTimerView;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

public class Game extends AppCompatActivity implements ChineseChessGame.Listener {

    /** A side that does not move within this many milliseconds loses the game. */
    private static final long TURN_MILLIS = 3 * 60 * 1000L;
    private static final long TICK_MILLIS = 100L;

    ChineseChessGame game;

    private final Handler clock = new Handler(Looper.getMainLooper());
    private AdView adView;
    private View computerSide;
    private View playerSide;
    private TurnTimerView computerTimer;
    private TurnTimerView playerTimer;
    private TextView computerStatus;
    private TextView playerStatus;

    private AlertDialog resultDialog;

    private boolean computerToMove;
    private boolean clockRunning;
    private boolean thinking;
    private long remainingMillis = TURN_MILLIS;
    private long deadline;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            long left = deadline - SystemClock.elapsedRealtime();
            if (left <= 0) {
                clockRunning = false;
                remainingMillis = 0;
                showClock(0);
                game.loseByTimeout();
                return;
            }
            showClock(left);
            clock.postDelayed(this, TICK_MILLIS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game);
        SystemBars.applyInsetsAsPadding(this, findViewById(R.id.game_root));

        int turnGame = 1;
        int levelGame = 1;

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            turnGame = extras.getInt("turn_game");
            levelGame = extras.getInt("level_game");
        }

        if (turnGame == 2) {
            game = new ChineseChessGame(this, levelGame, true);
            loadGame();
        } else {
            game = new ChineseChessGame(this, levelGame, turnGame == 0);
        }

        setUpAds();

        computerSide = findViewById(R.id.computer_side);
        playerSide = findViewById(R.id.player_side);
        computerTimer = findViewById(R.id.computer_timer);
        playerTimer = findViewById(R.id.player_timer);
        computerStatus = findViewById(R.id.computer_status);
        playerStatus = findViewById(R.id.player_status);

        LinearLayout layout = findViewById(R.id.layout);
        layout.addView(game, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        findViewById(R.id.menu_button).setOnClickListener(this::showGameMenu);

        tintAvatars(BoardTheme.load(this));

        game.setListener(this);
        game.start();
    }

    /**
     * Loads the top banner. The slot is resized to the exact height the ad will occupy before the
     * first layout pass, so the placeholder simply gives way to the banner and nothing below it
     * moves - a fixed 50dp box would clip the taller creatives wide screens get served. The ids in
     * strings.xml are Google's test ids for now.
     */
    private void setUpAds() {
        MobileAds.initialize(this);

        FrameLayout slot = findViewById(R.id.ad_slot);
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int slotMarginsDp = 24;
        int widthDp = (int) (metrics.widthPixels / metrics.density) - slotMarginsDp;
        AdSize adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, widthDp);

        ViewGroup.LayoutParams slotParams = slot.getLayoutParams();
        slotParams.height = adSize.getHeightInPixels(this);
        slot.setLayoutParams(slotParams);

        adView = new AdView(this);
        adView.setAdUnitId(getString(R.string.banner_ad_unit_id));
        adView.setAdSize(adSize);
        slot.addView(adView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));
        adView.loadAd(new AdRequest.Builder().build());
    }

    /** Undo and Back live in this overflow menu now, so the board keeps the whole screen. */
    private void showGameMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.inflate(R.menu.game_menu);
        popup.setForceShowIcon(true);
        // Undo rolls back a full round - the player's move and the reply - so it needs both.
        popup.getMenu().findItem(R.id.action_undo).setEnabled(game.board.listUndo.size() > 1);
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_undo) {
                undoGame();
                return true;
            }
            if (id == R.id.action_theme) {
                openThemeDialog();
                return true;
            }
            if (id == R.id.action_back) {
                finish();
                return true;
            }
            return false;
        });
        popup.show();
    }

    /** Repaints the board straight away, so the five materials can be compared on the position. */
    private void openThemeDialog() {
        BoardTheme[] themes = BoardTheme.values();
        new AlertDialog.Builder(this).setTitle(R.string.theme_title)
                .setSingleChoiceItems(BoardTheme.labels(this), BoardTheme.load(this).ordinal(),
                        (dialog, i) -> {
                            themes[i].save(this);
                            game.setTheme(themes[i]);
                            tintAvatars(themes[i]);
                            dialog.dismiss();
                        }).show();
    }

    /**
     * The two generals fronting the side panels are the same pieces that stand on the board, so
     * they are drawn the same way - otherwise a themed set would sit under untouched heads.
     */
    private void tintAvatars(BoardTheme theme) {
        dressAvatar(findViewById(R.id.computer_avatar), theme, true);
        dressAvatar(findViewById(R.id.player_avatar), theme, false);
    }

    /** Puts one side's general on a view, inside a ring struck in that side's own colour. */
    private void dressAvatar(ImageView view, BoardTheme theme, boolean red) {
        view.setImageBitmap(PieceArt.general(getResources(), theme, red,
                getResources().getDimensionPixelSize(R.dimen.avatar_piece)));
        GradientDrawable ring = new GradientDrawable();
        ring.setShape(GradientDrawable.OVAL);
        ring.setColor(ContextCompat.getColor(this, R.color.avatarFill));
        ring.setStroke(Math.round(getResources().getDisplayMetrics().density * 2),
                red ? theme.rimOf(true) : theme.rimOf(false));
        view.setBackground(ring);
    }

    // --- turn clock ---------------------------------------------------------

    @Override
    public void onTurnStarted(boolean computerToMove) {
        this.computerToMove = computerToMove;
        remainingMillis = TURN_MILLIS;
        startClock();
        updateStatus();
    }

    @Override
    public void onThinkingChanged(boolean thinking) {
        this.thinking = thinking;
        computerTimer.setThinking(thinking);
        updateStatus();
    }

    @Override
    public void onGameOver(boolean playerWon, boolean byTimeout) {
        stopClock();
        thinking = false;
        computerTimer.setThinking(false);
        computerStatus.setText(R.string.status_finished);
        playerStatus.setText(R.string.status_finished);
        showClock(remainingMillis);
        highlight(false, false);
        showResultDialog(playerWon, byTimeout);
    }

    /** The end-of-game card: who won, why, and the two ways out of the finished board. */
    private void showResultDialog(boolean playerWon, boolean byTimeout) {
        if (isFinishing() || (resultDialog != null && resultDialog.isShowing())) return;

        View content = getLayoutInflater().inflate(R.layout.dialog_result, null);
        ImageView avatar = content.findViewById(R.id.result_avatar);
        TextView title = content.findViewById(R.id.result_title);
        TextView message = content.findViewById(R.id.result_message);

        // The winner's general fronts the card, in the material the set is currently made of.
        dressAvatar(avatar, BoardTheme.load(this), !playerWon);
        title.setText(playerWon ? R.string.result_win_title : R.string.result_lose_title);
        title.setTextColor(ContextCompat.getColor(this,
                playerWon ? R.color.resultWin : R.color.resultLose));
        if (byTimeout) {
            message.setText(playerWon ? R.string.result_win_timeout
                    : R.string.result_lose_timeout);
        } else {
            message.setText(playerWon ? R.string.result_win_checkmate
                    : R.string.result_lose_checkmate);
        }

        resultDialog = new AlertDialog.Builder(this).setView(content).create();
        if (resultDialog.getWindow() != null) {
            // Let the card's own rounded corners show instead of the default square panel.
            resultDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        content.findViewById(R.id.result_menu).setOnClickListener(v -> {
            resultDialog.dismiss();
            finish();
        });
        content.findViewById(R.id.result_new).setOnClickListener(v -> {
            resultDialog.dismiss();
            game.newGame();
        });
        resultDialog.show();
    }


    /** Runs the clock of whichever side is to move; the idle side keeps its ring dimmed. */
    private void startClock() {
        clock.removeCallbacks(tick);
        deadline = SystemClock.elapsedRealtime() + remainingMillis;
        clockRunning = true;
        showClock(remainingMillis);
        clock.postDelayed(tick, TICK_MILLIS);
    }

    private void stopClock() {
        if (clockRunning) {
            remainingMillis = Math.max(0, deadline - SystemClock.elapsedRealtime());
        }
        clockRunning = false;
        clock.removeCallbacks(tick);
    }

    private void showClock(long left) {
        float fraction = (float) left / TURN_MILLIS;
        String label = formatTime(left);
        String full = formatTime(TURN_MILLIS);
        if (computerToMove) {
            computerTimer.setTime(fraction, label, true);
            playerTimer.setTime(1f, full, false);
        } else {
            playerTimer.setTime(fraction, label, true);
            computerTimer.setTime(1f, full, false);
        }
    }

    private void updateStatus() {
        computerStatus.setText(computerToMove || thinking ? R.string.status_thinking
                : R.string.status_waiting);
        playerStatus.setText(computerToMove ? R.string.status_waiting
                : R.string.status_your_turn);
        highlight(computerToMove, !computerToMove);
    }

    private void highlight(boolean computerActive, boolean playerActive) {
        computerSide.setActivated(computerActive);
        playerSide.setActivated(playerActive);
    }

    private static String formatTime(long millis) {
        long seconds = (millis + 999) / 1000;
        return String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60);
    }

    @Override
    protected void onPause() {
        if (adView != null) {
            adView.pause();
        }
        super.onPause();
        stopClock();
        try {
            saveGame();
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
        }
        if (game != null && !game.isGameOver && !clockRunning && remainingMillis > 0) {
            startClock();
        }
    }

    @Override
    protected void onDestroy() {
        if (resultDialog != null && resultDialog.isShowing()) {
            resultDialog.dismiss();
        }
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
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
            //the move that is now the latest one - or none left at all
            if (game.board.listUndo.isEmpty()) {
                game.clearLastMove();
            } else {
                State last = game.board.listUndo.get(game.board.listUndo.size() - 1);
                game.showLastMove(last.prev, last.curr);
            }
            //reset result
            game.isGameOver = false;
            game.invalidate();
            onTurnStarted(game.board.RED);
        }
    }
}
