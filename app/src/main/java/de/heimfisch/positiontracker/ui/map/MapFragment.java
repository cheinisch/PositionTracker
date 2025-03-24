package de.heimfisch.positiontracker.ui.map;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import de.heimfisch.positiontracker.R;
import de.heimfisch.positiontracker.SettingsManager;
import de.heimfisch.positiontracker.databinding.FragmentMapBinding;
import de.heimfisch.positiontracker.utils.DataPush;
import de.heimfisch.positiontracker.utils.GetLocation;
import de.heimfisch.positiontracker.utils.PushStatusManager;

public class MapFragment extends Fragment {

    private static final String TAG = "MapFragment";
    private static final long SEND_COOLDOWN = 20000; // 20 Sekunden Cooldown

    private FragmentMapBinding binding;
    private MapView mapView;
    private Marker userMarker;
    private GetLocation getLocation;
    private Handler uiUpdateHandler = new Handler();
    private Runnable uiUpdateRunnable;

    private Button btnDataPush;
    private Button btnDataPushQueue;
    private TextView tvPointsQueue;
    private TextView tvTimeRemaining;

    private DataPush dataPush;
    private PushStatusManager pushStatusManager;
    private SettingsManager settingsManager;

    private long lastSentTime = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMapBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        settingsManager = new SettingsManager(requireContext());
        dataPush = new DataPush(requireContext());
        pushStatusManager = new PushStatusManager(requireContext());

        btnDataPush = binding.btnPushPosition;
        btnDataPushQueue = binding.btnPushPositionQueue;
        tvPointsQueue = binding.tvPointsQueue;
        tvTimeRemaining = binding.tvTimeRemaining;

        showButton();

        // OSM MapView
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        mapView = binding.mapView;
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        // Startposition (Berlin)
        GeoPoint startPoint = new GeoPoint(52.5200, 13.4050);
        mapView.getController().setZoom(10.0);
        mapView.getController().setCenter(startPoint);

        // Marker initialisieren
        userMarker = new Marker(mapView);
        Drawable gmapsMarker = getResources().getDrawable(R.drawable.location_marker);
        userMarker.setIcon(gmapsMarker);
        userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        userMarker.setPosition(startPoint);
        mapView.getOverlays().add(userMarker);

        getLocation = new GetLocation(requireContext());

        FloatingActionButton fab = binding.fabLocation;
        fab.setOnClickListener(v -> getCurrentLocation());

        btnDataPush.setOnClickListener(v -> {
            Log.d(TAG, "Manueller Push gestartet...");
            sendCurrentLocation();
        });

        // UI-Update-Loop
        uiUpdateRunnable = () -> {
            updateQueueStatus();
            updateTimeRemaining();
            uiUpdateHandler.postDelayed(uiUpdateRunnable, 1000);
        };

        return root;
    }

    private void showButton() {
        boolean active = settingsManager.isBackgroundServiceEnabled();
        btnDataPush.setVisibility(active ? View.GONE : View.VISIBLE);
        btnDataPushQueue.setVisibility(active ? View.VISIBLE : View.GONE);
    }

    private void sendCurrentLocation() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSentTime < SEND_COOLDOWN) {
            Log.d(TAG, "Cooldown aktiv, Position wurde kürzlich gesendet.");
            return;
        }

        getLocation.requestLocation(new GetLocation.LocationResultCallback() {
            @Override
            public void onLocationSuccess(Location location) {
                lastSentTime = System.currentTimeMillis();
                dataPush.sendLocation(location);
                showToast(getString(R.string.map_position_send_success));
                getLocation.stopLocationUpdates();
            }

            @Override
            public void onLocationError(String error) {
                Log.e(TAG, "Fehler beim Standort: " + error);
                showToast(getString(R.string.map_position_cannot_locate));
            }
        });
    }

    private void getCurrentLocation() {
        getLocation.requestLocation(new GetLocation.LocationResultCallback() {
            @Override
            public void onLocationSuccess(Location location) {
                GeoPoint newLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                mapView.getController().animateTo(newLocation);
                userMarker.setPosition(newLocation);
                mapView.invalidate();
            }

            @Override
            public void onLocationError(String error) {
                Log.e(TAG, "Fehler beim Standortabruf: " + error);
            }
        });
    }

    private void updateQueueStatus() {
        if (dataPush != null && tvPointsQueue != null) {
            int count = dataPush.getPendingPointCount();
            tvPointsQueue.setText("Warteschlange: " + count + " Punkt(e)");
        }
    }

    private void updateTimeRemaining() {
        if (pushStatusManager != null && tvTimeRemaining != null) {
            long seconds = pushStatusManager.getRemainingSeconds();
            tvTimeRemaining.setText("Nächster Push in: " + seconds + "s");
        }
    }

    private void showToast(String msg) {
        if (getActivity() != null) {
            requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "`onResume()` – Starte UI-Update");
        mapView.onResume();
        uiUpdateHandler.post(uiUpdateRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
        uiUpdateHandler.removeCallbacks(uiUpdateRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mapView.onDetach();
        binding = null;
    }
}
