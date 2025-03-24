package de.heimfisch.positiontracker.ui.permission;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.heimfisch.positiontracker.R;

public class PermissionFragment extends Fragment {

    private TextView notificationStatus;
    private TextView gpsStatus;
    private TextView backgroundLocationStatus;

    public PermissionFragment() {}

    public static PermissionFragment newInstance(String param1, String param2) {
        PermissionFragment fragment = new PermissionFragment();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_permission, container, false);
        notificationStatus = view.findViewById(R.id.notification_status);
        gpsStatus = view.findViewById(R.id.gps_status);
        backgroundLocationStatus = view.findViewById(R.id.background_location_status);

        checkPermissions();

        return view;
    }

    private void checkPermissions() {
        Context context = requireContext();

        // Benachrichtigungen
        boolean notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled();
        notificationStatus.setText("Benachrichtigungen: " + (notificationsEnabled ? "Aktiv" : "Inaktiv"));

        // GPS (Feinstandort)
        boolean fineLocationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        gpsStatus.setText("GPS-Position: " + (fineLocationGranted ? "Erlaubt" : "Nicht erlaubt"));

        // GPS im Hintergrund (nur ab Android 10/Q)
        boolean backgroundLocationGranted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundLocationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        backgroundLocationStatus.setText("GPS im Hintergrund: " + (backgroundLocationGranted ? "Erlaubt" : "Nicht erlaubt"));
    }
}
