package com.ttnt.chinesschess;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ttnt.chinesschess.graph.BoardTheme;
import com.ttnt.chinesschess.graph.PieceArt;

public class Menu extends AppCompatActivity implements OnClickListener {

    private int level;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu);
        SystemBars.applyInsetsAsPadding(this, findViewById(R.id.menu_root));
        level = Settings.level(this);

        for (int id : new int[]{R.id.new_button, R.id.continue_button, R.id.level_button,
                R.id.theme_button, R.id.about_button, R.id.exit_button}) {
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

    /** Dresses the two seats and writes each one's setting underneath its name. */
    private void showSeats() {
        BoardTheme theme = BoardTheme.load(this);
        PieceArt.dressAvatar(findViewById(R.id.lobby_computer_avatar), theme, true);
        PieceArt.dressAvatar(findViewById(R.id.lobby_player_avatar), theme, false);

        String[] levels = getResources().getStringArray(R.array.level);
        int index = Math.min(Math.max(level - Settings.DEFAULT_LEVEL, 0), levels.length - 1);
        ((TextView) findViewById(R.id.lobby_level_value)).setText(levels[index]);
        ((TextView) findViewById(R.id.lobby_theme_value)).setText(theme.labelRes);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.new_button) {
            openNewGameDialog();
        } else if (id == R.id.continue_button) {
            Intent continueGame = new Intent(Menu.this, Game.class);
            continueGame.putExtra("turn_game", 2);
            continueGame.putExtra("level_game", level);
            startActivity(continueGame);
        } else if (id == R.id.level_button) {
            openLevelDialog();
        } else if (id == R.id.theme_button) {
            openThemeDialog();
        } else if (id == R.id.about_button) {
            Intent about = new Intent(Menu.this, About.class);
            startActivity(about);
        } else if (id == R.id.exit_button) {
            finish();
        }
    }

    private void openNewGameDialog() {
        ChoiceDialog.show(this, R.string.new_game_title,
                getResources().getTextArray(R.array.select), -1, i -> {
                    Intent newGame = new Intent(Menu.this, Game.class);
                    newGame.putExtra("turn_game", i);
                    newGame.putExtra("level_game", level);
                    startActivity(newGame);
                });
    }

    /** The board palette is a setting, not a per-game choice, so it is saved straight away. */
    private void openThemeDialog() {
        BoardTheme[] themes = BoardTheme.values();
        ChoiceDialog.show(this, R.string.theme_title, BoardTheme.labels(this),
                BoardTheme.load(this).ordinal(), i -> {
                    themes[i].save(this);
                    showSeats();
                });
    }

    private void openLevelDialog() {
        ChoiceDialog.show(this, R.string.level_title,
                getResources().getTextArray(R.array.level),
                level - Settings.DEFAULT_LEVEL, i -> {
                    level = i + 2;
                    Settings.saveLevel(this, level);
                    showSeats();
                });
    }
}
