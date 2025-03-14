package de.heimfisch.positiontracker.ui.map;

import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import de.heimfisch.positiontracker.databinding.FragmentMapBinding;
import de.heimfisch.positiontracker.utils.GetLocation;

public class MapFragment extends Fragment {

    private FragmentMapBinding binding;
    private MapView mapView;
    private Marker userMarker;
    private GetLocation getLocation;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        MapViewModel homeViewModel = new ViewModelProvider(this).get(MapViewModel.class);

        binding = FragmentMapBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // OSM MapView initialisieren
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        mapView = binding.mapView;
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        // Standardposition setzen (Berlin)
        GeoPoint startPoint = new GeoPoint(52.5200, 13.4050);
        mapView.getController().setZoom(10.0);
        mapView.getController().setCenter(startPoint);

        // Google Maps Stil Marker setzen (eigene Grafik)
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

        return root;
    }

    /**
     * Ermittelt die aktuelle Position und aktualisiert die Karte + Marker
     */
    private void getCurrentLocation() {
        getLocation.requestLocation(new GetLocation.LocationResultCallback() {
            @Override
            public void onLocationSuccess(Location location) {
                GeoPoint newLocation = new GeoPoint(location.getLatitude(), location.getLongitude());

                // Karte auf neue Position zentrieren
                mapView.getController().animateTo(newLocation);
                // mapView.getController().setZoom(15.0);

                // Marker aktualisieren
                userMarker.setPosition(newLocation);
                userMarker.setTitle("Mein aktueller Standort");
                mapView.invalidate(); // Karte aktualisieren
            }

            @Override
            public void onLocationError(String error) {
                // Falls kein Standort verfügbar ist
                System.out.println("Fehler: " + error);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mapView.onDetach();
        binding = null;
    }
}
