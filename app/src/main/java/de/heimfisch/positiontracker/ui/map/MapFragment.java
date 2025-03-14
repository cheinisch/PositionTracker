package de.heimfisch.positiontracker.ui.map;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import de.heimfisch.positiontracker.databinding.FragmentMapBinding;

public class MapFragment extends Fragment {

    private FragmentMapBinding binding;
    private MapView mapView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        MapViewModel homeViewModel =
                new ViewModelProvider(this).get(MapViewModel.class);

        binding = FragmentMapBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // OSM MapView initialisieren
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        mapView = binding.mapView;
        mapView.setTileSource(TileSourceFactory.MAPNIK); // Standard OpenStreetMap Kacheln
        mapView.setMultiTouchControls(true);
        mapView.setMinZoomLevel(4.0);

        // Setze Standardposition auf Berlin
        GeoPoint startPoint = new GeoPoint(52.5200, 13.4050);
        mapView.getController().setZoom(10.0);

        // Begrenzung nur für Höhe (Nord/Süd) setzen, aber Ost/West offen lassen
        double maxLatitude = 85.0511;  // Maximale nördliche Breite (Oberhalb von Grönland)
        double minLatitude = -85.0511; // Maximale südliche Breite (Antarktis)
        double maxLongitude = 180.0;   // Keine Begrenzung für Osten
        double minLongitude = -180.0;  // Keine Begrenzung für Westen

        BoundingBox boundingBox = new BoundingBox(maxLatitude, maxLongitude, minLatitude, minLongitude);
        mapView.setScrollableAreaLimitDouble(boundingBox); // Setzt die vertikale Begrenzung

        mapView.getController().setCenter(startPoint);

        // Beispiel-Marker setzen
        Marker marker = new Marker(mapView);
        marker.setPosition(startPoint);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle("Berlin");
        mapView.getOverlays().add(marker);

        return root;
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
