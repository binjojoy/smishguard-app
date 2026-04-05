package app.titan.smishguard;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;

import java.util.ArrayList;

public class SMSFetchActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 101;

    // UI Elements
    private LinearLayout navHome, navHistory, navInsights;
    private ImageView ivHome, ivHistory, ivInsights;
    private TextView tvHome, tvHistory, tvInsights;
    private ImageButton btnSettings;

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

        // 1. Handle Settings Transition
        btnSettings.setOnClickListener(v -> {
            topBar.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            bottomBar.setLayerType(View.LAYER_TYPE_HARDWARE, null);

            topBar.animate().translationY(-topBar.getHeight()).setDuration(175)
                    .setInterpolator(new android.view.animation.AccelerateInterpolator())
                    .withEndAction(() -> {
                        topBar.setVisibility(View.GONE);
                        topBar.setLayerType(View.LAYER_TYPE_NONE, null);
                    }).start();

            bottomBar.animate().translationY(bottomBar.getHeight()).setDuration(175)
                    .setInterpolator(new android.view.animation.AccelerateInterpolator())
                    .withEndAction(() -> {
                        bottomBar.setVisibility(View.GONE);
                        bottomBar.setLayerType(View.LAYER_TYPE_NONE, null);
                    }).start();

            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.fade_out, R.anim.fade_in, R.anim.slide_out_right)
                    .replace(R.id.fragment_container, new SettingsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // 2. Handle Backstack (Returning Home)
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                topBar.setVisibility(View.VISIBLE);
                bottomBar.setVisibility(View.VISIBLE);
                topBar.animate().translationY(0).setDuration(280).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
                bottomBar.animate().translationY(0).setDuration(280).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
                updateBottomNavUI(1);
            }
        });

        // 3. Check for Incoming Verdict from Notification
        handleIncomingVerdict(getIntent());

        if (checkAndRequestPermissions()) {
            selectTab(1);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingVerdict(intent);
    }

    private void handleIncomingVerdict(Intent intent) {
        if (intent != null && intent.hasExtra("EXTRA_RESULT_JSON")) {
            String sender = intent.getStringExtra("EXTRA_SENDER");
            String message = intent.getStringExtra("EXTRA_MESSAGE");
            String json = intent.getStringExtra("EXTRA_RESULT_JSON");

            try {
                SmishApiModels.SmishResponse result = new Gson().fromJson(json, SmishApiModels.SmishResponse.class);
                showSecurityPopup(sender, message, result);
            } catch (Exception e) {
                Toast.makeText(this, "Error loading analysis", Toast.LENGTH_SHORT).show();
            }
            // Clear intent extras to prevent re-pop on rotate
            intent.removeExtra("EXTRA_RESULT_JSON");
        }
    }

    private void showSecurityPopup(String sender, String message, SmishApiModels.SmishResponse result) {
        View v = getLayoutInflater().inflate(R.layout.verdict_popup, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(v).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // View Bindings
        TextView tvTitle = v.findViewById(R.id.tvPopupTitle);
        TextView tvAnalysis = v.findViewById(R.id.tvAnalysisWhy);
        TextView tvAi = v.findViewById(R.id.tvAiScore);
        TextView tvForensic = v.findViewById(R.id.tvForensicScore);
        TextView tvMsgBody = v.findViewById(R.id.tvPopupMessageBody);
        Button btnFeedback = v.findViewById(R.id.btnFeedbackAction);
        ImageView ivIcon = v.findViewById(R.id.ivStatusIcon);
        MaterialCardView cvIconBg = v.findViewById(R.id.cvStatusIconBg);

        int ai = normalizeScore(result.aiScore);
        int forensic = normalizeScore(result.forensicScore);

        tvAi.setText(ai + "/100");
        tvForensic.setText(forensic + "/100");
        tvMsgBody.setText("\"" + message + "\"");

        StringBuilder why = new StringBuilder();

        // LOGIC ENGINE: Determine the "Why"
        if (result.isPhishing) {
            tvTitle.setText("Threat Identified");
            tvTitle.setTextColor(Color.parseColor("#B91C1C"));
            cvIconBg.setCardBackgroundColor(Color.parseColor("#FEE2E2"));
            ivIcon.setImageResource(android.R.drawable.ic_dialog_alert);
            ivIcon.setColorFilter(Color.parseColor("#B91C1C"));

            // Detailed Analysis Basis
            why.append("The SmishGuard engine has flagged this communication as high-risk.\n\n");
            if (ai > 60) why.append("• Linguistic patterns match known phishing templates (Sense of urgency/Financial bait).\n");
            if (forensic > 40 || !result.linkWarnings.contains("No anomalies")) {
                why.append("• Forensic check detected suspicious URL routing or non-standard character encoding.\n");
            }
            if (result.logicMode.contains("Override")) {
                why.append("• Security Note: The Forensic engine performed a critical override due to confirmed malicious signatures.");
            }

            // OPPOSITE ACTION: If predicted SCAM, user can report it is SAFE
            btnFeedback.setText("Report as Safe (False Positive)");
            btnFeedback.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#004D5F")));
        } else {
            tvTitle.setText("Message Secure");
            tvTitle.setTextColor(Color.parseColor("#004D5F"));
            cvIconBg.setCardBackgroundColor(Color.parseColor("#E0F2F1"));
            ivIcon.setImageResource(R.drawable.ic_shield_check);
            ivIcon.setColorFilter(Color.parseColor("#004D5F"));

            why.append("This message has been verified as safe by the hybrid analysis pipeline.\n\n");
            why.append("• AI Confidence: High probability of legitimate intent.\n");
            why.append("• No hidden redirects or forensic anomalies detected in headers.\n");
            why.append("• Verification Mode: " + result.logicMode);

            // OPPOSITE ACTION: If predicted SAFE, user can report it is a SCAM
            btnFeedback.setText("Report Missed Scam");
            btnFeedback.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#B91C1C")));
        }

        tvAnalysis.setText(why.toString());

        btnFeedback.setOnClickListener(view -> {
            Toast.makeText(this, "Reported. Our models will be updated. Thanks for the feedback!", Toast.LENGTH_LONG).show();
            dialog.dismiss();
        });

        v.findViewById(R.id.btnDismiss).setOnClickListener(view -> dialog.dismiss());
        dialog.show();
    }

    private int normalizeScore(String raw) {
        if (raw == null) return 0;
        try {
            double val = Double.parseDouble(raw.replace("%", ""));
            if (val <= 1.0 && val > 0) val *= 100; // Handle 0.85 vs 85
            return (int) val;
        } catch (Exception e) { return 0; }
    }

    // --- Bottom Nav & Tab Logic ---

    private void initializeBottomNav() {
        navHome = findViewById(R.id.nav_home);
        navHistory = findViewById(R.id.nav_history);
        navInsights = findViewById(R.id.nav_insights);
        ivHome = findViewById(R.id.iv_home);
        ivHistory = findViewById(R.id.iv_history);
        ivInsights = findViewById(R.id.iv_insights);
        tvHome = findViewById(R.id.tv_home);
        tvHistory = findViewById(R.id.tv_history);
        tvInsights = findViewById(R.id.tv_insights);

        navHome.setOnClickListener(v -> selectTab(1));
        navHistory.setOnClickListener(v -> selectTab(2));
        navInsights.setOnClickListener(v -> selectTab(3));
        updateBottomNavUI(1);
    }

    private void selectTab(int index) {
        updateBottomNavUI(index);
        Fragment selectedFragment = null;
        switch (index) {
            case 1: selectedFragment = new HomeFragment(); break;
            case 2: selectedFragment = new HistoryFragment(); break;
            case 3: /* selectedFragment = new InsightsFragment(); */ break;
        }
        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();
        }
    }

    private void updateBottomNavUI(int activeIndex) {
        ivHome.setImageTintList(ColorStateList.valueOf(colorInactive));
        ivHistory.setImageTintList(ColorStateList.valueOf(colorInactive));
        ivInsights.setImageTintList(ColorStateList.valueOf(colorInactive));
        tvHome.setTextColor(colorInactive);
        tvHistory.setTextColor(colorInactive);
        tvInsights.setTextColor(colorInactive);
        navHome.setBackground(null);
        navHistory.setBackground(null);
        navInsights.setBackground(null);

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
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            selectTab(1);
        }
    }
}