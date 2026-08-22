package com.ttnt.chinesschess;

import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.ttnt.chinesschess.chess.Board;
import com.ttnt.chinesschess.chess.State;
import com.ttnt.chinesschess.game.ChineseChessGame;
import com.ttnt.chinesschess.game.TurnTimerView;
import com.ttnt.chinesschess.graph.BoardTheme;
import com.ttnt.chinesschess.graph.PieceArt;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

public class Game extends AppCompatActivity implements ChineseChessGame.Listener {

    /**
     * Set when the lobby is carrying on the saved game rather than starting a fresh one.
     */
    public final static String EXTRA_RESUME = "resume_game";

    /**
     * A side that does not move within this many milliseconds loses the game.
     */
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

        // Strength and who opens are settings, read here rather than carried in: the lobby is
        // where they are chosen, and the only thing this screen needs told is which of the two
        // ways in was taken.
        boolean resume = getIntent().getBooleanExtra(EXTRA_RESUME, false);
        int levelGame = Settings.level(this);

        if (resume) {
            game = new ChineseChessGame(this, levelGame, true);
            loadGame();
        } else {
            game = new ChineseChessGame(this, levelGame, Settings.playerFirst(this));
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
        findViewById(R.id.settings_button).setOnClickListener(this::showSettingsMenu);
        findViewById(R.id.river_new_game).setOnClickListener(v -> startNewGame());

        tintAvatars(Settings.theme(this));
        showLevel(levelGame);
        // Carrying on a saved game is not a new one, so only a fresh start counts.

        game.setListener(this);
        // A restored game is picked up where it was left, so it carries straight on. A new one is
        // only laid out: coming through the door is not the same as sitting down to play, and
        // nothing runs until the player says they are ready.
        if (resume) {
            game.start();
        } else {
            awaitReady();
        }
    }

    /**
     * Loads the top banner. The slot is resized to the exact height the ad will occupy before the
     * first layout pass, so the placeholder simply gives way to the banner and nothing below it
     * moves - a fixed 50dp box would clip the taller creatives wide screens get served. The ids in
     * strings.xml are Google's test ids for now.
     */
    @SuppressWarnings("deprecation")
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

