package com.ttnt.chinesschess;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class About extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        SystemBars.applyInsetsAsPadding(this, findViewById(R.id.about_root));

        findViewById(R.id.about_button).setOnClickListener(v -> finish());
    }
}
