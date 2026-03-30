package app.titan.smishguard;

import android.Manifest;
import android.content.ContentResolver;
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
import java.io.IOException;
import java.util.ArrayList;

public class SMSFetchActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 101;
    private ListView listView;

    // 1. CLASS LEVEL VARIABLES (Crucial)
    private ArrayList<String> smsList = new ArrayList<>();
    private ArrayList<String> scamList = new ArrayList<>();
    private ArrayList<String> safeList = new ArrayList<>();

    private SmishEngine engine; // The "Brain"
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
        try {
            engine = new SmishEngine(this);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load AI model!", Toast.LENGTH_LONG).show();
        }

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

    private void fetchSMS() {
        // FIX: Don't re-declare 'ArrayList<String> smsList' here! Use the class variable.
        smsList.clear();
        Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, "date DESC");

        if (cursor != null && cursor.moveToFirst()) {
            int indexBody = cursor.getColumnIndex("body");
            int indexAddress = cursor.getColumnIndex("address");
            do {
                smsList.add("From: " + cursor.getString(indexAddress) + "\n" + cursor.getString(indexBody));
            } while (cursor.moveToNext());
            cursor.close();
        }

        // Use our custom adapter to show the initial list
        updateListView(smsList);
    }

    private void runBulkScan() {
        if (engine == null) {
            Toast.makeText(this, "Engine not ready!", Toast.LENGTH_SHORT).show();
            return;
        }

        scamList.clear();
        safeList.clear();

        for (String fullSms : smsList) {
            // Run the v2 Hybrid Engine Analysis
            SmishEngine.SmishResult result = engine.analyze(fullSms);

            if (result.isPhishing) {
                scamList.add("🔴 [FLAGGED]\n" + fullSms);
            } else {
                safeList.add("🟢 [SAFE]\n" + fullSms);
            }
        }

        // Update Summary Counts
        if (tvNormalCount != null) tvNormalCount.setText("Normal: " + safeList.size());
        if (tvScamCount != null) tvScamCount.setText("Scams: " + scamList.size());

        // Show filters
        filterGroup.setVisibility(View.VISIBLE);

        filterGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbShowScams) {
                updateListView(scamList);
            } else if (checkedId == R.id.rbShowSafe) {
                updateListView(safeList);
            } else {
                updateListView(smsList);
            }
        });

        Toast.makeText(this, "Scan Complete: Found " + scamList.size() + " threats.", Toast.LENGTH_SHORT).show();
    }

    private void updateListView(ArrayList<String> data) {
        // Pass the engine to the adapter for per-row coloring
        SMSAdapter adapter = new SMSAdapter(this, data, engine);
        listView.setAdapter(adapter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchSMS();
        } else {
            Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show();
        }
    }
}