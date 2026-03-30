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
        String messageWithMarker = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        }

        TextView tv = convertView.findViewById(android.R.id.text1);
        tv.setText(messageWithMarker);
        tv.setPadding(30, 40, 30, 40);

        // FIX: Only color based on the marker added by the Activity
        if (messageWithMarker != null && messageWithMarker.contains("🔴")) {
            convertView.setBackgroundColor(Color.parseColor("#FFF1F0")); // Soft Red
            tv.setTextColor(Color.parseColor("#D32F2F"));
        } else if (messageWithMarker != null && messageWithMarker.contains("🟢")) {
            convertView.setBackgroundColor(Color.parseColor("#F6FFED")); // Soft Green
            tv.setTextColor(Color.parseColor("#388E3C"));
        } else {
            convertView.setBackgroundColor(Color.WHITE);
            tv.setTextColor(Color.BLACK);
        }

        return convertView;
    }
}