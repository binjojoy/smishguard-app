package app.titan.smishguard;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
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

    private ImageButton btnSettings;

    // Colors
    private final int colorActive = Color.parseColor("#004D5F");
    private final int colorInactive = Color.parseColor("#94A3B8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_fetch);
        final View topBar = findViewById(R.id.topBar);
        final View bottomBar = findViewById(R.id.bottomNavCard);

        initializeBottomNav();
        btnSettings = findViewById(R.id.btn_settings);
        SettingsFragment settingsFragment = new SettingsFragment();
        btnSettings.setOnClickListener(v -> {
            // 1. Enable Hardware Layers (GPU pre-rendering)
            topBar.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            bottomBar.setLayerType(View.LAYER_TYPE_HARDWARE, null);

            // 2. Slide-out: Use AccelerateInterpolator (speeds up as it leaves)
            topBar.animate()
                    .translationY(-topBar.getHeight())
                    .setDuration(175) // 250ms is the sweet spot for smoothness
                    .setInterpolator(new android.view.animation.AccelerateInterpolator())
                    .withEndAction(() -> {
                        topBar.setVisibility(View.GONE);
                        topBar.setLayerType(View.LAYER_TYPE_NONE, null);
                    }).start();

            bottomBar.animate()
                    .translationY(bottomBar.getHeight())
                    .setDuration(175)
                    .setInterpolator(new android.view.animation.AccelerateInterpolator())
                    .withEndAction(() -> {
                        bottomBar.setVisibility(View.GONE);
                        bottomBar.setLayerType(View.LAYER_TYPE_NONE, null);
                    }).start();

            // 3. Fragment Transition
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(
                            R.anim.slide_in_right,
                            R.anim.fade_out,
                            R.anim.fade_in,
                            R.anim.slide_out_right
                    )
                    .replace(R.id.fragment_container, new SettingsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                topBar.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                bottomBar.setLayerType(View.LAYER_TYPE_HARDWARE, null);

                topBar.setVisibility(View.VISIBLE);
                bottomBar.setVisibility(View.VISIBLE);

                // Slide-in: Use DecelerateInterpolator (Slowing down as it arrives feels smoother)
                topBar.animate()
                        .translationY(0)
                        .setDuration(280) // Slightly longer return feels more "natural"
                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                        .withEndAction(() -> topBar.setLayerType(View.LAYER_TYPE_NONE, null))
                        .start();

                bottomBar.animate()
                        .translationY(0)
                        .setDuration(280)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                        .withEndAction(() -> bottomBar.setLayerType(View.LAYER_TYPE_NONE, null))
                        .start();

                updateBottomNavUI(1);
            }
        });

        // Start by checking permissions
        if (checkAndRequestPermissions()) {
            selectTab(1); // <--- This fixes the white screen
        }
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
                selectedFragment = new HistoryFragment(); // Create later
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

    private boolean checkAndRequestPermissions() {
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
            ActivityCompat.requestPermissions(this,
                    listPermissionsNeeded.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
            return false; // Permissions not granted yet
        }

        return true; // All good, ready to load
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // User just gave permission, load Home now!
                selectTab(1);
            } else {
                Toast.makeText(this, "Permission denied. App won't work properly.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}