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
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize UI components
        listView = view.findViewById(R.id.smsListView);
        engine = new SmishEngine();

        // Check if listView was found successfully
        if (listView != null) {
            fetchSMS();
        } else {
            Log.e("SmishGuard", "Error: smsListView not found in fragment_home.xml");
        }

        return view;
    }

    private void fetchSMS() {
        if (getContext() == null) return;

        smsList.clear();
        try (Cursor cursor = requireContext().getContentResolver().query(
                Uri.parse("content://sms/inbox"),
                null, null, null, "date DESC"
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                int indexBody = cursor.getColumnIndexOrThrow("body");
                int indexAddress = cursor.getColumnIndexOrThrow("address");
                do {
                    String address = cursor.getString(indexAddress);
                    String body = cursor.getString(indexBody);
                    smsList.add("From: " + address + "\n" + body);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("SmishGuard", "Cursor error: " + e.getMessage());
        }

        // Set the adapter
        if (listView != null) {
            listView.setAdapter(new SMSAdapter(requireContext(), smsList));
        }

        // Run the API Test after fetching
        runApiTest();
    }

    private void runApiTest() {
        engine.checkMessage("Hello Binjo", new SmishEngine.ApiCallback() {
            @Override
            public void onSuccess(SmishApiModels.SmishResponse result) {
                // Ensure fragment is still visible before showing toast
                if (isAdded() && getContext() != null) {
                    Toast.makeText(requireContext(), "API Success! Score: " + result.finalRiskScore, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String error) {
                Log.e("SmishGuard", "API Failure: " + error);
            }
        });
    }
}