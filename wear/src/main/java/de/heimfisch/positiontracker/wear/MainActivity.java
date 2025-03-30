package de.heimfisch.positiontracker.wear;

import android.app.Activity;
import android.os.Bundle;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.api.IMapController;
import android.preference.PreferenceManager;

public class MainActivity extends Activity {

    private MapView map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(
                getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
        );

        setContentView(R.layout.activity_main);

        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(false);

        GeoPoint berlin = new GeoPoint(52.5200, 13.4050);
        IMapController controller = map.getController();
        controller.setZoom(14.5);
        controller.setCenter(berlin);

        Marker marker = new Marker(map);
        marker.setPosition(berlin);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle("Berlin");
        map.getOverlays().add(marker);
    }
}