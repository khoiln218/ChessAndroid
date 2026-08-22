package com.ttnt.chinesschess;

import android.content.Context;
import android.content.SharedPreferences;

import com.ttnt.chinesschess.graph.BoardTheme;

/**
 * Everything the app remembers between sessions, in one place: how strong the machine plays,
 * which side opens, and what the board is made of. All three outlive the game they were chosen
 * for - a choice made once should not have to be made again at every new game - and all three
 * can be changed from either the lobby or the game itself.
 */
public final class Settings {

    private final static String PREFS = "cotuong";
    private final static String KEY_LEVEL = "level";
    private final static String KEY_THEME = "board_theme";
    private final static String KEY_PLAYER_FIRST = "player_first";

    /** The strength the lobby starts on, and what an unspecified level falls back to. */
    public final static int DEFAULT_LEVEL = 2;
    /** Black opens by convention here, and black is the player's side. */
    private final static boolean DEFAULT_PLAYER_FIRST = true;

    private Settings() {
    }

    public static int level(Context context) {
        return prefs(context).getInt(KEY_LEVEL, DEFAULT_LEVEL);
    }

    public static void saveLevel(Context context, int level) {
        prefs(context).edit().putInt(KEY_LEVEL, level).apply();
    }

    /** Whether the player moves first. The machine opens when this is false. */
    public static boolean playerFirst(Context context) {
        return prefs(context).getBoolean(KEY_PLAYER_FIRST, DEFAULT_PLAYER_FIRST);
    }

    public static void savePlayerFirst(Context context, boolean playerFirst) {
        prefs(context).edit().putBoolean(KEY_PLAYER_FIRST, playerFirst).apply();
    }

    /** The saved palette, or the default one if there is none - or if the saved name is stale. */
    public static BoardTheme theme(Context context) {
        String saved = prefs(context).getString(KEY_THEME, BoardTheme.DEFAULT.name());
        for (BoardTheme theme : BoardTheme.values()) {
            if (theme.name().equals(saved)) return theme;
        }
        return BoardTheme.DEFAULT;
    }

    public static void saveTheme(Context context, BoardTheme theme) {
        prefs(context).edit().putString(KEY_THEME, theme.name()).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
