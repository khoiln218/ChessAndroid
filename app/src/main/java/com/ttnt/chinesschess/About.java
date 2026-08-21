package com.ttnt.chinesschess;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.ttnt.chinesschess.graph.BoardTheme;
import com.ttnt.chinesschess.graph.PieceArt;

public class About extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        SystemBars.applyInsetsAsPadding(this, findViewById(R.id.about_root));

        PieceArt.dressAvatar(findViewById(R.id.about_avatar), BoardTheme.load(this), true);
        findViewById(R.id.about_button).setOnClickListener(v -> finish());
    }
}
