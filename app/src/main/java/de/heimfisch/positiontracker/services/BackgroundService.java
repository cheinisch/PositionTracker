package de.heimfisch.positiontracker.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import de.heimfisch.positiontracker.MainActivity;
import de.heimfisch.positiontracker.R;
import de.heimfisch.positiontracker.SettingsManager;
import de.heimfisch.positiontracker.utils.DataPush;
import de.heimfisch.positiontracker.utils.GetLocation;

public class BackgroundService extends Service {

    private static final String TAG = "BackgroundService";
    private static final String CHANNEL_ID = "PositionTrackerChannel";
    private static final int NOTIFICATION_ID = 1;

    private final Handler handler = new Handler();
    private Location lastLocation = null;
    private DataPush dataPush;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Hintergrunddienst gestartet.");
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, getNotification());

        dataPush = new DataPush(getApplicationContext());
        handler.post(movementCheckRunnable); // Starte mit Bewegungserkennung
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        checkSettingsAndStopIfNeeded();
        restoreNotification();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(movementCheckRunnable);
        handler.removeCallbacks(sendPositionRunnable);
        Log.d(TAG, "Hintergrunddienst gestoppt.");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Phase 1: Alle 60s Bewegung prüfen
    private final Runnable movementCheckRunnable = new Runnable() {
        @Override
        public void run() {
            checkSettingsAndStopIfNeeded();
            restoreNotification();

            Log.d(TAG, "Bewegungserkennung läuft...");
            GetLocation locationProvider = new GetLocation(getApplicationContext());
            locationProvider.requestLocation(new GetLocation.LocationResultCallback() {
                @Override
                public void onLocationSuccess(Location location) {
                    if (checkDistance(location)) {
                        Log.d(TAG, "Bewegung erkannt – starte Positionsübertragung.");
                        handler.removeCallbacks(movementCheckRunnable);
                        handler.post(sendPositionRunnable);
                    }
                    locationProvider.stopLocationUpdates();
                }

                @Override
                public void onLocationError(String error) {
                    Log.e(TAG, "Fehler bei Bewegungserkennung: " + error);
                    locationProvider.stopLocationUpdates();
                }
            });

            handler.postDelayed(this, 60 * 1000L);
        }
    };

    // Phase 2: Regelmäßig Position senden
    private final Runnable sendPositionRunnable = new Runnable() {
        @Override
        public void run() {
            checkSettingsAndStopIfNeeded();
            restoreNotification();

            Log.d(TAG, "Sende aktuelle Position...");
            GetLocation locationProvider = new GetLocation(getApplicationContext());
            locationProvider.requestLocation(new GetLocation.LocationResultCallback() {
                @Override
                public void onLocationSuccess(Location location) {
                    sendCurrentPosition(location);
                    locationProvider.stopLocationUpdates();
                }

                @Override
                public void onLocationError(String error) {
                    Log.e(TAG, "Fehler beim Senden der Position: " + error);
                    locationProvider.stopLocationUpdates();
                }
            });

            long interval = getIntervalSettingInSeconds() * 1000L;
            handler.postDelayed(this, interval);
        }
    };

    private boolean checkDistance(Location currentLocation) {
        int movementThreshold = 60;

        try {
            SettingsManager settingsManager = new SettingsManager(getApplicationContext());
            String minDistanceStr = settingsManager.getMinimumDistance();
            if (minDistanceStr != null && !minDistanceStr.trim().isEmpty()) {
                movementThreshold = Integer.parseInt(minDistanceStr);
            }
        } catch (NumberFormatException e) {
            Log.w(TAG, "Ungültige Mindestbewegung – fallback auf 60m");
        }

        if (lastLocation == null) {
            lastLocation = currentLocation;
            Log.d(TAG, "Kein vorheriger Standort – speichere ersten Punkt.");
            return false;
        }

        float distance = currentLocation.distanceTo(lastLocation);
        Log.d(TAG, "Distanz zur letzten Position: " + distance + " Meter");

        if (distance >= movementThreshold) {
            Log.d(TAG, "Bewegung erkannt (" + distance + " m > " + movementThreshold + " m)");
            lastLocation = currentLocation;
            return true;
        }

        Log.d(TAG, "Noch nicht genug bewegt – keine Übertragung.");
        return false;
    }

    private void sendCurrentPosition(Location location) {
        Log.d(TAG, "Sende aktuelle Position: " + location.getLatitude() + ", " + location.getLongitude());
        dataPush.sendLocation(location);
        lastLocation = location;
    }

    private long getIntervalSettingInSeconds() {
        long interval = 60; // default

        try {
            SettingsManager settingsManager = new SettingsManager(getApplicationContext());
            String intervalStr = settingsManager.getUpdateDistanceTime();
            if (intervalStr != null && !intervalStr.trim().isEmpty()) {
                interval = Long.parseLong(intervalStr);
            }
        } catch (NumberFormatException e) {
            Log.w(TAG, "Ungültiges Intervall – fallback auf 60s");
        }

        return interval;
    }

    private void checkSettingsAndStopIfNeeded() {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean isTrackingEnabled = prefs.getBoolean("background_service_enabled", true);

        if (!isTrackingEnabled) {
            Log.d(TAG, "Tracking deaktiviert. Stoppe Hintergrunddienst.");
            stopSelf();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "PositionTracker Hintergrunddienst",
                    NotificationManager.IMPORTANCE_HIGH
            );
            serviceChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            serviceChannel.setDescription("Dieser Dienst läuft dauerhaft im Hintergrund.");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification getNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("PositionTracker läuft")
                .setContentText("Der Hintergrunddienst ist aktiv und prüft deine Position.")
                .setSmallIcon(R.drawable.ic_notify_icon)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();
    }

    private void restoreNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean isVisible = false;
            for (android.service.notification.StatusBarNotification sbn : notificationManager.getActiveNotifications()) {
                if (sbn.getNotification().getChannelId().equals(CHANNEL_ID)) {
                    isVisible = true;
                    break;
                }
            }
            if (!isVisible) {
                Log.d(TAG, "Notification war nicht sichtbar – wird wiederhergestellt.");
                startForeground(NOTIFICATION_ID, getNotification());
            }
        }
    }
}
