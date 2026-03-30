package app.titan.smishguard;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;

public class SMSFetchActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 101;

    // UI Elements for Bottom Nav
    private LinearLayout navHome, navHistory, navInsights;
    private ImageView ivHome, ivHistory, ivInsights;
    private TextView tvHome, tvHistory, tvInsights;

    // Colors
    private final int colorActive = Color.parseColor("#004D5F");
    private final int colorInactive = Color.parseColor("#94A3B8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_fetch);

        initializeBottomNav();
        ImageButton btnSettings = findViewById(R.id.btn_settings);
        btnSettings.setOnClickListener(v -> {
            Toast.makeText(this, "Settings Clicked!", Toast.LENGTH_SHORT).show();
        });

        // Start by checking permissions
        checkAndRequestPermissions();
    }

    private void initializeBottomNav() {
        // Layouts
        navHome = findViewById(R.id.nav_home);
        navHistory = findViewById(R.id.nav_history);
        navInsights = findViewById(R.id.nav_insights);

        // Icons
        ivHome = findViewById(R.id.iv_home);
        ivHistory = findViewById(R.id.iv_history);
        ivInsights = findViewById(R.id.iv_insights);

        // Texts
        tvHome = findViewById(R.id.tv_home);
        tvHistory = findViewById(R.id.tv_history);
        tvInsights = findViewById(R.id.tv_insights);

        // Click Listeners
        navHome.setOnClickListener(v -> selectTab(1));
        navHistory.setOnClickListener(v -> selectTab(2));
        navInsights.setOnClickListener(v -> selectTab(3));

        // Set Home as default active tab
        updateBottomNavUI(1);
    }

    // Inside selectTab(int index)
    private void selectTab(int index) {
        updateBottomNavUI(index);

        Fragment selectedFragment = null;
        switch (index) {
            case 1:
                selectedFragment = new HomeFragment();
                break;
            case 2:
                // selectedFragment = new HistoryFragment(); // Create later
                break;
            case 3:
                // selectedFragment = new InsightsFragment(); // Create later
                break;
        }

        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
        }
    }

    private void updateBottomNavUI(int activeIndex) {
        // 1. Reset all to Inactive
        navHome.setBackground(null);
        navHistory.setBackground(null);
        navInsights.setBackground(null);

        ivHome.setImageTintList(ColorStateList.valueOf(colorInactive));
        ivHistory.setImageTintList(ColorStateList.valueOf(colorInactive));
        ivInsights.setImageTintList(ColorStateList.valueOf(colorInactive));

        tvHome.setTextColor(colorInactive);
        tvHistory.setTextColor(colorInactive);
        tvInsights.setTextColor(colorInactive);

        // 2. Set Active Tab
        switch (activeIndex) {
            case 1:
                navHome.setBackgroundResource(R.drawable.nav_item_active_bg);
                ivHome.setImageTintList(ColorStateList.valueOf(colorActive));
                tvHome.setTextColor(colorActive);
                break;
            case 2:
                navHistory.setBackgroundResource(R.drawable.nav_item_active_bg);
                ivHistory.setImageTintList(ColorStateList.valueOf(colorActive));
                tvHistory.setTextColor(colorActive);
                break;
            case 3:
                navInsights.setBackgroundResource(R.drawable.nav_item_active_bg);
                ivInsights.setImageTintList(ColorStateList.valueOf(colorActive));
                tvInsights.setTextColor(colorActive);
                break;
        }
    }

    // --- Keep your Permission methods below ---

    private void checkAndRequestPermissions() {
        ArrayList<String> permissionsNeeded = new ArrayList<>();
        permissionsNeeded.add(Manifest.permission.READ_SMS);
        permissionsNeeded.add(Manifest.permission.RECEIVE_SMS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        ArrayList<String> listPermissionsNeeded = new ArrayList<>();
        for (String perm : permissionsNeeded) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(perm);
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Logic handled if needed
        }
    }
}