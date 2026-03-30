package app.titan.smishguard;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;

public class SMSFetchActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 101;
    private ListView listView;
    private ArrayList<String> smsList = new ArrayList<>();
    private SmishEngine engine; // Declare Engine

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_fetch);

        listView = findViewById(R.id.smsListView);
        engine = new SmishEngine(); // Initialize Engine

        checkAndRequestPermissions();
    }

    private void fetchSMS() {
        smsList.clear();
        // try-with-resources ensures cursor is closed automatically
        try (Cursor cursor = getContentResolver().query(
                Uri.parse("content://sms/inbox"),
                null, null, null, "date DESC"
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                int indexBody = cursor.getColumnIndexOrThrow("body");
                int indexAddress = cursor.getColumnIndexOrThrow("address");
                do {
                    smsList.add("From: " + cursor.getString(indexAddress) + "\n" + cursor.getString(indexBody));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("SmishGuard", "Cursor error: " + e.getMessage());
        }

        listView.setAdapter(new SMSAdapter(this, smsList));

        // 🔥 TRIGGER API TEST CALL
        runApiTest();
    }

    private void runApiTest() {
        String testText = "Hello Binjo";

        engine.checkMessage(testText, new SmishEngine.ApiCallback() {
            @Override
            public void onSuccess(SmishApiModels.SmishResponse result) {
                String logMsg = "API Success! Phishing: " + result.isPhishing + " | Score: " + result.finalRiskScore;
                Log.d("SmishGuard", logMsg);
                Toast.makeText(SMSFetchActivity.this, logMsg, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String error) {
                Log.e("SmishGuard", "API Failure: " + error);
                Toast.makeText(SMSFetchActivity.this, "API Failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ... Keep your checkAndRequestPermissions() and onRequestPermissionsResult() as they were ...

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
        } else {
            fetchSMS();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchSMS();
            }
        }
    }
}