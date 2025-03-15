package de.heimfisch.positiontracker.ui.map;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

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

public class MapFragment extends Fragment {

    private FragmentMapBinding binding;
    private MapView mapView;
    private Marker userMarker;
    private GetLocation getLocation;
    private Handler uiUpdateHandler = new Handler();
    private Runnable uiUpdateRunnable;

    private boolean isBound = false;

    private SettingsManager settingsManager;

    private Button btnDataPush;
    private Button btnDataPushQueue;
    private DataPush dataPush;

    private long lastSentTime = 0;
    private static final long SEND_COOLDOWN = 20000; // 20 Sekunden Cooldown
    private static String TAG = "MapFragment";


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMapBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        settingsManager = new SettingsManager(requireContext());
        dataPush = new DataPush(requireContext()); // Initialisiere DataPush

        btnDataPush = binding.btnPushPosition;
        btnDataPushQueue = binding.btnPushPositionQueue;

        showButton();

        // OSM MapView initialisieren
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        mapView = binding.mapView;
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        // Standardposition setzen (Berlin)
        GeoPoint startPoint = new GeoPoint(52.5200, 13.4050);
        mapView.getController().setZoom(10.0);
        mapView.getController().setCenter(startPoint);

        // Marker setzen
        userMarker = new Marker(mapView);
        Drawable gmapsMarker = getResources().getDrawable(R.drawable.location_marker);
        userMarker.setIcon(gmapsMarker);
        userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        userMarker.setPosition(startPoint);
        userMarker.setTitle("Mein Standort");
        mapView.getOverlays().add(userMarker);

        // Standortermittlung
        getLocation = new GetLocation(requireContext());

        // Floating Action Button (FAB) für Standort
        FloatingActionButton fab = binding.fabLocation;
        fab.setOnClickListener(v -> getCurrentLocation());

        btnDataPush.setOnClickListener(v -> {
            Log.d("MapFragment", "Button gedrückt: Versuche, Standort zu senden...");
            sendCurrentLocation();
        });


        // UI regelmäßig aktualisieren
        uiUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                uiUpdateHandler.postDelayed(this, 5000); // Alle 5 Sekunden aktualisieren
            }
        };

        return root;
    }


    /**
     * Check if Basckgroundservice is active
     * if not show button to push data manual
     */

    private void showButton()
    {
        boolean active = settingsManager.isBackgroundServiceEnabled();
        if(active)
        {
            btnDataPush.setVisibility(View.GONE);
            btnDataPushQueue.setVisibility(View.VISIBLE);
        }else{
            btnDataPush.setVisibility(View.VISIBLE);
            btnDataPushQueue.setVisibility(View.GONE);
        }
    }

    private void sendCurrentLocation() {
        long currentTime = System.currentTimeMillis();

        // Verhindert mehrfaches Senden innerhalb der Cooldown-Zeit
        if (currentTime - lastSentTime < SEND_COOLDOWN) {
            Log.d("MapFragment", "Standort wurde kürzlich gesendet. Cooldown aktiv.");
            return; // Verhindert erneute Standortanfragen und Toasts
        }

        Log.d("MapFragment", "Fordere aktuellen Standort an...");

        getLocation.requestLocation(new GetLocation.LocationResultCallback() {
            @Override
            public void onLocationSuccess(Location location) {
                lastSentTime = System.currentTimeMillis(); // Cooldown-Zeit aktualisieren

                Log.d(TAG, "Standort erfolgreich erfasst: "
                        + location.getLatitude() + ", " + location.getLongitude());

                // Toast NUR beim erfolgreichen Senden der Position anzeigen
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Standort gesendet!", Toast.LENGTH_SHORT).show());

                dataPush.sendLocation(location);

                getLocation.stopLocationUpdates();  // <-- Diese Methode sicherstellen!
                Log.d(TAG, "Standort-Updates gestoppt.");
            }

            @Override
            public void onLocationError(String error) {
                Log.e("MapFragment", "Fehler beim Standort: " + error);

                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Standort konnte nicht erfasst werden!",
                                Toast.LENGTH_SHORT).show());
            }
        });
    }


    /**
     * Holt den aktuellen Standort und sendet ihn an den Hintergrundservice.
     */
    private void getCurrentLocation() {
        getLocation.requestLocation(new GetLocation.LocationResultCallback() {
            @Override
            public void onLocationSuccess(Location location) {
                GeoPoint newLocation = new GeoPoint(location.getLatitude(), location.getLongitude());

                // Karte auf neue Position zentrieren
                mapView.getController().animateTo(newLocation);

                // Marker aktualisieren
                userMarker.setPosition(newLocation);
                userMarker.setTitle("Mein aktueller Standort");
                mapView.invalidate(); // Karte aktualisieren


            }

            @Override
            public void onLocationError(String error) {
                System.out.println("Fehler: " + error);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "`onResume()` wurde aufgerufen – UI-Update gestartet.");
        mapView.onResume();
        uiUpdateHandler.post(uiUpdateRunnable); // UI-Update starten
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
        uiUpdateHandler.removeCallbacks(uiUpdateRunnable); // UI-Update stoppen
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mapView.onDetach();
        if (isBound) {
            isBound = false;
        }
        binding = null;
    }
}
