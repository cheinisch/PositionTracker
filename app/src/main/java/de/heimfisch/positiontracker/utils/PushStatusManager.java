package de.heimfisch.positiontracker.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PushStatusManager {

    private static final String PREF_NAME = "push_status";
    private static final String KEY_NEXT_PUSH_TIME = "next_push_time";

    private final SharedPreferences prefs;

    public PushStatusManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Speichert den Zeitpunkt des nächsten geplanten Pushs (Millisekunden)
    public void setNextPushTime(long timestampMillis) {
        prefs.edit().putLong(KEY_NEXT_PUSH_TIME, timestampMillis).apply();
    }

    // Holt den gespeicherten nächsten Push-Zeitpunkt
    public long getNextPushTime() {
        return prefs.getLong(KEY_NEXT_PUSH_TIME, System.currentTimeMillis());
    }

    // Berechnet verbleibende Sekunden
    public long getRemainingSeconds() {
        long now = System.currentTimeMillis();
        long diff = getNextPushTime() - now;
        return Math.max(0, diff / 1000);
    }
}
