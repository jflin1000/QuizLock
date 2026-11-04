package com.lin.quizlock;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {
    private static final String NAME = "quizlock_prefs";
    public static final int DEFAULT_INTERVAL_MIN = 5;
    public static final String DEFAULT_PIN = "0000";

    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static boolean isEnabled(Context c) {
        return sp(c).getBoolean("enabled", true);
    }

    public static void setEnabled(Context c, boolean v) {
        sp(c).edit().putBoolean("enabled", v).apply();
    }

    public static int getIntervalMinutes(Context c) {
        return sp(c).getInt("interval_min", DEFAULT_INTERVAL_MIN);
    }

    public static void setIntervalMinutes(Context c, int minutes) {
        if (minutes < 1) minutes = 1;
        if (minutes > 180) minutes = 180;
        sp(c).edit().putInt("interval_min", minutes).apply();
    }

    public static String getPin(Context c) {
        return sp(c).getString("pin", DEFAULT_PIN);
    }

    public static void setPin(Context c, String pin) {
        sp(c).edit().putString("pin", pin).apply();
    }

    public static boolean checkPin(Context c, String input) {
        return getPin(c).equals(input == null ? "" : input.trim());
    }
}
