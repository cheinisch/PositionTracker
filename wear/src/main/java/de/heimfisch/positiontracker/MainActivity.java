package de.heimfisch.positiontracker;

import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.wear.activity.WearableActivity;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.api.IMapController;

public class MainActivity extends WearableActivity {

    private MapView map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // osmdroid vorbereiten
        Configuration.getInstance().load(
                getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
        );

        // Layout setzen
        setContentView(R.layout.activity_main);

        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(false); // Wear OS: kein Multitouch

        // Karte zentrieren
        GeoPoint point = new GeoPoint(52.5200, 13.4050); // Berlin
        IMapController controller = map.getController();
        controller.setZoom(14.5);
        controller.setCenter(point);

        // Marker setzen
        Marker marker = new Marker(map);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle("Berlin");
        map.getOverlays().add(marker);

        // Optional: UI für Wear vorbereiten
        setAmbientEnabled(); // falls du Ambient Mode nutzen willst
    }
}
