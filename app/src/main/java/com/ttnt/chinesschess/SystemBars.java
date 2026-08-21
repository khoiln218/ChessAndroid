package com.ttnt.chinesschess;

import android.app.Activity;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * From Android 15 on, an app targeting SDK 35 is laid out edge to edge, so a NoActionBar layout
 * runs underneath the status and navigation bars unless it pads itself. Below API 35 the decor
 * view still consumes the insets first, so the listener receives zeros and this is a no-op.
 */
final class SystemBars {

    private SystemBars() {
    }

    static void applyInsetsAsPadding(Activity activity, View root) {
        // The theme is light, so the bar icons have to be drawn dark to stay readable.
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(activity.getWindow(), root);
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
