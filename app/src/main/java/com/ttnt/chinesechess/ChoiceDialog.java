package com.ttnt.chinesechess;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

/**
 * A list to pick from, wearing the same card as the result dialog - gold rule, gold title, the
 * chosen row ticked in gold. The stock AlertDialog list looked like a different app every time
 * one opened over the board.
 */
final class ChoiceDialog {

    interface OnPick {
        void pick(int index);
    }

    private ChoiceDialog() {
    }

    /** {@code checked} is the row to tick, or -1 when nothing is chosen yet. */
    static void show(Activity host, int titleRes, CharSequence[] items, int checked,
                     OnPick onPick) {
        LayoutInflater inflater = host.getLayoutInflater();
        View content = inflater.inflate(R.layout.dialog_choice, null);
        ((TextView) content.findViewById(R.id.choice_title)).setText(titleRes);
        LinearLayout list = content.findViewById(R.id.choice_items);

        AlertDialog dialog = new AlertDialog.Builder(host).setView(content).create();
        if (dialog.getWindow() != null) {
            // Let the card's own rounded corners show instead of the default square panel.
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        for (int i = 0; i < items.length; i++) {
            TextView row = (TextView) inflater.inflate(R.layout.item_choice, list, false);
            row.setText(items[i]);
            boolean picked = i == checked;
            row.setTextColor(ContextCompat.getColor(host,
                    picked ? R.color.gold : R.color.textMuted));
            row.setTypeface(null, picked ? Typeface.BOLD : Typeface.NORMAL);
            row.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0,
                    picked ? R.drawable.ic_check : 0, 0);
            final int index = i;
            row.setOnClickListener(v -> {
                dialog.dismiss();
                onPick.pick(index);
            });
            list.addView(row);
        }
        dialog.show();
    }
}
