package app.titan.smishguard;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import androidx.core.app.NotificationCompat;

public class SMSReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "smish_alerts";
    private SmishEngine engine;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (engine == null) engine = new SmishEngine();

        final PendingResult pendingResult = goAsync();

        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(action)) {
            // REAL SMS FLOW
            for (SmsMessage sms : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                processAndNotify(context, sms.getDisplayOriginatingAddress(), sms.getDisplayMessageBody(), pendingResult);
            }
        } else if ("app.titan.smishguard.TEST_SMS".equals(action)) {
            // ADB TEST FLOW
            String sender = "ADB_TESTER";
            String messageBody = intent.getStringExtra("msg");
            if (messageBody == null) messageBody = "Test message";

            Log.d("SmishGuard", "🧪 Testing API with: " + messageBody);
            processAndNotify(context, sender, messageBody, pendingResult);
        } else {
            pendingResult.finish();
        }
    }

    private void processAndNotify(Context context, String sender, String message, PendingResult pendingResult) {
        engine.checkMessage(message, new SmishEngine.ApiCallback() {
            @Override
            public void onSuccess(SmishApiModels.SmishResponse result) {
                showVerdictNotification(context, sender, message, result);
                pendingResult.finish();
            }

            @Override
            public void onError(String error) {
                Log.e("SmishGuard", "Check failed: " + error);
                showSimpleNotification(context, sender, message);
                pendingResult.finish();
            }
        });
    }

    private void showVerdictNotification(Context context, String sender, String message, SmishApiModels.SmishResponse result) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        setupChannel(nm);

        // 1. Parse the score (it comes as "45.00%" from your server, so we strip the %)
        double score = 0;
        try {
            score = Double.parseDouble(result.finalRiskScore.replace("%", ""));
        } catch (Exception e) {
            Log.e("SmishGuard", "Score parse error");
        }

        String title;
        int color;
        int icon;

        // 2. THE VERDICT LOGIC
        // Trust the server's boolean for the RED zone
        if (result.isPhishing) {
            title = "🚩 SCAM DETECTED";
            color = Color.RED;
            icon = android.R.drawable.ic_dialog_alert;
        }
        // If server says it's NOT phishing, but score is still high-ish (e.g. 20% to 45%)
        // OR if there are actual link warnings (not the "No anomalies" filler)
        else if (score > 20 || (result.linkWarnings != null && !result.linkWarnings.contains("No anomalies"))) {
            title = "⚠️ SUSPICIOUS CONTENT";
            color = Color.parseColor("#FFA500"); // Orange
            icon = android.R.drawable.ic_dialog_info;
        }
        // Truly clean
        else {
            title = "✅ MESSAGE SAFE";
            color = Color.parseColor("#004D5F"); // Your MACE Teal
            icon = R.drawable.ic_shield_check;
        }

        // 3. CLEAN UP THE TEXT
        // Don't show "No anomalies" to the user; keep it clean
        String displayWarnings = result.linkWarnings.contains("No anomalies") ? "" : "\n\n" + result.linkWarnings;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(result.logicMode + " | Risk: " + result.finalRiskScore)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Message: " + message + displayWarnings))
                .setColor(color)
                .setColorized(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true);

        nm.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void showSimpleNotification(Context context, String sender, String message) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        setupChannel(nm);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("New Message: " + sender)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        nm.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void setupChannel(NotificationManager nm) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "SmishGuard Alerts", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Real-time Smishing Detection");
            channel.enableLights(true);
            channel.setLightColor(Color.CYAN);
            nm.createNotificationChannel(channel);
        }
    }
}