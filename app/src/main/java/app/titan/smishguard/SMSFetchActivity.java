package app.titan.smishguard;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class SMSFetchActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 101;

    private ListView listView;
    private RadioGroup filterGroup;
    private Button btnRunScan;
    private TextView tvNormalCount, tvScamCount;

    private ArrayList<String> originalSmsList = new ArrayList<>();
    private ArrayList<String> scannedResults = new ArrayList<>();
    private ArrayList<String> filteredScams = new ArrayList<>();

    private SmishEngine engine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_fetch);

        listView = findViewById(R.id.smsListView);
        filterGroup = findViewById(R.id.filterGroup);
        btnRunScan = findViewById(R.id.btnRunScan);
        tvNormalCount = findViewById(R.id.tvNormalCount);
        tvScamCount = findViewById(R.id.tvScamCount);

        // Disable button initially
        btnRunScan.setEnabled(false);
        btnRunScan.setText("Initializing AI Engine...");

        // Initialize engine
        engine = new SmishEngine(this);

        // 🔥 Check readiness in background (simple polling)
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                if (engine != null && engine.isReady()) {
                    Log.d("SmishGuard", "✅ Engine READY in Activity");

                    btnRunScan.setEnabled(true);
                    btnRunScan.setText("Start Forensic Audit");

                    Toast.makeText(SMSFetchActivity.this, "AI Engine Ready!", Toast.LENGTH_SHORT).show();

                } else {
                    Log.d("SmishGuard", "⏳ Engine NOT ready yet...");
                    // Retry after 1 sec
                    new Handler().postDelayed(this, 1000);
                }
            }
        }, 1000);

        btnRunScan.setOnClickListener(v -> runBulkScan());

        // Permission check
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_SMS},
                    SMS_PERMISSION_CODE
            );

        } else {
            fetchSMS();
        }
    }

    private void fetchSMS() {
        originalSmsList.clear();

        Cursor cursor = getContentResolver().query(
                Uri.parse("content://sms/inbox"),
                null,
                null,
                null,
                "date DESC"
        );

        if (cursor != null && cursor.moveToFirst()) {

            int indexBody = cursor.getColumnIndexOrThrow("body");
            int indexAddress = cursor.getColumnIndexOrThrow("address");

            do {
                originalSmsList.add(
                        "From: " + cursor.getString(indexAddress) +
                                "\n" + cursor.getString(indexBody)
                );
            } while (cursor.moveToNext());

            cursor.close();
        }

        updateListView(originalSmsList);
    }

    private void runBulkScan() {

        Log.d("SmishGuard", "🔍 Scan button clicked");

        if (engine == null) {
            Toast.makeText(this, "Engine not initialized!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!engine.isReady()) {
            Toast.makeText(this, "AI Engine not ready yet!", Toast.LENGTH_SHORT).show();
            Log.e("SmishGuard", "❌ Engine NOT READY at scan time");
            return;
        }

        scannedResults.clear();
        filteredScams.clear();

        int scamCount = 0;

        for (String sms : originalSmsList) {

            SmishEngine.SmishResult result = engine.analyze(sms);

            if (result.isPhishing) {
                String tagged = "🔴 [SCAM: " +
                        String.format("%.1f", result.score) +
                        "%]\n" + sms;

                scannedResults.add(tagged);
                filteredScams.add(tagged);
                scamCount++;

            } else {
                scannedResults.add("🟢 [SAFE]\n" + sms);
            }
        }

        tvScamCount.setText("Scams: " + scamCount);
        tvNormalCount.setText("Safe: " + (scannedResults.size() - scamCount));

        filterGroup.setVisibility(View.VISIBLE);

        filterGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbShowScams) {
                updateListView(filteredScams);
            } else {
                updateListView(scannedResults);
            }
        });

        updateListView(scannedResults);

        Toast.makeText(this, "Audit Complete!", Toast.LENGTH_SHORT).show();
    }

    private void updateListView(ArrayList<String> data) {
        SMSAdapter adapter = new SMSAdapter(this, data, engine);
        listView.setAdapter(adapter);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == SMS_PERMISSION_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            fetchSMS();
        }
    }
}