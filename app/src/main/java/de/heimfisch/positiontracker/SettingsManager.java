package de.heimfisch.positiontracker;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.gms.maps.model.LatLng;

public class SettingsManager {
    private static final String PREFS_NAME = "AppSettings";
    private static final String KEY_BACKGROUND_SERVICE = "background_service";
    private static final String KEY_DATA_PUSH_SERVICE = "data_push_service";
    private static final String KEY_UPDATE_DISTANCE = "update_distance";
    private static final String KEY_DARWARICH_HOST = "darwarich_host";
    private static final String KEY_DARWARICH_PORT = "darwarich_port";
    private static final String KEY_DARWARICH_API = "darwarich_api";

    private SharedPreferences sharedPreferences;

    public SettingsManager(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 🚀 Speichert die Stellung des Hintergrunddienst-Switches
     */
    public void setBackgroundServiceEnabled(boolean isEnabled) {
        sharedPreferences.edit().putBoolean(KEY_BACKGROUND_SERVICE, isEnabled).apply();
    }

    /**
     * 📌 Lädt den gespeicherten Zustand des Hintergrunddienst-Switches
     */
    public boolean isBackgroundServiceEnabled() {
        return sharedPreferences.getBoolean(KEY_BACKGROUND_SERVICE, false);
    }

    /**
     * 🚀 Speichert die Stellung des Data-Push-Service-Switches
     */
    public void setDataPushServiceEnabled(boolean isEnabled) {
        sharedPreferences.edit().putBoolean(KEY_DATA_PUSH_SERVICE, isEnabled).apply();
    }

    /**
     * 📌 Lädt den gespeicherten Zustand des Data-Push-Service-Switches
     */
    public boolean isDataPushServiceEnabled() {
        return sharedPreferences.getBoolean(KEY_DATA_PUSH_SERVICE, false);
    }

    /**
     * 🚀 Speichert den DARWARICH_HOST
     */
    public void setDarwarichHost(String host) {
        sharedPreferences.edit().putString(KEY_DARWARICH_HOST, host).apply();
    }

    /**
     * 📌 Liest den gespeicherten DARWARICH_HOST (Standard: leerer String)
     */
    public String getDarwarichHost() {
        return sharedPreferences.getString(KEY_DARWARICH_HOST, "");
    }

    /**
     * 🚀 Speichert den DARWARICH_PORT
     */
    public void setDarwarichPort(String port) {
        sharedPreferences.edit().putString(KEY_DARWARICH_PORT, port).apply();
    }

    /**
     * 📌 Liest den gespeicherten DARWARICH_PORT (Standard: leerer String)
     */
    public String getDarwarichPort() {
        return sharedPreferences.getString(KEY_DARWARICH_PORT, "");
    }

    /**
     * 🚀 Speichert den DARWARICH_API
     */
    public void setDarwarichApi(String api) {
        sharedPreferences.edit().putString(KEY_DARWARICH_API, api).apply();
    }

    /**
     * 📌 Liest den gespeicherten DARWARICH_API (Standard: leerer String)
     */
    public String getDarwarichApi() {
        return sharedPreferences.getString(KEY_DARWARICH_API, "");
    }

    public void setUpdateDistance(String distance)
    {
        sharedPreferences.edit().putString(KEY_UPDATE_DISTANCE, distance).apply();
    }

    public String getUpdateDistance() {
        return sharedPreferences.getString(KEY_UPDATE_DISTANCE, "0");
    }

    public void setLastKnownLocation(double latitude, double longitude) {
        sharedPreferences.edit()
                .putString("last_latitude", String.valueOf(latitude))
                .putString("last_longitude", String.valueOf(longitude))
                .apply();
    }

    public LatLng getLastKnownLocation() {
        String lat = sharedPreferences.getString("last_latitude", "0.0");
        String lon = sharedPreferences.getString("last_longitude", "0.0");
        return new LatLng(Double.parseDouble(lat), Double.parseDouble(lon));
    }
}