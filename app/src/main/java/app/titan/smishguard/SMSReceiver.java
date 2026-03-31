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

        // goAsync allows the receiver to stay alive for the network call (approx 10s)
        final PendingResult pendingResult = goAsync();

        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(action)) {
            // REAL SMS ARRIVAL
            for (SmsMessage sms : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                processAndNotify(context, sms.getDisplayOriginatingAddress(), sms.getDisplayMessageBody(), pendingResult);
            }
        } else if ("app.titan.smishguard.TEST_SMS".equals(action)) {
            // ADB COMMAND TEST
            String sender = "ADB_TESTER";
            String msg = intent.getStringExtra("msg");
            processAndNotify(context, sender, msg != null ? msg : "Test Message", pendingResult);
        } else {
            pendingResult.finish();
        }
    }

    private void processAndNotify(Context context, String sender, String message, PendingResult pendingResult) {
        engine.checkMessage(message, new SmishEngine.ApiCallback() {
            @Override
            public void onSuccess(SmishApiModels.SmishResponse result) {
                SmishRepository repo = new SmishRepository(context);

                // 1. DETERMINE VERDICT CATEGORY
                String verdictType = result.isPhishing ? "SCAM" : "SAFE";
                double score = 0;
                try {
                    score = Double.parseDouble(result.finalRiskScore.replace("%", ""));
                } catch (Exception e) { Log.e("SmishGuard", "Score parse error"); }

                // Check for the "Orange" Suspicious zone
                if (!result.isPhishing && (score > 20 || (result.linkWarnings != null && !result.linkWarnings.contains("No anomalies")))) {
                    verdictType = "SUSPICIOUS";
                }

                // 2. UPDATE REPOSITORY (Dynamic Dashboard)
                repo.addVerdict(sender, message, verdictType);

                if (result.isPhishing) {
                    repo.incrementScamCount();
                    // NEW: Dynamic Flagged Section - Save brands found by Pipeline B
                    if (result.entitiesDetected != null && !result.entitiesDetected.isEmpty()) {
                        repo.saveFlaggedBrands(result.entitiesDetected);
                    }
                }

                // 3. TRIGGER NOTIFICATION
                showVerdictNotification(context, sender, message, result, verdictType);
                pendingResult.finish();
            }

            @Override
            public void onError(String error) {
                Log.e("SmishGuard", "API Error: " + error);
                pendingResult.finish();
            }
        });
    }

    private void showVerdictNotification(Context context, String sender, String message, SmishApiModels.SmishResponse result, String verdict) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        setupChannel(nm);

        int color = Color.parseColor("#004D5F"); // SmishGuard Teal
        int icon = R.drawable.ic_shield_check;
        String title = "✅ MESSAGE SAFE";

        if (verdict.equals("SCAM")) {
            title = "🚩 SCAM DETECTED";
            color = Color.RED;
            icon = android.R.drawable.ic_dialog_alert;
        } else if (verdict.equals("SUSPICIOUS")) {
            title = "⚠️ SUSPICIOUS CONTENT";
            color = Color.parseColor("#FFA500"); // Orange
            icon = android.R.drawable.ic_dialog_info;
        }

        String displayWarnings = result.linkWarnings.contains("No anomalies") ? "" : "\n\n" + result.linkWarnings;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(result.logicMode + " | Risk: " + result.finalRiskScore)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .setBigContentTitle(title)
                        .bigText("From: " + sender + "\nMessage: " + message + displayWarnings))
                .setColor(color)
                .setColorized(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true);

        nm.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void setupChannel(NotificationManager nm) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "SmishGuard Alerts", NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);
            nm.createNotificationChannel(channel);
        }
    }
}