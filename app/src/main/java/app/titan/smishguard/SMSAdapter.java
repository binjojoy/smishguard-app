package app.titan.smishguard;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.List;

public class SMSAdapter extends ArrayAdapter<String> {

    public SMSAdapter(Context context, List<String> messages, SmishEngine engine) {
        // We keep 'engine' in the signature to match your Activity,
        // but we prioritize pre-scanned markers for performance.
        super(context, 0, messages);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String fullSms = getItem(position);

        if (convertView == null) {
            // Corrected: Uses the system layout resource (R.layout)
            convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        }

// Then you find the TextView inside that layout
        TextView tv = convertView.findViewById(android.R.id.text1);
        tv.setText(fullSms);
        tv.setPadding(30, 40, 30, 40); // Makes it look professional
        tv.setTextSize(14);

        // UI Logic based on the markers added in SMSFetchActivity
        if (fullSms.contains("🔴")) {
            // Light red background for scams
            convertView.setBackgroundColor(Color.parseColor("#FFF1F0"));
            tv.setTextColor(Color.parseColor("#D32F2F"));
        } else if (fullSms.contains("🟢")) {
            // Light green background for safe messages
            convertView.setBackgroundColor(Color.parseColor("#F6FFED"));
            tv.setTextColor(Color.parseColor("#388E3C"));
        } else {
            // Default view before the scan is run
            convertView.setBackgroundColor(Color.WHITE);
            tv.setTextColor(Color.BLACK);
        }

        return convertView;
    }
}