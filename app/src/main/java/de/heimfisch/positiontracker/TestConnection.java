package de.heimfisch.positiontracker;

import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestConnection {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final String TAG = "TestConnection";

    public interface ConnectionCallback {
        void onResult(boolean success);
    }

    public void checkConnection(String hostname, String port, ConnectionCallback callback) {
        executor.execute(() -> {
            boolean isSuccess = false;
            try {
                String urlString = hostname + ":" + port;
                Log.d(TAG, "Teste Verbindung zu: " + urlString);

                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.connect();

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "HTTP-Response-Code: " + responseCode);
                isSuccess = (responseCode == 200);

            } catch (IOException e) {
                Log.e(TAG, "Verbindungsfehler: " + e.getMessage());
            }

            Log.d(TAG, "Callback wird mit Wert: " + isSuccess + "aufgerufen");
            callback.onResult(isSuccess); // Immer das Callback aufrufen!
        });
    }
}
