package de.heimfisch.positiontracker.utils;

import android.content.Context;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.util.Log;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.Queue;

import de.heimfisch.positiontracker.SettingsManager;

public class DataPush {

    private static final String TAG = "DataPush";
    private final Context context;
    private final SettingsManager settingsManager;
    private final Queue<Location> locationQueue = new LinkedList<>();

    public DataPush(Context context) {
        this.context = context;
        this.settingsManager = new SettingsManager(context);
    }

    /**
     * Speichert oder sendet den Standort je nach Internetverfügbarkeit.
     */
    public void sendLocation(Location location) {
        if (isInternetAvailable()) {
            Log.d(TAG, "Internet verfügbar, sende Standort...");
            sendToServer(location);
        } else {
            Log.w(TAG, "Keine Internetverbindung – speichere Standort in Warteschlange.");
            locationQueue.add(location);
        }
    }

    /**
     * Prüft, ob gespeicherte Standorte gesendet werden können.
     */
    public void retrySendingQueuedLocations() {
        if (!isInternetAvailable()) {
            Log.w(TAG, "Kein Internet – gespeicherte Standorte bleiben in Warteschlange.");
            return;
        }

        while (!locationQueue.isEmpty()) {
            Location location = locationQueue.poll();
            if (location != null) {
                sendToServer(location);
            }
        }
    }

    /**
     * Erstellt das JSON-Format für die API.
     */
    private JSONObject createJson(Location location) {
        JSONObject json = new JSONObject();
        try {
            json.put("_type", "location");
            json.put("lat", location.getLatitude());
            json.put("lon", location.getLongitude());
            json.put("tst", System.currentTimeMillis() / 1000);
            json.put("acc", location.getAccuracy());
            json.put("alt", location.hasAltitude() ? location.getAltitude() : 0);
            json.put("batt", getBatteryLevel());
            json.put("bs", isCharging() ? 1 : 0);
            json.put("vel", location.hasSpeed() ? location.getSpeed() : 0);
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Erstellen des JSON-Objekts: " + e.getMessage());
        }
        return json;
    }

    /**
     * Sendet eine Position an den Server.
     */
    private void sendToServer(Location location) {
        new Thread(() -> {
            try {
                String serverUrl = getServerUrl();
                String apiKey = getApiKey();
                JSONObject json = createJson(location);

                String requestUrl = serverUrl + "?api_key=" + apiKey;

                Log.d(TAG, "Sende Standort an Server: " + requestUrl);
                Log.d(TAG, "JSON-Daten: " + json.toString());

                URL url = new URL(requestUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                // Daten senden
                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                // Server-Antwort prüfen
                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Server-Antwort: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "Standort erfolgreich gesendet.");
                } else {
                    Log.e(TAG, "Fehler beim Senden! HTTP-Code: " + responseCode);
                    locationQueue.add(location);
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Fehler beim Senden: " + e.getMessage());
                locationQueue.add(location);
            }
        }).start();
    }

    /**
     * Prüft, ob eine Internetverbindung besteht.
     */
    private boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
                return capabilities != null &&
                        (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
            } else {
                return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
            }
        }
        return false;
    }

    /**
     * Holt die API-URL aus den Einstellungen.
     */
    private String getServerUrl() {
        return settingsManager.getDarwarichHost() + ":" + settingsManager.getDarwarichPort() + "/api/v1/owntracks/points";
    }

    /**
     * Holt den API-Key aus den Einstellungen.
     */
    private String getApiKey() {
        return settingsManager.getDarwarichApi();
    }

    /**
     * Holt den aktuellen Batteriestand.
     */
    private int getBatteryLevel() {
        BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        return (batteryManager != null) ? batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) : -1;
    }

    /**
     * Prüft, ob das Gerät geladen wird.
     */
    private boolean isCharging() {
        BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        return batteryManager != null && batteryManager.isCharging();
    }
}
