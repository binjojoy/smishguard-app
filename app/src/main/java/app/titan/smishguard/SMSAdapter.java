package app.titan.smishguard;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;

public class SMSAdapter extends ArrayAdapter<String> {

    public SMSAdapter(Context context, ArrayList<String> messages) {
        super(context, 0, messages);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_verdict, parent, false);
        }

        // Get and Split the Data: "Sender|Verdict|Body"
        String rawData = getItem(position);
        if (rawData == null) return convertView;

        String[] parts = rawData.split("\\|");

        TextView tvSender = convertView.findViewById(R.id.tvSender);
        TextView tvVerdict = convertView.findViewById(R.id.tvVerdictBadge);
        TextView tvBody = convertView.findViewById(R.id.tvMessageBody);

        if (parts.length >= 3) {
            String sender = parts[0];
            String verdict = parts[1];
            String body = parts[2];

            tvSender.setText(sender);
            tvBody.setText(body);
            tvVerdict.setText(verdict);

            // APPLY THE STYLE DYNAMICALLY
            switch (verdict) {
                case "SCAM":
                    tvVerdict.setTextColor(Color.parseColor("#B71C1C")); // Dark Red Text
                    tvVerdict.setBackgroundResource(R.drawable.tag_scam_bg);
                    break;
                case "SUSPICIOUS":
                    tvVerdict.setTextColor(Color.parseColor("#E65100")); // Dark Orange Text
                    tvVerdict.setBackgroundResource(R.drawable.tag_suspicious_bg);
                    break;
                case "SAFE":
                default:
                    tvVerdict.setTextColor(Color.parseColor("#004D40")); // Dark Teal Text
                    tvVerdict.setBackgroundResource(R.drawable.tag_safe_bg);
                    break;
            }
        }

        return convertView;
    }
}