    /**
     * Undo and Back live in this overflow menu now, so the board keeps the whole screen.
     */
    private void showGameMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.inflate(R.menu.game_menu);
        popup.setForceShowIcon(true);
        // Undo rolls back a full round - the player's move and the reply - so it needs both.
        MenuItem undo = popup.getMenu().findItem(R.id.action_undo);
        undo.setEnabled(game.board.listUndo.size() > 1);
        matchIconToLabel(undo);
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_undo) {
                undoGame();
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

    /**
     * Everything the lobby lets you set, gathered on the machine's own card: how hard it plays,
     * whether it opens, and what the board is made of. All three are remembered, so what is
     * chosen here is what the lobby shows next time.
     */
    private void showSettingsMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.inflate(R.menu.settings_menu);
        popup.setForceShowIcon(true);
        for (int i = 0; i < popup.getMenu().size(); i++) {
            matchIconToLabel(popup.getMenu().getItem(i));
        }
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_level) {
                openLevelDialog();
                return true;
            }
            if (id == R.id.action_turn) {
                openTurnDialog();
                return true;
            }
            if (id == R.id.action_theme) {
                openThemeDialog();
                return true;
            }
            return false;
        });
        popup.show();
    }

    /**
     * A new strength applies from the next move, so it can be turned up mid-game.
     */
    private void openLevelDialog() {
        ChoiceDialog.show(this, R.string.level_title,
                getResources().getTextArray(R.array.level),
                Settings.level(this) - Settings.DEFAULT_LEVEL, i -> {
                    int level = i + Settings.DEFAULT_LEVEL;
                    Settings.saveLevel(this, level);
                    game.setLevel(level);
                    showLevel(level);
                });
    }

    /**
     * Which side opens cannot change a game already under way; it takes the next one.
     */
    private void openTurnDialog() {
        ChoiceDialog.show(this, R.string.turn_title,
                getResources().getTextArray(R.array.select),
                Settings.playerFirst(this) ? 0 : 1,
                i -> Settings.savePlayerFirst(this, i == 0));
    }

    /**
     * Repaints the board straight away, so the five materials can be compared on the position.
     */
    private void openThemeDialog() {
        BoardTheme[] themes = BoardTheme.values();
        ChoiceDialog.show(this, R.string.theme_title, BoardTheme.labels(this),
                Settings.theme(this).ordinal(), i -> {
                    Settings.saveTheme(this, themes[i]);
                    game.setTheme(themes[i]);
                    tintAvatars(themes[i]);
                });
    }


    /**
     * Names the strength the machine is playing at, beside its own name on its panel.
     */
    private void showLevel(int level) {
        String[] levels = getResources().getStringArray(R.array.level);
        // The lobby numbers its levels from 2; anything outside the list falls back to the first.
        int index = Math.min(Math.max(level - Settings.DEFAULT_LEVEL, 0), levels.length - 1);
        ((TextView) findViewById(R.id.computer_level)).setText(levels[index]);
    }

    /**
     * The two generals fronting the side panels are the same pieces that stand on the board, so
     * they are drawn the same way - otherwise a themed set would sit under untouched heads.
     */
    private void tintAvatars(BoardTheme theme) {
        dressAvatar(findViewById(R.id.computer_avatar), theme, true);
        dressAvatar(findViewById(R.id.player_avatar), theme, false);
    }

    /**
     * Puts one side's general on a view, inside a ring struck in that side's own colour.
     */
    private void dressAvatar(ImageView view, BoardTheme theme, boolean red) {
        PieceArt.dressAvatar(view, theme, red);
    }

    /**
     * A disabled menu item greys its label but leaves its icon at full strength, so the row reads
     * as half switched off. This paints the icon in the colour the label is actually using - read
     * off the theme rather than guessed, so the two always agree.
     */
    private void matchIconToLabel(MenuItem item) {
        Drawable icon = item.getIcon();
        if (icon == null || item.isEnabled()) return;
        TypedArray styled = obtainStyledAttributes(new int[]{android.R.attr.textColorPrimary});
        ColorStateList labelColors = styled.getColorStateList(0);
        styled.recycle();
        if (labelColors == null) return;
        int disabled = labelColors.getColorForState(new int[]{-android.R.attr.state_enabled},
                labelColors.getDefaultColor());
        Drawable tinted = DrawableCompat.wrap(icon.mutate());
        DrawableCompat.setTint(tinted, disabled);
        item.setIcon(tinted);
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
    public void onGameOver(boolean playerWon, ChineseChessGame.End how) {
        stopClock();
        thinking = false;
        computerTimer.setThinking(false);
        computerStatus.setText(R.string.status_finished);
        playerStatus.setText(R.string.status_finished);
        showClock(remainingMillis);
        highlight(false, false);
        showResultDialog(playerWon, how);
    }

    /**
     * The end-of-game card: who won, why, and the two ways out of the finished board.
     */
    private void showResultDialog(boolean playerWon, ChineseChessGame.End how) {
        if (isFinishing() || (resultDialog != null && resultDialog.isShowing())) return;

        View content = getLayoutInflater().inflate(R.layout.dialog_result, null);
        ImageView avatar = content.findViewById(R.id.result_avatar);
        TextView title = content.findViewById(R.id.result_title);
        TextView message = content.findViewById(R.id.result_message);

        boolean draw = how == ChineseChessGame.End.REPETITION_DRAW;
        // The winner's general fronts the card, in the material the set is currently made of.
        // A draw has no winner, so the player's own general stands there instead.
        dressAvatar(avatar, Settings.theme(this), !draw && !playerWon);
        title.setText(draw ? R.string.result_draw_title
                : playerWon ? R.string.result_win_title : R.string.result_lose_title);
        title.setTextColor(ContextCompat.getColor(this, draw ? R.color.textMuted
                : playerWon ? R.color.resultWin : R.color.resultLose));
        message.setText(switch (how) {
            case TIMEOUT -> playerWon ? R.string.result_win_timeout : R.string.result_lose_timeout;
            case PERPETUAL_CHECK -> playerWon ? R.string.result_win_perpetual_check
                    : R.string.result_lose_perpetual_check;
            case PERPETUAL_CHASE -> playerWon ? R.string.result_win_perpetual_chase
                    : R.string.result_lose_perpetual_chase;
            case REPETITION_DRAW -> R.string.result_draw_repetition;
            default -> playerWon ? R.string.result_win_checkmate : R.string.result_lose_checkmate;
        });

        resultDialog = new AlertDialog.Builder(this).setView(content).create();
        if (resultDialog.getWindow() != null) {
            // Let the card's own rounded corners show instead of the default square panel.
            resultDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        content.findViewById(R.id.result_menu).setOnClickListener(v -> {
            resultDialog.dismiss();
            finish();
        });
        content.findViewById(R.id.result_new).setOnClickListener(v -> resultDialog.dismiss());
        // Waving the card away is not the same as answering it: the game is still over, so the
        // way into the next one has to stay somewhere on screen.
        resultDialog.setOnDismissListener(d -> {
            if (game.isGameOver && !isFinishing()) showRiverButton(R.string.ready_label);
            else hideRiverButton();
        });
        resultDialog.show();
    }


    /**
     * The board set out with nobody on the clock and nothing accepted, and the one button that
     * begins it. Which side opens is read now rather than at the tap, so the panels behind the
     * button already show the game that is about to be played.
     */
    private void awaitReady() {
        computerToMove = !game.turn;
        remainingMillis = TURN_MILLIS;
        showClock(remainingMillis);
        updateStatus();
        showRiverButton(R.string.ready_label);
    }

    private void startNewGame() {
        hideRiverButton();
        game.newGame(Settings.playerFirst(this));
        playOpeningFlourish(game::start);
    }

    /**
     * The word brushed across the board as a game opens: in on an overshoot, a beat to be read,
     * then away on a swell, the way a stamp is lifted off the paper. It sits above the board and
     * is put back out of the way at the end, so it never stands between a finger and a piece.
     *
     * <p>The game does not exist yet while it shows: {@code begin} is what starts it, and until
     * that runs no clock ticks, no piece can be picked up and the machine does not think. A turn
     * spent reading an announcement is not a turn spent playing, and on a short clock it would
     * be most of one.
     */
    private void playOpeningFlourish(Runnable begin) {
        View flourish = findViewById(R.id.board_flourish);
        stopClock();
        // Both clocks stand full behind the word, on the sides the game is about to use.
        computerToMove = !game.turn;
        remainingMillis = TURN_MILLIS;
        showClock(remainingMillis);
        // The panels still carry the last game's words until the new one is under way.
        updateStatus();
        flourish.animate().cancel();
        flourish.setAlpha(0f);
        flourish.setScaleX(0.55f);
        flourish.setScaleY(0.55f);
        flourish.setVisibility(View.VISIBLE);
        flourish.animate()
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(420)
                .setInterpolator(new OvershootInterpolator(1.6f))
                .withEndAction(() -> flourish.animate()
                        .alpha(0f).scaleX(1.35f).scaleY(1.35f)
                        .setStartDelay(560)
                        .setDuration(420)
                        .setInterpolator(new AccelerateInterpolator())
                        .withEndAction(() -> {
                            flourish.setVisibility(View.GONE);
                            if (!isFinishing()) begin.run();
                        })
                        .start())
                .start();
    }

    /** Puts the button on the river under the given word, and leaves it there for a tap. */
    private void showRiverButton(int textRes) {
        Button button = findViewById(R.id.river_new_game);
        button.setText(textRes);
        button.setVisibility(View.VISIBLE);
    }

    private void hideRiverButton() {
        findViewById(R.id.river_new_game).setVisibility(View.GONE);
    }

    /**
     * Runs the clock of whichever side is to move; the idle side keeps its ring dimmed.
     */
    private void startClock() {
        clock.removeCallbacks(tick);
        // The one gate every route to the clock passes through - a turn started, the screen
        // coming back to the front - so a board that is only waiting to be begun never ticks.
        if (!game.isStarted()) return;
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
                    f.write(game.board.redToMove ? 1 : 0);
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
                    game.board.redToMove = reader.read() == 1;
                    game.board.resyncHistory();
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
            game.board.prevMove = pos.from;
            game.board.currMove = pos.to;
            game.board.cell[pos.from.x][pos.from.y] = pos.piece;
            game.board.cell[pos.to.x][pos.to.y] = pos.captured;
            //undo computer
            pos = game.board.listUndo.get(game.board.listUndo.size() - 1);
            game.board.listUndo.remove(game.board.listUndo.size() - 1);
            game.board.prevMove = pos.from;
            game.board.currMove = pos.to;
            game.board.cell[pos.from.x][pos.from.y] = pos.piece;
            game.board.cell[pos.to.x][pos.to.y] = pos.captured;
            // Both halves of the round come off the repetition record too, or the position
            // would be judged against a history that no longer matches the board.
            game.board.undo();
            game.board.undo();
            //the move that is now the latest one - or none left at all
            if (game.board.listUndo.isEmpty()) {
                game.clearLastMove();
            } else {
                State last = game.board.listUndo.get(game.board.listUndo.size() - 1);
                game.showLastMove(last.from, last.to);
            }
            //reset result
            game.isGameOver = false;
            game.invalidate();
            onTurnStarted(game.board.redToMove);
        }
    }
}
