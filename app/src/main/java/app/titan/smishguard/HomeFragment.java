package app.titan.smishguard;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private TextView tvStatus, tvScamCount, tvMonitorLabel, tvFlaggedBrands;
    private ImageView ivStatusIcon;
    private ListView listView;
    private View cardRecentVerdicts; // Move this to a class variable
    private SmishRepository repo;
    private SmishEngine engine;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        repo = new SmishRepository(requireContext());
        engine = new SmishEngine();

        // 1. UI Bindings
        tvStatus = view.findViewById(R.id.tvStatus);
        tvMonitorLabel = view.findViewById(R.id.tvMonitorLabel);
        ivStatusIcon = view.findViewById(R.id.ivStatusIcon);
        tvScamCount = view.findViewById(R.id.tvScamCount);
        tvFlaggedBrands = view.findViewById(R.id.tvFlaggedBrands);
        listView = view.findViewById(R.id.smsListView);
        cardRecentVerdicts = view.findViewById(R.id.cardRecentVerdicts); // Bind here

        // 2. Initial Data Load - Pass the 'view' we just inflated
        updateDashboard();

        // 3. Perform Real-time Connection Check
        checkApiHealth();

        return view;
    }

    /**
     * Pulls the latest stats, scan history, and flagged entities.
     */
    private void updateDashboard() {
        // Safety check: Ensure Fragment is attached and repo exists
        if (repo == null || !isAdded()) return;

        // A. Update the 'Scams Caught' bento card
        tvScamCount.setText(String.valueOf(repo.getScamCount()));

        // B. Update the dynamic 'Flagged' brands section
        List<String> flagged = repo.getFlaggedBrands();
        if (flagged.isEmpty()) {
            tvFlaggedBrands.setText("No active threats");
        } else {
            StringBuilder sb = new StringBuilder();
            for (String brand : flagged) {
                sb.append("• ").append(brand).append("\n");
            }
            tvFlaggedBrands.setText(sb.toString().trim());
        }

        // C. Update the 'Recent Verdicts' list and Visibility
        List<String> recentData = repo.getRecentVerdicts();

        if (recentData.isEmpty()) {
            if (cardRecentVerdicts != null) cardRecentVerdicts.setVisibility(View.GONE);
        } else {
            if (cardRecentVerdicts != null) cardRecentVerdicts.setVisibility(View.VISIBLE);

            ArrayList<String> dataList = new ArrayList<>(recentData);
            SMSAdapter adapter = new SMSAdapter(requireContext(), dataList);
            listView.setAdapter(adapter);
        }
    }

    private void checkApiHealth() {
        engine.checkMessage("health_check_ping", new SmishEngine.ApiCallback() {
            @Override
            public void onSuccess(SmishApiModels.SmishResponse result) {
                setShieldState(true);
            }

            @Override
            public void onError(String error) {
                setShieldState(false);
            }
        });
    }

    private void setShieldState(boolean active) {
        if (!isAdded() || getContext() == null) return;

        if (active) {
            tvStatus.setText("Shielded");
            tvStatus.setTextColor(Color.parseColor("#004D5F"));
            tvMonitorLabel.setText("ACTIVE MONITOR");
            ivStatusIcon.setImageResource(R.drawable.ic_shield_check);
            ivStatusIcon.setColorFilter(Color.WHITE);
        } else {
            tvStatus.setText("Unprotected");
            tvStatus.setTextColor(Color.RED);
            tvMonitorLabel.setText("API OFFLINE");
            ivStatusIcon.setImageResource(android.R.drawable.ic_dialog_alert);
            ivStatusIcon.setColorFilter(null);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateDashboard();
    }
}