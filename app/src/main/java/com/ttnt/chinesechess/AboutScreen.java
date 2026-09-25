package com.ttnt.chinesechess;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.ttnt.chinesechess.theme.PieceArt;

public class AboutScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        SystemBars.applyInsetsAsPadding(this, findViewById(R.id.about_root));

        PieceArt.dressAvatar(findViewById(R.id.about_avatar), Settings.theme(this), true);
        findViewById(R.id.about_button).setOnClickListener(v -> finish());
    }
}
