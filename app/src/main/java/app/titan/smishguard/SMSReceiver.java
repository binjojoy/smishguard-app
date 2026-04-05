package app.titan.smishguard;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
            // 1. Get the array of message fragments
            SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);

            if (messages != null && messages.length > 0) {
                StringBuilder fullMessage = new StringBuilder();
                String sender = messages[0].getDisplayOriginatingAddress();

                // 2. Loop only to CONCATENATE the text
                for (SmsMessage sms : messages) {
                    if (sms != null) {
                        fullMessage.append(sms.getDisplayMessageBody());
                    }
                }

                // 3. NOW process the full, single string ONE time
                processAndNotify(context, sender, fullMessage.toString(), pendingResult);
            } else {
                pendingResult.finish();
            }

        } else if ("app.titan.smishguard.TEST_SMS".equals(action)) {
            // ADB COMMAND TEST (Usually single part anyway)
            String sender = "Admin";
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

        int color;
        int smallIcon;
        String statusTitle;
        String securitySummary;

        // Use Title Case for a cleaner, modern look
        if (verdict.equals("SCAM")) {
            statusTitle = "Scam Detected";
            securitySummary = "High-risk smishing alert";
            color = Color.parseColor("#B71C1C");
            smallIcon = android.R.drawable.ic_dialog_alert;
        } else if (verdict.equals("SUSPICIOUS")) {
            statusTitle = "Suspicious Content";
            securitySummary = "Potential risk identified";
            color = Color.parseColor("#E65100");
            smallIcon = android.R.drawable.ic_dialog_info;
        } else {
            statusTitle = "Message Verified";
            securitySummary = "No threats detected";
            color = Color.parseColor("#004D40");
            smallIcon = R.drawable.ic_shield_check;
        }

        Intent intent = new Intent(context, SMSFetchActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(smallIcon)
                .setSubText(securitySummary) // Sentence case for the summary
                .setContentTitle(statusTitle) // Title case for the title
                .setContentText("Sender: " + sender)
                .setColor(color)
                .setColorized(verdict.equals("SCAM"))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setContentIntent(pIntent)
                .addAction(new NotificationCompat.Action.Builder(
                        android.R.drawable.ic_menu_view, "View Analysis", pIntent).build())
                .setStyle(new NotificationCompat.BigTextStyle()
                        .setBigContentTitle(statusTitle)
                        .setSummaryText("Security Briefing")
                        .bigText("Sender: " + sender + "\n" +
                                "Message: " + message + "\n\n" +
                                "Analysis: " + result.logicMode + " (" + result.finalRiskScore + ")" +
                                (result.linkWarnings.contains("No anomalies") ? "" : "\n\nAlert: " + result.linkWarnings)));

        nm.notify(sender.hashCode(), builder.build());
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