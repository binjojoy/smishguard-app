package app.titan.smishguard;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;

public class SMSReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "smish_alerts";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d("SmishGuard", "📡 ON_RECEIVE TRIGGERED: " + action);

        // This Toast will show even if the notification is blocked
        Toast.makeText(context, "SmishGuard: Signal Received!", Toast.LENGTH_SHORT).show();

        if ("android.provider.Telephony.SMS_RECEIVED".equals(action)) {
            for (SmsMessage sms : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                showNotification(context, sms.getDisplayOriginatingAddress(), sms.getDisplayMessageBody());
            }
        } else if ("app.titan.smishguard.TEST_SMS".equals(action)) {
            String msg = intent.getStringExtra("msg");
            showNotification(context, "ADB TESTER", msg != null ? msg : "Test Successful!");
        }
    }

    private void showNotification(Context context, String sender, String message) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Alerts", NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("New Message: " + sender)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_MAX) // MAX priority for heads-up
                .setDefaults(NotificationCompat.DEFAULT_ALL)   // Sound + Vibrate
                .setAutoCancel(true);

        nm.notify((int) System.currentTimeMillis(), builder.build());
    }
}