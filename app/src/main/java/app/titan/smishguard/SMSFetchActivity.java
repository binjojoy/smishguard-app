package app.titan.smishguard;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;

public class SMSFetchActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 101;
    private ListView listView;
    private ArrayList<String> smsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_fetch);

        listView = findViewById(R.id.smsListView);

        // Check for SMS permissions immediately on launch
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, SMS_PERMISSION_CODE);
        } else {
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
        if (requestCode == SMS_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchSMS();
        } else {
            Toast.makeText(this, "Permission denied to read SMS", Toast.LENGTH_SHORT).show();
        }
    }
}