package app.titan.smishguard;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_fetch);

        listView = findViewById(R.id.smsListView);

        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        // Build the list of permissions needed
        ArrayList<String> permissionsNeeded = new ArrayList<>();
        permissionsNeeded.add(Manifest.permission.READ_SMS);
        permissionsNeeded.add(Manifest.permission.RECEIVE_SMS);

        // Add Notification permission only for Android 13 (API 33) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        // Filter out permissions we already have
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
        } else {
            // We have all permissions, proceed to fetch
            fetchSMS();
        }
    }

    private void fetchSMS() {
        smsList.clear();
        Cursor cursor = getContentResolver().query(
                Uri.parse("content://sms/inbox"),
                null, null, null, "date DESC"
        );

        if (cursor != null && cursor.moveToFirst()) {
            int indexBody = cursor.getColumnIndexOrThrow("body");
            int indexAddress = cursor.getColumnIndexOrThrow("address");

            do {
                smsList.add("From: " + cursor.getString(indexAddress) + "\n" + cursor.getString(indexBody));
            } while (cursor.moveToNext());
            cursor.close();
        }

        listView.setAdapter(new SMSAdapter(this, smsList));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                fetchSMS();
            } else {
                // At least check if READ_SMS was granted to show the list
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
                    fetchSMS();
                    Toast.makeText(this, "Some features (background alerts) may not work.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "SMS Permission is required to show the inbox.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}