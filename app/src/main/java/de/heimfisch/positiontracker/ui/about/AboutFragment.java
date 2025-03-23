package de.heimfisch.positiontracker.ui.about;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;

import de.heimfisch.positiontracker.R;

public class AboutFragment extends Fragment {

    public AboutFragment() {
        // Required empty public constructor
    }

    public static AboutFragment newInstance() {
        return new AboutFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);

        // TextViews aus dem Layout referenzieren
        TextView appNameText = view.findViewById(R.id.text_app_name);
        TextView versionText = view.findViewById(R.id.text_app_version);
        TextView buildText = view.findViewById(R.id.text_app_build);

        try {
            PackageManager pm = requireContext().getPackageManager();
            PackageInfo info = pm.getPackageInfo(requireContext().getPackageName(), 0);

            String appName = getString(R.string.app_name);
            String version = info.versionName;
            int buildCode = info.versionCode; // oder: info.getLongVersionCode() für API 28+

            appNameText.setText(appName);
            versionText.setText("Version: " + version);
            buildText.setText("Build Code: " + buildCode);

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        view.findViewById(R.id.button_licenses).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), OssLicensesMenuActivity.class);
            intent.putExtra("title", "Verwendete Open Source Libraries");
            startActivity(intent);
        });

        return view;
    }
}
