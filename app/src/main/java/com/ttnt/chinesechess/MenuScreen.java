package com.ttnt.chinesechess;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ttnt.chinesechess.ai.engine.Algorithm;
import com.ttnt.chinesechess.theme.BoardTheme;
import com.ttnt.chinesechess.theme.PieceArt;

public class MenuScreen extends AppCompatActivity implements OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu);
        SystemBars.applyInsetsAsPadding(this, findViewById(R.id.menu_root));

        for (int id : new int[]{R.id.new_button, R.id.level_button, R.id.algorithm_button,
                R.id.theme_button, R.id.turn_button, R.id.about_button, R.id.exit_button}) {
            findViewById(id).setOnClickListener(this);
        }
        showSeats();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The board can be changed from inside a game, so the lobby re-reads it on the way back.
        showSeats();
    }

    /** Dresses the two seats and writes each setting where it is chosen. */
    private void showSeats() {
        BoardTheme theme = Settings.theme(this);
        PieceArt.dressAvatar(findViewById(R.id.lobby_computer_avatar), theme, true);

        String[] levels = getResources().getStringArray(R.array.level);
        int level = Settings.level(this);
        int index = Math.min(Math.max(level - Settings.DEFAULT_LEVEL, 0), levels.length - 1);
        ((TextView) findViewById(R.id.lobby_level_value)).setText(levels[index]);
        ((TextView) findViewById(R.id.lobby_algorithm_value)).setText(
                getResources().getStringArray(R.array.algorithm)[Settings.algorithm(this).ordinal()]);
        ((TextView) findViewById(R.id.lobby_theme_value)).setText(theme.labelRes);
        ((TextView) findViewById(R.id.lobby_turn_value)).setText(
                Settings.playerFirst(this) ? R.string.first_label : R.string.second_label);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.new_button) {
            // No question to answer on the way in: which side opens was settled in the lobby.
            startActivity(new Intent(MenuScreen.this, GameScreen.class));
        } else if (id == R.id.level_button) {
            openLevelDialog();
        } else if (id == R.id.algorithm_button) {
            openAlgorithmDialog();
        } else if (id == R.id.theme_button) {
            openThemeDialog();
        } else if (id == R.id.turn_button) {
            openTurnDialog();
        } else if (id == R.id.about_button) {
            startActivity(new Intent(MenuScreen.this, AboutScreen.class));
        } else if (id == R.id.exit_button) {
            finish();
        }
    }

    /** The board palette is a setting, not a per-game choice, so it is saved straight away. */
    private void openThemeDialog() {
        BoardTheme[] themes = BoardTheme.values();
        ChoiceDialog.show(this, R.string.theme_title, BoardTheme.labels(this),
                Settings.theme(this).ordinal(), i -> {
                    Settings.saveTheme(this, themes[i]);
                    showSeats();
                });
    }

    private void openLevelDialog() {
        ChoiceDialog.show(this, R.string.level_title,
                getResources().getTextArray(R.array.level),
                Settings.level(this) - Settings.DEFAULT_LEVEL, i -> {
                    Settings.saveLevel(this, i + Settings.DEFAULT_LEVEL);
                    showSeats();
                });
    }

    /** The items are listed in the order of {@link Algorithm}, so the index is the ordinal. */
    private void openAlgorithmDialog() {
        ChoiceDialog.show(this, R.string.algorithm_title,
                getResources().getTextArray(R.array.algorithm),
                Settings.algorithm(this).ordinal(), i -> {
                    Settings.saveAlgorithm(this, Algorithm.values()[i]);
                    showSeats();
                });
    }

    /** The first item is the player opening, which is what {@code true} means to the setting. */
    private void openTurnDialog() {
        ChoiceDialog.show(this, R.string.turn_title,
                getResources().getTextArray(R.array.select),
                Settings.playerFirst(this) ? 0 : 1, i -> {
                    Settings.savePlayerFirst(this, i == 0);
                    showSeats();
                });
    }
}
