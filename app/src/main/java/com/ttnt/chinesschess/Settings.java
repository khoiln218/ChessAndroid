package com.ttnt.chinesschess;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * What the app remembers between sessions, other than the board itself - that one belongs to
 * {@link com.ttnt.chinesschess.graph.BoardTheme}, which keeps its own key in the same file.
 */
final class Settings {

    private final static String PREFS = "cotuong";
    private final static String KEY_LEVEL = "level";
    private final static String KEY_GAMES = "games_played";

    /** The strength the lobby starts on, and what an unspecified level falls back to. */
    final static int DEFAULT_LEVEL = 2;

    private Settings() {
    }

    static int level(Context context) {
        return prefs(context).getInt(KEY_LEVEL, DEFAULT_LEVEL);
    }

    static void saveLevel(Context context, int level) {
        prefs(context).edit().putInt(KEY_LEVEL, level).apply();
    }

    static int gamesPlayed(Context context) {
        return prefs(context).getInt(KEY_GAMES, 0);
    }

    /** Counts one more game and hands back the new total. */
    static int bumpGames(Context context) {
        int played = gamesPlayed(context) + 1;
        prefs(context).edit().putInt(KEY_GAMES, played).apply();
        return played;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
