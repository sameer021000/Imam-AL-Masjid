package com.example.imam_al_masjid;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * ThemeManager handles persisting and applying the Imam's chosen UI theme.
 * On first install, it defaults to the system theme (follow device setting).
 * Once the Imam changes it, it is saved and restored on every subsequent launch.
 */
public class ThemeManager {

    private static final String PREFS_NAME = "imam_app_theme_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";

    // Sentinel value meaning "no preference saved yet, use system default"
    private static final int MODE_NOT_SET = -99;

    /**
     * Call this in Activity.onCreate() before setContentView().
     * Set forceSystem=true for Login/Splash screens to ignore user preference.
     */
    public static void applyTheme(Context context, boolean forceSystem) {
        int targetMode;
        if (forceSystem) {
            targetMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        } else {
            int savedMode = getSavedMode(context);
            if (savedMode == MODE_NOT_SET) {
                targetMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            } else {
                targetMode = savedMode;
            }
        }

        // Only apply if the mode is different from current to avoid infinite recreation loops
        if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
            AppCompatDelegate.setDefaultNightMode(targetMode);
        }
    }

    /**
     * Call this when the Imam selects a specific theme option.
     * Saves the choice and immediately applies it (the activity will recreate).
     */
    public static void setTheme(Context context, int mode) {
        saveMode(context, mode);
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    /**
     * Returns the saved AppCompatDelegate night mode constant, or MODE_NOT_SET if nothing saved yet.
     */
    public static int getSavedMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME_MODE, MODE_NOT_SET);
    }

    /**
     * Returns the current theme selection as a 0-indexed int for UI (0=System, 1=Light, 2=Dark).
     */
    public static int getThemeSelectionIndex(Context context) {
        int savedMode = getSavedMode(context);
        if (savedMode == AppCompatDelegate.MODE_NIGHT_NO) return 1;
        if (savedMode == AppCompatDelegate.MODE_NIGHT_YES) return 2;
        return 0; // Default System
    }

    private static void saveMode(Context context, int mode) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putInt(KEY_THEME_MODE, mode);
        editor.apply();
    }
}