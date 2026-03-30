package app.titan.smishguard;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private ListView listView;
    private ArrayList<String> smsList = new ArrayList<>();
    private SmishEngine engine;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        listView = view.findViewById(R.id.smsListView);
        engine = new SmishEngine();

        fetchSMS();
        return view;
    }

    private void fetchSMS() {
        smsList.clear();
        try (Cursor cursor = getContext().getContentResolver().query(
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
            Log.e("SmishGuard", "Fragment Cursor error: " + e.getMessage());
        }

        listView.setAdapter(new SMSAdapter(getContext(), smsList));
        runApiTest();
    }

    private void runApiTest() {
        engine.checkMessage("Hello Binjo", new SmishEngine.ApiCallback() {
            @Override
            public void onSuccess(SmishApiModels.SmishResponse result) {
                if (isAdded()) { // Check if fragment is still attached to UI
                    Toast.makeText(getContext(), "API Success! Score: " + result.finalRiskScore, Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onError(String error) {
                Log.e("SmishGuard", "API Failure: " + error);
            }
        });
    }
}