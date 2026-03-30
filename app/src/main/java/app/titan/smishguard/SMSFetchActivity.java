package app.titan.smishguard;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;

public class SMSFetchActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 101;
    private ListView listView;

    // 1. CLASS LEVEL VARIABLES
    // These hold your data across the entire activity lifecycle
    private ArrayList<String> smsList = new ArrayList<>();
    private ArrayList<String> scamList = new ArrayList<>();
    private ArrayList<String> safeList = new ArrayList<>();

    private SmishEngine engine;
    private RadioGroup filterGroup;
    private Button btnRunScan;
    private TextView tvNormalCount, tvScamCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_fetch);

        // 2. LINK UI ELEMENTS
        listView = findViewById(R.id.smsListView);
        filterGroup = findViewById(R.id.filterGroup);
        btnRunScan = findViewById(R.id.btnRunScan);
        tvNormalCount = findViewById(R.id.tvNormalCount);
        tvScamCount = findViewById(R.id.tvScamCount);

        // 3. INITIALIZE THE AI ENGINE
        // Note: The new engine initializes asynchronously via Google Play Services
        engine = new SmishEngine(this);

        // 4. SETUP SCAN BUTTON
        btnRunScan.setOnClickListener(v -> runBulkScan());

        // 5. CHECK PERMISSIONS
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, SMS_PERMISSION_CODE);
        } else {
            fetchSMS();
        }
    }

    /**
     * Queries the Android SMS Content Provider and populates the main list.
     */
    private void fetchSMS() {
        // Clear the class-level list to avoid duplicates on refresh
        smsList.clear();

        Cursor cursor = getContentResolver().query(
                Uri.parse("content://sms/inbox"),
                null,
                null,
                null,
                "date DESC"
        );

        if (cursor != null && cursor.moveToFirst()) {
            int indexBody = cursor.getColumnIndex("body");
            int indexAddress = cursor.getColumnIndex("address");
            do {
                String address = cursor.getString(indexAddress);
                String body = cursor.getString(indexBody);
                smsList.add("From: " + address + "\n" + body);
            } while (cursor.moveToNext());
            cursor.close();
        }

        // Show the initial "All Messages" view
        updateListView(smsList);
    }

    /**
     * The Bulk Scan (Audit Mode) logic.
     * Iterates through all messages and classifies them using the Hybrid Engine.
     */
    private void runBulkScan() {
        // Safety check: Ensure engine has finished loading from GMS
        if (engine == null) {
            Toast.makeText(this, "AI Engine is waking up... try again in a second", Toast.LENGTH_SHORT).show();
            return;
        }

        scamList.clear();
        safeList.clear();

        if (smsList.isEmpty()) {
            Toast.makeText(this, "No messages found to scan.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Iterate through all messages currently in the inbox list
        for (String fullSms : smsList) {
            // Run the Hybrid N-gram Analysis (AI + Forensics)
            SmishEngine.SmishResult result = engine.analyze(fullSms);

            if (result.isPhishing) {
                // Prepend a tag for the UI
                scamList.add("🔴 [FLAGGED: " + String.format("%.1f", result.score) + "%]\n" + fullSms);
            } else {
                safeList.add("🟢 [SAFE]\n" + fullSms);
            }
        }

        // 6. UPDATE UI WITH RESULTS
        if (tvNormalCount != null) tvNormalCount.setText("Normal: " + safeList.size());
        if (tvScamCount != null) tvScamCount.setText("Scams: " + scamList.size());

        // Make the filter options visible now that the scan is done
        filterGroup.setVisibility(View.VISIBLE);

        // Handle the RadioButton filtering logic
        filterGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbShowScams) {
                updateListView(scamList);
            } else if (checkedId == R.id.rbShowSafe) {
                updateListView(safeList);
            } else {
                // Show original list
                updateListView(smsList);
            }
        });

        Toast.makeText(this, "Audit Complete! Found " + scamList.size() + " threats.", Toast.LENGTH_LONG).show();
    }

    /**
     * Refreshes the ListView using the custom SMSAdapter.
     */
    private void updateListView(ArrayList<String> data) {
        // Passing 'engine' to the adapter allows for dynamic row coloring/score display
        SMSAdapter adapter = new SMSAdapter(this, data, engine);
        listView.setAdapter(adapter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchSMS();
        } else {
            Toast.makeText(this, "Permission denied! SmishGuard cannot read your inbox.", Toast.LENGTH_LONG).show();
        }
    }
}