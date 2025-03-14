package de.heimfisch.positiontracker.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.*;

import de.heimfisch.positiontracker.SettingsManager;

public class GetLocation {

    private static final String TAG = "GetLocation";
    private final Context context;
    private final FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private final SettingsManager settingsManager;

    public interface LocationResultCallback {
        void onLocationSuccess(Location location);
        void onLocationError(String error);
    }

    public GetLocation(Context context) {
        this.context = context;
        this.settingsManager = new SettingsManager(context);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    /**
     * Ermittelt die Standortquelle aus den Einstellungen und startet die Standortabfrage.
     */
    public void requestLocation(LocationResultCallback callback) {
        int accuracySetting = Integer.parseInt(settingsManager.getUpdateAccuracy()); // 1 = WIFI, 2 = MOBILFUNK, 3 = GPS

        String mode;
        switch (accuracySetting) {
            case 1:
                mode = "WIFI";
                break;
            case 2:
                mode = "MOBILFUNK";
                break;
            case 3:
                mode = "GPS";
                break;
            default:
                mode = "WIFI"; // Standardwert
                break;
        }

        requestLocationWithMode(mode, callback);
    }

    /**
     * Startet die Standortabfrage mit dem übergebenen Modus ("WIFI", "MOBILFUNK" oder "GPS").
     */
    @SuppressLint("MissingPermission")
    public void requestLocationWithMode(String mode, LocationResultCallback callback) {
        if (!hasLocationPermission()) {
            callback.onLocationError("Fehlende Berechtigung für Standort.");
            return;
        }

        // Wähle die passende Standortgenauigkeit
        switch (mode.toUpperCase()) {
            case "GPS":
                locationRequest = LocationRequest.create()
                        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY) // Nur GPS
                        .setInterval(5000) // Alle 5 Sekunden
                        .setFastestInterval(2000);
                break;

            case "WIFI":
                locationRequest = LocationRequest.create()
                        .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY) // WLAN-Ortung
                        .setInterval(8000) // Alle 8 Sekunden für häufige Updates
                        .setFastestInterval(4000);
                break;

            case "MOBILFUNK":
                locationRequest = LocationRequest.create()
                        .setPriority(LocationRequest.PRIORITY_LOW_POWER) // Mobilfunkmasten, weniger genau
                        .setInterval(15000) // Alle 15 Sekunden, da Mobilfunk weniger genau ist
                        .setFastestInterval(10000);
                break;

            default:
                callback.onLocationError("Ungültiger Modus! Wähle 'GPS', 'WIFI' oder 'MOBILFUNK'.");
                return;
        }


        // Callback für Standort-Aktualisierungen
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null || locationResult.getLastLocation() == null) {
                    callback.onLocationError("Kein Standort verfügbar.");
                    return;
                }
                callback.onLocationSuccess(locationResult.getLastLocation());
            }
        };

        // Startet die Standortaktualisierungen
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    /**
     * Stoppt die Standortaktualisierung.
     */
    public void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            Log.d(TAG, "Standortaktualisierung gestoppt.");
        }
    }

    /**
     * Prüft, ob die notwendigen Berechtigungen vorhanden sind.
     */
    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
}
