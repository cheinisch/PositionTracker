package de.heimfisch.positiontracker;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import de.heimfisch.positiontracker.databinding.ActivityMainBinding;
import de.heimfisch.positiontracker.services.BackgroundService;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private boolean showStatsMenu = false;
    private static final String TAG = "MainActivity";
    private SettingsManager settingsManager;
    private Handler handler = new Handler();
    private boolean lastTrackingState = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settingsManager = new SettingsManager(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);

        // Menü-Elemente steuern
        Menu menu = navView.getMenu();
        MenuItem statsItem = menu.findItem(R.id.navigation_stats);
        statsItem.setVisible(showStatsMenu);

        // Navigation Setup
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_stats, R.id.navigation_map, R.id.navigation_settings)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        // Hintergrunddienst regelmäßig prüfen
        handler.post(checkServiceRunnable);
    }

    /**
     * Prüft alle 5 Sekunden, ob der `BackgroundService` gestartet oder gestoppt werden muss.
     */
    private final Runnable checkServiceRunnable = new Runnable() {
        @Override
        public void run() {
            manageBackgroundService();
            handler.postDelayed(this, 5000); // Wiederhole die Prüfung alle 5 Sekunden
        }
    };

    /**
     * Prüft die gespeicherten Einstellungen und startet oder stoppt den `BackgroundService`.
     */
    private void manageBackgroundService() {
        boolean isTrackingEnabled = settingsManager.isBackgroundServiceEnabled();
        Intent serviceIntent = new Intent(this, BackgroundService.class);

        if (isTrackingEnabled != lastTrackingState) { // Prüfe, ob sich die Einstellung geändert hat
            lastTrackingState = isTrackingEnabled;
            if (isTrackingEnabled) {
                Log.d(TAG, "Hintergrunddienst ist aktiviert – starte Service.");
                startService(serviceIntent);
            } else {
                Log.d(TAG, "Hintergrunddienst ist deaktiviert – stoppe Service.");
                stopService(serviceIntent);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(checkServiceRunnable); // Stoppt die regelmäßige Überprüfung, wenn die App geschlossen wird
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {

        getMenuInflater().inflate(R.menu.top_nav_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.navigation_about) {
            // z. B. Navigation zu den Einstellungen
            Navigation.findNavController(this, R.id.nav_host_fragment_activity_main)
                    .navigate(R.id.navigation_about);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
