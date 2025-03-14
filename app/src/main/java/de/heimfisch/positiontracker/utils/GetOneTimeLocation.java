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

public class GetOneTimeLocation {

    private static final String TAG = "GetOneTimeLocation";
    private final Context context;
    private final FusedLocationProviderClient fusedLocationClient;

    public interface LocationResultCallback {
        void onLocationSuccess(Location location);
        void onLocationError(String error);
    }

    public GetOneTimeLocation(Context context) {
        this.context = context;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    /**
     * Startet eine EINMALIGE Standortbestimmung und stoppt sich selbst danach.
     */
    @SuppressLint("MissingPermission")
    public void requestSingleLocation(LocationResultCallback callback) {
        if (!hasLocationPermission()) {
            callback.onLocationError("Fehlende Berechtigung für Standort.");
            return;
        }

        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY) // GPS für beste Genauigkeit
                .setNumUpdates(1) // EINMALIGE Standortabfrage
                .setInterval(0); // Sofortiges Update

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null || locationResult.getLastLocation() == null) {
                    callback.onLocationError("Kein Standort verfügbar.");
                    return;
                }

                // Standort zurückgeben
                callback.onLocationSuccess(locationResult.getLastLocation());

                // Sofort nach dem ersten Update stoppen
                fusedLocationClient.removeLocationUpdates(this);
                Log.d(TAG, "Standortaktualisierung gestoppt (einmaliger Abruf).");
            }
        };

        // Startet die EINMALIGE Standortabfrage
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    /**
     * Prüft, ob die notwendigen Berechtigungen vorhanden sind.
     */
    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
}
