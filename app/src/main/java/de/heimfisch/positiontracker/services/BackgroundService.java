package de.heimfisch.positiontracker.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import de.heimfisch.positiontracker.MainActivity;
import de.heimfisch.positiontracker.R;

public class BackgroundService extends Service {
    private static final String TAG = "BackgroundService";
    private static final String CHANNEL_ID = "PositionTrackerChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final long CHECK_INTERVAL = 60000; // 1 Minute
    private final Handler handler = new Handler();

    private final Runnable checkSettingsRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Prüfe Einstellungen für den Hintergrunddienst...");
            checkSettingsAndStopIfNeeded();
            restoreNotification();
            handler.postDelayed(this, CHECK_INTERVAL);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Hintergrunddienst gestartet.");
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, getNotification());

        handler.postDelayed(checkSettingsRunnable, CHECK_INTERVAL);
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
        handler.removeCallbacks(checkSettingsRunnable);
        Log.d(TAG, "Hintergrunddienst gestoppt.");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Erstellt einen Notification-Kanal (ab Android 8.0 erforderlich).
     */
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

    /**
     * Erstellt die permanente "Sticky" Notification.
     */
    private Notification getNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("PositionTracker läuft")
                .setContentText("Der Hintergrunddienst ist aktiv und kann nicht entfernt werden.")
                .setSmallIcon(R.drawable.ic_notify_icon) // Ersetze mit deinem Icon
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();
    }

    /**
     * Stellt sicher, dass die Notification weiterhin aktiv ist.
     */
    private void restoreNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            boolean isNotificationActive = isNotificationVisible(notificationManager);
            if (!isNotificationActive) {
                Log.d(TAG, "Notification wurde gelöscht – Wiederherstellung.");
                startForeground(NOTIFICATION_ID, getNotification());
            }
        }
    }

    /**
     * Prüft, ob die Notification aktiv ist.
     */
    private boolean isNotificationVisible(NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (StatusBarNotification sbn : notificationManager.getActiveNotifications()) {
                if (sbn.getNotification().getChannelId().equals(CHANNEL_ID)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Prüft die Einstellungen und stoppt den Service, falls er deaktiviert wurde.
     */
    private void checkSettingsAndStopIfNeeded() {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean isTrackingEnabled = prefs.getBoolean("background_service_enabled", true);

        if (!isTrackingEnabled) {
            Log.d(TAG, "Hintergrunddienst wurde deaktiviert. Stoppe den Service...");
            stopSelf();
        }
    }
}
