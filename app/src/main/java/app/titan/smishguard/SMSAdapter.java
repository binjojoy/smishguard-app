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
    private SmishEngine engine;

    public SMSAdapter(Context context, List<String> messages, SmishEngine engine) {
        super(context, 0, messages);
        this.engine = engine;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        }

        TextView tv = convertView.findViewById(android.R.id.text1);
        String fullSms = getItem(position);

        // Run v2 Neuro-Symbolic Analysis
        SmishEngine.SmishResult result = engine.analyze(fullSms);

        tv.setText(fullSms + "\n\n[" + result.mode + " | Score: " + String.format("%.1f", result.score) + "%]");

        if (result.isPhishing) {
            convertView.setBackgroundColor(Color.parseColor("#FFEBEE")); // Warning Red
            tv.setTextColor(Color.parseColor("#B71C1C"));
        } else {
            convertView.setBackgroundColor(Color.WHITE);
            tv.setTextColor(Color.BLACK);
        }

        return convertView;
    }
}
