package de.heimfisch.positiontracker.ui.settings;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.slider.LabelFormatter;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

import de.heimfisch.positiontracker.R;
import de.heimfisch.positiontracker.SettingsManager;
import de.heimfisch.positiontracker.TestConnection;
import de.heimfisch.positiontracker.databinding.FragmentSettingsBinding;
import de.heimfisch.positiontracker.ui.permission.PermissionFragment;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private TestConnection connTest;
    private SettingsManager settingsManager;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1002;

    private String TAG = "SettingsFragment";

    private boolean enableUpdateIntervall = false;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        //SettingsViewModel settingsViewModel =
          //      new ViewModelProvider(this).get(SettingsViewModel.class);

        settingsManager = new SettingsManager(requireContext());
        connTest = new TestConnection();



        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        Button btnSave = binding.btnSave;
        Button btnConTest = binding.btnConTest;
        Button btnPermission = binding.btnPermission;
        Button btnDistanceTime = binding.btnTime;
        Button btnDistanceMeter = binding.btnDistance;

        Switch swAutorun = binding.swAutoRun;

        TextInputEditText txtHostName = binding.txtHostName;
        TextInputEditText txtPort = binding.txtPort;
        TextInputEditText txtApiKey = binding.txtApiKey;
        TextInputEditText txtMinimumDistance = binding.txtMinimumDistance;
        TextInputEditText txtDistanceTime = binding.txtDistanceTime;

        TextView tvAccurancay = binding.tvSettingsAccurancay;

        RadioGroup rgDistance = binding.radioGroupDistance;
        RadioButton rgDistance0 = binding.radioDistance0;
        RadioButton rgDistance10 = binding.radioDistance10;
        RadioButton rgDistance50 = binding.radioDistance50;
        RadioButton rgDistance100 = binding.radioDistance100;
        RadioButton rgDistance200 = binding.radioDistance200;

        Slider slideAccurancay = binding.sliderAccurancay;

        LinearLayout layoutRadioGroup = binding.layoutDistanceMeter;
        TextInputLayout textInputLayoutDistance = binding.textFieldMinimumDistance;


        // Read Settings
        txtHostName.setText(settingsManager.getDarwarichHost());
        txtPort.setText(settingsManager.getDarwarichPort());
        txtApiKey.setText(settingsManager.getDarwarichApi());
        swAutorun.setChecked(settingsManager.isBackgroundServiceEnabled());

        loadRadioGroup();
        loadSlider(slideAccurancay, tvAccurancay);
        enableDisableButtons();
        loadUpdateMode();
        loadMinimumDistance();
        loadMDistanceTime();
        // Listener

        rgDistance.setOnCheckedChangeListener((group, checkedId) -> {
            String selectedValue = "";
            if (checkedId == binding.radioDistance0.getId()) {
                selectedValue = "0";
            } else if (checkedId == binding.radioDistance10.getId()) {
                selectedValue = "10";
            } else if (checkedId == binding.radioDistance50.getId()) {
                selectedValue = "50";
            } else if (checkedId == binding.radioDistance100.getId()) {
                selectedValue = "100";
            } else if (checkedId == binding.radioDistance200.getId()) {
                selectedValue = "200";
            }
            Log.d(TAG, "Gewählte Distanz: " + selectedValue + "m");
            settingsManager.setUpdateDistance(selectedValue);
        });

        btnSave.setOnClickListener(v -> {
            Log.d(TAG, "Save Button pressed");
            String hostname = txtHostName.getText().toString();
            String port = txtPort.getText().toString();
            String key = txtApiKey.getText().toString();
            settingsManager.setDarwarichHost(hostname);
            settingsManager.setDarwarichPort(port);
            settingsManager.setDarwarichApi(key);
            Toast.makeText(requireContext(), "Settings saved", Toast.LENGTH_SHORT).show();
        });

        btnConTest.setOnClickListener(v -> {
            String hostname = txtHostName.getText().toString();
            String port = txtPort.getText().toString();

            Log.d(TAG, "Starte Verbindungstest mit: " + hostname + ":" + port);

            connTest.checkConnection(hostname, port, success -> {
                Log.d(TAG, "Callback erhalten: success = " + success); // Debug-Log zum Testen

                requireActivity().runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(requireContext(), "Connection Successful", Toast.LENGTH_SHORT).show();
                        settingsManager.setDarwarichHost(hostname);
                        settingsManager.setDarwarichPort(port);
                        Log.d(TAG, "ConnTest Successful");
                    } else {
                        Toast.makeText(requireContext(), "Connection NOT Successful", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "ConnTest NOT Successful");
                    }
                });
            });

            Log.d(TAG, "CheckConnection-Methode wurde aufgerufen");
        });

        btnPermission.setOnClickListener(v -> {
            /*checkLocationPermission();
            checkNotificationPermission();*/
            PermissionFragment fragment = PermissionFragment.newInstance(null, null);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment) // ID deines FragmentContainers im Layout
                    .addToBackStack(null)
                    .commit();
        });

        btnDistanceMeter.setOnClickListener(v -> {
            selectUpdateMode(1);
        });

        btnDistanceTime.setOnClickListener(v -> {
            selectUpdateMode(2);
        });

        swAutorun.setOnClickListener(v -> {
            settingsManager.setBackgroundServiceEnabled(swAutorun.isChecked());
            Log.d(TAG,"Autorun is changed");
            enableDisableButtons();
        });

        txtDistanceTime.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                settingsManager.setUpdateDistanceTime(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        txtMinimumDistance.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                settingsManager.setMinimumDistance(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        slideAccurancay.setLabelBehavior(LabelFormatter.LABEL_GONE);

        slideAccurancay.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                int i = (int) slideAccurancay.getValue();
                settingsManager.setUpdateAccuracy(String.valueOf(i));
                setAccuracyValue(i, tvAccurancay);
            }
        });


        return root;
    }

    private void loadSlider(Slider slideAccurancay, TextView tvAccurancay) {
        int value = Integer.parseInt(settingsManager.getUpdateAccuracy());

        slideAccurancay.setValue(value);
        setAccuracyValue(value, tvAccurancay);
    }

    private void setAccuracyValue(int i, TextView tvAccuracy) {
        if(i == 1)
        {
            tvAccuracy.setText(R.string.settings_accuracy_1);
        }else if(i == 2)
        {
            tvAccuracy.setText(R.string.settings_accuracy_2);
        }else{
            tvAccuracy.setText(R.string.settings_accuracy_3);
        }
    }

    private void loadRadioGroup() {
        if(Objects.equals(settingsManager.getUpdateDistance(), "0"))
        {
            binding.radioGroupDistance.check(binding.radioDistance0.getId());
        }else if(Objects.equals(settingsManager.getUpdateDistance(), "10"))
        {
            binding.radioGroupDistance.check(binding.radioDistance10.getId());
        }else if(Objects.equals(settingsManager.getUpdateDistance(), "50"))
        {
            binding.radioGroupDistance.check(binding.radioDistance50.getId());
        }else if(Objects.equals(settingsManager.getUpdateDistance(), "100"))
        {
            binding.radioGroupDistance.check(binding.radioDistance100.getId());
        }else if(Objects.equals(settingsManager.getUpdateDistance(), "200"))
        {
            binding.radioGroupDistance.check(binding.radioDistance200.getId());
        }
    }

    private void enableDisableButtons()
    {
        if(settingsManager.isBackgroundServiceEnabled())
        {
            Log.d(TAG,"Buttons enabled");
            binding.radioGroupDistance.setEnabled(true);
        }else{
            Log.d(TAG,"Buttons disabled");
            binding.radioGroupDistance.setEnabled(false);
        }
        for(int i = 0; i < binding.radioGroupDistance.getChildCount(); i++){
            ((RadioButton)binding.radioGroupDistance.getChildAt(i)).setEnabled(binding.swAutoRun.isChecked());
        }
        binding.sliderAccurancay.setEnabled(binding.swAutoRun.isChecked());
    }

    private void checkLocationPermission() {
        Log.d(TAG,"Prüfe Berechtigungen");
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void selectUpdateMode(int mode)
    {
        if(!enableUpdateIntervall)
        {
            mode = 2;
            binding.btnTime.setVisibility(View.GONE);
            binding.btnDistance.setVisibility(View.GONE);
        }
        // MODE 1 = distance, MODE 2 = time
        if(mode == 1)
        {
            settingsManager.setUpdateDistanceMode(String.valueOf(mode));
            binding.layoutDistanceTime.setVisibility(View.GONE);
            binding.layoutDistanceMeter.setVisibility(View.VISIBLE);
        }else if(mode == 2)
        {
            settingsManager.setUpdateDistanceMode(String.valueOf(mode));
            binding.layoutDistanceTime.setVisibility(View.VISIBLE);
            binding.layoutDistanceMeter.setVisibility(View.GONE);
        }
    }

    private void loadUpdateMode()
    {
        int mode = Integer.parseInt(settingsManager.getUpdateDistanceMode());

        selectUpdateMode(mode);
    }

    private void loadMinimumDistance()
    {
        binding.txtMinimumDistance.setText(settingsManager.getMinimumDistance());
    }

    private void loadMDistanceTime()
    {
        binding.txtDistanceTime.setText(settingsManager.getUpdateDistanceTime());
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
            } else {
                Toast.makeText(requireContext(), "Benachrichtigungsberechtigung bereits erteilt", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(requireContext(), "Benachrichtigungsberechtigung ist unter Android 13 nicht erforderlich", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Standortberechtigung erteilt", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Standortberechtigung verweigert!", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) { // Neu: Handling für Notifications
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Benachrichtigungsberechtigung erteilt", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Benachrichtigungsberechtigung verweigert!", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}