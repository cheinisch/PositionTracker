package de.heimfisch.positiontracker.utils;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.Queue;
import de.heimfisch.positiontracker.SettingsManager;

public class DataPushService extends Service {

    private static final String TAG = "DataPushService";
    //private static final long PUSH_INTERVAL = 300000; // 5 Minuten in Millisekunden
    private static final long PUSH_INTERVAL = 30000; // 5 Minuten in Millisekunden
    private final Queue<Location> locationQueue = new LinkedList<>();
    private final Handler handler = new Handler();
    private long lastPushTime = System.currentTimeMillis();
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public DataPushService getService() {
            return DataPushService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "DataPushService gestartet.");
        handler.postDelayed(pushRunnable, PUSH_INTERVAL);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand aufgerufen, Timer neu gestartet.");
        handler.postDelayed(pushRunnable, PUSH_INTERVAL);
        return START_STICKY;
    }

    private final Runnable pushRunnable = new Runnable() {
        @Override
        public void run() {
            pushData();
            handler.postDelayed(this, PUSH_INTERVAL);
        }
    };

    public void addLocation(Location location) {
        locationQueue.add(location);
    }

    public long getRemainingTime() {
        long elapsedTime = System.currentTimeMillis() - lastPushTime;
        return Math.max(PUSH_INTERVAL - elapsedTime, 0);
    }

    public int getPendingPointsCount() {
        return locationQueue.size();
    }

    private void pushData() {
        if (!isInternetAvailable()) {
            return;
        }
        while (!locationQueue.isEmpty()) {
            sendToServer(locationQueue.poll());
        }
        lastPushTime = System.currentTimeMillis();
    }

    private void sendToServer(Location location) {
        new Thread(() -> {
            try {
                String serverUrl = getServerUrl();
                String apiKey = getApiKey();
                JSONObject json = createDawarichJson(location);

                Log.d(TAG, "Sende Position an Server: " + serverUrl);
                Log.d(TAG, "JSON-Daten: " + json.toString());

                URL url = new URL(serverUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "Position erfolgreich gesendet: " + json.toString());
                } else {
                    Log.e(TAG, "Fehler beim Senden: HTTP-Code " + responseCode);
                    locationQueue.add(location);
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Fehler beim Senden: " + e.getMessage());
                locationQueue.add(location);
            }
        }).start();
    }


    private JSONObject createDawarichJson(Location location) {
        JSONObject json = new JSONObject();
        try {
            json.put("_type", "location");
            json.put("lat", location.getLatitude());
            json.put("lon", location.getLongitude());
            json.put("tst", System.currentTimeMillis() / 1000);
        } catch (Exception ignored) {}
        return json;
    }

    private String getServerUrl() {
        return "http://" + new SettingsManager(getApplicationContext()).getDarwarichHost() + ":" +
                new SettingsManager(getApplicationContext()).getDarwarichPort() + "/api/location";
    }

    private String getApiKey() {
        return new SettingsManager(getApplicationContext()).getDarwarichApi();
    }

    private boolean isInternetAvailable() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL("https://www.google.com").openConnection();
            conn.setConnectTimeout(3000);
            conn.connect();
            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
