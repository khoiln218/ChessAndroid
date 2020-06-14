package com.quanpk.chinesschess;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class Menu extends Activity implements OnClickListener {

    int level = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu);

        Button newGame = (Button) findViewById(R.id.new_button);
        newGame.setOnClickListener(this);
        Button continueView = (Button) findViewById(R.id.continue_button);
        continueView.setOnClickListener(this);
        Button levelView = (Button) findViewById(R.id.level_button);
        levelView.setOnClickListener(this);
        Button aboutView = (Button) findViewById(R.id.about_button);
        aboutView.setOnClickListener(this);
        Button exiView = (Button) findViewById(R.id.exit_button);
        exiView.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.new_button:
                openNewGameDialog();
                break;
            case R.id.continue_button:
                Intent continueGame = new Intent(Menu.this, Game.class);
                continueGame.putExtra("turn_game", 2);
                startActivity(continueGame);
                break;
            case R.id.level_button:
                openLevelDialog();
                break;
            case R.id.about_button:
                Intent about = new Intent(Menu.this, About.class);
                startActivity(about);
                break;
            case R.id.exit_button:
                finish();
                break;

            default:
                break;
        }
    }

    private void openNewGameDialog() {
        new AlertDialog.Builder(this).setTitle(R.string.new_game_title)
                .setItems(R.array.select, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialoginterface, int i) {
                        Intent newGame = new Intent(Menu.this, Game.class);
                        newGame.putExtra("turn_game", i);
                        if (level < 0 || level > 1) level = 1;
                        newGame.putExtra("level_game", level + 1);
                        startActivity(newGame);
                    }
                }).show();
    }

    private void openLevelDialog() {
        new AlertDialog.Builder(this).setTitle(R.string.level_title)
                .setItems(R.array.level, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialoginterface, int i) {
                        level = i;
                    }
                }).show();
    }
}
