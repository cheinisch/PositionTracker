package de.heimfisch.positiontracker.utils;

import android.content.Context;
import android.location.Location;
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
     * Holt die API-URL aus den Einstellungen.
     */
    private String getServerUrl() {
        String hostname = settingsManager.getDarwarichHost();
        String port = settingsManager.getDarwarichPort();
        return "http://" + hostname + ":" + port + "/api/location"; // API-Endpunkt anpassen
    }

    /**
     * Holt den API-Key aus den Einstellungen.
     */
    private String getApiKey() {
        return settingsManager.getDarwarichApi();
    }

    /**
     * Sendet die aktuelle Position an Dawarich. Falls keine Verbindung besteht, wird sie gespeichert.
     */
    public void sendLocation(Location location) {
        if (isInternetAvailable()) {
            sendToServer(location);
        } else {
            // Falls keine Verbindung besteht, speichere die Position in der Queue
            locationQueue.add(location);
            Log.w(TAG, "Keine Verbindung – Position gespeichert.");
        }
    }

    /**
     * Versucht, alle gespeicherten Positionen aus der Queue zu senden, falls Internet wieder verfügbar ist.
     */
    public void retrySendingQueuedLocations() {
        if (!isInternetAvailable()) {
            Log.w(TAG, "Internet noch nicht verfügbar – Warten mit Senden.");
            return;
        }

        while (!locationQueue.isEmpty()) {
            Location location = locationQueue.poll(); // Nächste gespeicherte Position abrufen
            if (location != null) {
                sendToServer(location);
            }
        }
    }

    /**
     * Erstellt das JSON-Format für die Dawarich API.
     */
    private JSONObject createDawarichJson(Location location) {
        JSONObject json = new JSONObject();
        try {
            json.put("_type", "location");
            json.put("t", "u"); // Typ "u" für Update
            json.put("acc", location.getAccuracy()); // Genauigkeit
            json.put("alt", location.hasAltitude() ? location.getAltitude() : 0); // Höhe
            json.put("batt", getBatteryLevel()); // Batteriestand
            json.put("bs", isCharging() ? 1 : 0); // 1 = Lädt, 0 = Nicht geladen
            json.put("lat", location.getLatitude()); // Breitengrad
            json.put("lon", location.getLongitude()); // Längengrad
            json.put("tst", System.currentTimeMillis() / 1000); // Zeitstempel (Unix-Zeit in Sekunden)
            json.put("vel", location.hasSpeed() ? location.getSpeed() : 0); // Geschwindigkeit (m/s)
        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Erstellen des JSON-Objekts: " + e.getMessage());
        }
        return json;
    }

    /**
     * Sendet eine einzelne Position an Dawarich.
     */
    private void sendToServer(Location location) {
        new Thread(() -> {
            try {
                String serverUrl = getServerUrl(); // Hole die aktuelle Server-URL
                String apiKey = getApiKey(); // Hole den API-Key
                JSONObject json = createDawarichJson(location); // JSON erstellen

                Log.d(TAG, "Sende Position an: " + serverUrl);
                Log.d(TAG, "JSON-Daten: " + json.toString());

                // Verbindung zum Server herstellen
                URL url = new URL(serverUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");

                // API-Key hinzufügen
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setDoOutput(true);

                // Daten senden
                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                // Server-Antwort prüfen
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "Position erfolgreich gesendet: " + json.toString());
                } else {
                    Log.e(TAG, "Fehler beim Senden: " + responseCode);
                    locationQueue.add(location); // Falls Fehler, speichere Position erneut
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Fehler beim Senden: " + e.getMessage());
                locationQueue.add(location); // Falls Fehler, speichere Position erneut
            }
        }).start();
    }

    /**
     * Prüft, ob eine Internetverbindung besteht.
     */
    private boolean isInternetAvailable() {
        try {
            URL url = new URL("https://www.google.com");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(3000);
            conn.connect();
            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Holt den aktuellen Batteriestand des Geräts.
     */
    private int getBatteryLevel() {
        BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        if (batteryManager != null) {
            return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        }
        return -1; // Fehlerwert
    }

    /**
     * Prüft, ob das Gerät gerade geladen wird.
     */
    private boolean isCharging() {
        BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        if (batteryManager != null) {
            return batteryManager.isCharging();
        }
        return false;
    }
}
