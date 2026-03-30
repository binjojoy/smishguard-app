package app.titan.smishguard;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.List;

public class SMSAdapter extends ArrayAdapter<String> {

    // ONLY 2 parameters here now
    public SMSAdapter(Context context, List<String> messages) {
        super(context, android.R.layout.simple_list_item_1, messages);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String sms = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        }

        TextView tv = convertView.findViewById(android.R.id.text1);
        if (sms != null) {
            tv.setText(sms);
        }

        return convertView;
    }
}