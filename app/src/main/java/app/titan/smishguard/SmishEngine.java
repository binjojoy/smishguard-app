package app.titan.smishguard;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import com.google.android.gms.tflite.java.TfLite;
import org.tensorflow.lite.Interpreter;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmishEngine {
    private Interpreter interpreter;
    private final Map<String, Integer> vocabData = new HashMap<>();

    // Constants from android_config.json
    private final int MAX_LEN = 120;
    private final float SCAM_THRESHOLD = 0.05744f;

    // Forensic Constants for Hybrid Analysis
    private final String[] SUSPICIOUS_TLDS = {".xyz", ".top", ".club", ".ru", ".cn", ".live", ".click", ".link", ".cam"};
    private final String[] KNOWN_BRANDS = {"sbi", "hdfc", "icici", "axis", "netflix", "disney", "fedex", "ups", "amazon"};

    public SmishEngine(Context context) {
        // Initialize TFLite via Google Play Services to handle newer model opcodes
        TfLite.initialize(context).addOnSuccessListener(aVoid -> {
            try {
                Interpreter.Options options = new Interpreter.Options();
                // Ensure this filename matches your assets folder exactly
                interpreter = new Interpreter(loadModelFile(context, "engine1.tflite"), options);
                loadVocab(context, "vocab.txt");
                android.util.Log.d("SmishGuard", "✅ AI Engine Online (GMS Runtime)");
            } catch (Exception e) {
                android.util.Log.e("SmishGuard", "❌ Model Load Error: " + e.getMessage());
            }
        });
    }

    /**
     * Pipeline A: Preprocessing based on config steps 1-9
     */
    private String preprocess(String text) {
        // 1. Lowercase
        String processed = text.toLowerCase().trim();

        // 2 & 8. Homoglyph normalization
        processed = processed.replace("@", "a").replace("3", "e").replace("0", "o")
                .replace("1", "l").replace("5", "s").replace("!", "i");

        // 3, 4, 5. Token Replacement (matching config token names)
        processed = processed.replaceAll("https?://\\S+|www\\.\\S+", " url_token ");
        processed = processed.replaceAll("\\b[\\d\\-\\.\\(\\)\\s]{9,}\\b", " phone_token ");
        processed = processed.replaceAll("[\\$\\u20b9£€]\\d+", " money_token ");

        // 6 & 7. Segmentation (Replace dots and hyphens with spaces)
        processed = processed.replace(".", " ").replace("-", " ");

        // 9. Whitespace normalize
        return processed.replaceAll("\\s+", " ").trim();
    }

    /**
     * Pipeline A: AI Inference with Bigram Support (ngrams: 2)
     */
    public float getAiScore(String text) {
        if (interpreter == null) return 0f;

        String clean = preprocess(text);
        String[] words = clean.split("\\s+");
        ArrayList<Integer> tokens = new ArrayList<>();

        // 10. Vocab Lookup: Unigrams and Bigrams
        for (int i = 0; i < words.length; i++) {
            // Add Unigram
            tokens.add(vocabData.getOrDefault(words[i], 1)); // 1 is UNK_INDEX

            // Add Bigram (if context exists)
            if (i < words.length - 1) {
                String bigram = words[i] + " " + words[i + 1];
                if (vocabData.containsKey(bigram)) {
                    tokens.add(vocabData.get(bigram));
                }
            }
        }

        // 11. Pad or Truncate to exactly MAX_LEN (120)
        int[][] inputIds = new int[1][MAX_LEN];
        for (int i = 0; i < MAX_LEN; i++) {
            if (i < tokens.size()) {
                inputIds[0][i] = tokens.get(i);
            } else {
                inputIds[0][i] = 0; // 0 is PAD_INDEX
            }
        }

        float[][] output = new float[1][1];
        interpreter.run(inputIds, output);
        return output[0][0]; // Probability (0.0 to 1.0)
    }

    /**
     * Pipeline B: Forensics (Entropy calculation)
     */
    private double calculateEntropy(String domain) {
        if (domain == null || domain.isEmpty()) return 0;
        Map<Character, Integer> counts = new HashMap<>();
        for (char c : domain.toCharArray()) counts.put(c, counts.getOrDefault(c, 0) + 1);
        double entropy = 0;
        for (int count : counts.values()) {
            double p = (double) count / domain.length();
            entropy -= p * (Math.log(p) / Math.log(2));
        }
        return entropy;
    }

    /**
     * The Fusion Engine: Combines AI and Forensic signals
     */
    public SmishResult analyze(String text) {
        float rawAiScore = getAiScore(text); // 0.0 to 1.0
        float aiScorePercent = rawAiScore * 100f;
        float forensicScore = 0f;
        String mode = "Hybrid Consensus";
        boolean isCritical = false;

        // URL Forensic Extraction
        Pattern urlPattern = Pattern.compile("https?://(\\S+)");
        Matcher matcher = urlPattern.matcher(text);

        if (matcher.find()) {
            String domain = matcher.group(1).toLowerCase();

            // 1. High Entropy check (random-looking domains)
            if (calculateEntropy(domain) > 3.9) forensicScore += 35f;

            // 2. Suspicious TLD check
            for (String tld : SUSPICIOUS_TLDS) {
                if (domain.contains(tld)) {
                    forensicScore += 45f;
                    break;
                }
            }

            // 3. Brand Mismatch check
            for (String brand : KNOWN_BRANDS) {
                if (text.toLowerCase().contains(brand) && !domain.contains(brand)) {
                    forensicScore += 75f;
                    isCritical = true;
                    break;
                }
            }
        }

        // Fusion Logic
        float finalScore;
        if (isCritical) {
            finalScore = 100f;
            mode = "Forensic Override (Critical)";
        } else {
            // Weighted average: 50% AI, 50% Forensics
            finalScore = (aiScorePercent * 0.5f) + (forensicScore * 0.5f);
        }

        // Use the threshold from config (approx 5.74%)
        boolean isPhishing = finalScore >= (SCAM_THRESHOLD * 100f);
        return new SmishResult(isPhishing, finalScore, mode);
    }

    private MappedByteBuffer loadModelFile(Context context, String name) throws IOException {
        AssetFileDescriptor fd = context.getAssets().openFd(name);
        FileInputStream fis = new FileInputStream(fd.getFileDescriptor());
        return fis.getChannel().map(FileChannel.MapMode.READ_ONLY, fd.getStartOffset(), fd.getDeclaredLength());
    }

    private void loadVocab(Context context, String name) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(context.getAssets().open(name)));
        String line;
        int index = 0;
        while ((line = r.readLine()) != null) {
            vocabData.put(line.trim(), index++);
        }
        r.close();
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