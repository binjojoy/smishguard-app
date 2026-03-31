package app.titan.smishguard;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;

public class SmishRepository {
    private static final String PREF_NAME = "smish_prefs";
    private static final String KEY_SCAM_COUNT = "scam_count";
    private static final String KEY_VERDICTS = "recent_verdicts";
    private static final String KEY_FLAGGED_BRANDS = "flagged_brands";

    private final SharedPreferences prefs;
    private final Gson gson;

    public SmishRepository(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    /**
     * Increments the total scam counter shown on the dashboard.
     */
    public void incrementScamCount() {
        int current = getScamCount();
        prefs.edit().putInt(KEY_SCAM_COUNT, current + 1).apply();
    }

    public int getScamCount() {
        return prefs.getInt(KEY_SCAM_COUNT, 0);
    }

    /**
     * Saves detected entities (brands) to the 'Flagged' section.
     * Ensures only unique brands are stored.
     */
    public void saveFlaggedBrands(List<String> newBrands) {
        if (newBrands == null || newBrands.isEmpty()) return;

        List<String> existing = getFlaggedBrands();

        for (String brand : newBrands) {
            // Capitalize for clean UI (e.g., "amazon" -> "Amazon")
            String formatted = brand.substring(0, 1).toUpperCase() + brand.substring(1).toLowerCase();

            if (!existing.contains(formatted)) {
                existing.add(0, formatted); // Add newest to the top
            }
        }

        // Limit the list to the top 3 unique brands to fit the Bento card
        if (existing.size() > 3) {
            existing = existing.subList(0, 3);
        }

        prefs.edit().putString(KEY_FLAGGED_BRANDS, gson.toJson(existing)).apply();
    }

    /**
     * Returns the list of currently flagged brands.
     */
    public List<String> getFlaggedBrands() {
        String json = prefs.getString(KEY_FLAGGED_BRANDS, "[]");
        List<String> list = gson.fromJson(json, new TypeToken<List<String>>(){}.getType());
        return (list != null) ? list : new ArrayList<>();
    }

    /**
     * Saves a scan result to the history list.
     */
    public void addVerdict(String sender, String message, String verdictType) {
        List<String> verdicts = getRecentVerdicts();

        // Format: "SENDER|VERDICT|MESSAGE"
        String entry = sender + "|" + verdictType + "|" + message;

        verdicts.add(0, entry);

        if (verdicts.size() > 10) {
            verdicts = verdicts.subList(0, 10);
        }

        String json = gson.toJson(verdicts);
        prefs.edit().putString(KEY_VERDICTS, json).apply();
    }

    public List<String> getRecentVerdicts() {
        String json = prefs.getString(KEY_VERDICTS, "[]");
        List<String> list = gson.fromJson(json, new TypeToken<List<String>>(){}.getType());
        return (list != null) ? list : new ArrayList<>();
    }
}