package app.titan.smishguard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsFragment extends Fragment {

    private TextView settingsTitle;
    private TextView settingsDescription;
    private SwitchMaterial settingToggle;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout we just built
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        settingsTitle=view.findViewById(R.id.tvRowTitle);
        settingsDescription=view.findViewById(R.id.tvRowDesc);
        settingToggle=view.findViewById(R.id.rowSwitch);

        // 1. Bind the Back Button
        ImageButton btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            // Returns the user to the previous fragment (Home)
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        settingsTitle.setText("Allow Notifications");
        settingsDescription.setText("Allow the app to show notification alerts when a scam message is detected");
        settingToggle.setChecked(true);

        return view;
    }
}