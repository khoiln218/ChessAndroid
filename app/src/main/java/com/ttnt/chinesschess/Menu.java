package com.ttnt.chinesschess;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.ttnt.chinesschess.graph.BoardTheme;

public class Menu extends AppCompatActivity implements OnClickListener {

    int level = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu);
        SystemBars.applyInsetsAsPadding(this, findViewById(R.id.menu_root));

        Button newGame = findViewById(R.id.new_button);
        newGame.setOnClickListener(this);
        Button continueView = findViewById(R.id.continue_button);
        continueView.setOnClickListener(this);
        Button levelView = findViewById(R.id.level_button);
        levelView.setOnClickListener(this);
        Button themeView = findViewById(R.id.theme_button);
        themeView.setOnClickListener(this);
        Button aboutView = findViewById(R.id.about_button);
        aboutView.setOnClickListener(this);
        Button exiView = findViewById(R.id.exit_button);
        exiView.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.new_button) {
            openNewGameDialog();
        } else if (id == R.id.continue_button) {
            Intent continueGame = new Intent(Menu.this, Game.class);
            continueGame.putExtra("turn_game", 2);
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
        new AlertDialog.Builder(this).setTitle(R.string.new_game_title)
                .setItems(R.array.select, (dialoginterface, i) -> {
                    Intent newGame = new Intent(Menu.this, Game.class);
                    newGame.putExtra("turn_game", i);
                    newGame.putExtra("level_game", level);
                    startActivity(newGame);
                }).show();
    }

    /** The board palette is a setting, not a per-game choice, so it is saved straight away. */
    private void openThemeDialog() {
        BoardTheme[] themes = BoardTheme.values();
        new AlertDialog.Builder(this).setTitle(R.string.theme_title)
                .setSingleChoiceItems(BoardTheme.labels(this), BoardTheme.load(this).ordinal(),
                        (dialog, i) -> {
                            themes[i].save(this);
                            dialog.dismiss();
                        }).show();
    }

    private void openLevelDialog() {
        new AlertDialog.Builder(this).setTitle(R.string.level_title)
                .setItems(R.array.level, (dialoginterface, i) -> level = i + 2).show();
    }
}
