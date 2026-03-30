package app.titan.smishguard;

import android.content.Context;

/**
 * SmishEngine: Cloud-Ready Edition.
 * All local TFLite logic and assets have been purged.
 */
public class SmishEngine {

    public SmishEngine(Context context) {
        // No longer needs to load files or initialize GMS
    }

    public boolean isReady() {
        // Always returns true now because there's no local model to "wait" for
        return true;
    }

    /**
     * Placeholder analyze method.
     * For now, it treats everything as safe so the app runs.
     * We will plug the API call here in the next step.
     */
    public SmishResult analyze(String text) {
        // Defaulting to "Safe" (isPhishing = false, score = 0)
        return new SmishResult(false, 0.0f, "Local Bypass (Cleaning Mode)");
    }

    public static class SmishResult {
        public boolean isPhishing;
        public float score;
        public String mode;

        public SmishResult(boolean isPhishing, float score, String mode) {
            this.isPhishing = isPhishing;
            this.score = score;
            this.mode = mode;
        }
    }
}