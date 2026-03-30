package app.titan.smishguard;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import org.tensorflow.lite.Interpreter;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.android.gms.tflite.java.TfLite; // This is the main GMS TFLite class
import com.google.android.gms.tasks.Task; // Required for the "Task" based loading
import org.tensorflow.lite.Interpreter;

public class SmishEngine {
    private Interpreter interpreter;
    private Map<String, Integer> vocabData = new HashMap<>();
    private final int MAX_LEN = 200;

    // Forensic Constants (Ported from your Python script)
    private final String[] SUSPICIOUS_TLDS = {".xyz", ".top", ".club", ".ru", ".cn", ".live", ".click", ".link", ".cam"};
    private final String[] KNOWN_BRANDS = {"sbi", "hdfc", "icici", "axis", "netflix", "disney", "fedex", "ups", "amazon"};

    public SmishEngine(Context context) throws IOException {
        // This tells the phone: "Hey, make sure you have the latest AI math ready"
        TfLite.initialize(context).addOnSuccessListener(aVoid -> {
            try {
                // ONLY load the model once the phone says it's ready
                Interpreter.Options options = new Interpreter.Options();
                interpreter = new Interpreter(loadModelFile(context, "engine.tflite"), options);
                loadVocab(context, "vocab.txt");
                android.util.Log.d("SmishGuard", "✅ AI is now online!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // --- PIPELINE A: PREPROCESSING & AI ---
    private String preprocess(String text) {
        text = text.toLowerCase().trim();
        // Homoglyph cleaning (@ -> a, etc.)
        text = text.replace("@", "a").replace("3", "e").replace("0", "o").replace("1", "l").replace("5", "s");
        // Token Replacement
        text = text.replaceAll("https?://\\S+|www\\.\\S+", " url_token ");
        text = text.replaceAll("\\b[\\d\\-\\.\\(\\)\\s]{9,}\\b", " phone_token ");
        text = text.replaceAll("[\\$\\£\\€\\u20b9][\\s\\d,\\.]+", " money_token ");
        return text.replaceAll("\\s+", " ").trim();
    }

    public float getAiScore(String text) {
        int[][] input = new int[1][MAX_LEN];
        String clean = preprocess(text);
        String[] words = clean.split("\\s+");
        for (int i = 0; i < MAX_LEN; i++) {
            if (i < words.length) {
                Integer id = vocabData.get(words[i]);
                input[0][i] = (id != null) ? id : 1;
            } else { input[0][i] = 0; }
        }
        float[][] output = new float[1][1];
        interpreter.run(input, output);
        return output[0][0] * 100f; // Return 0-100 scale
    }

    // --- PIPELINE B: FORENSICS ---
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

    // --- THE FUSION ENGINE ---
    public SmishResult analyze(String text) {
        float aiScore = getAiScore(text);
        float forensicScore = 0f;
        String mode = "AI Only";
        boolean isCritical = false;

        // URL Extraction
        Pattern urlPattern = Pattern.compile("https?://(\\S+)");
        Matcher matcher = urlPattern.matcher(text);

        if (matcher.find()) {
            String domain = matcher.group(1).toLowerCase();

            // 1. Entropy Check
            if (calculateEntropy(domain) > 3.9) {
                forensicScore += 35f;
            }

            // 2. TLD Check
            for (String tld : SUSPICIOUS_TLDS) {
                if (domain.contains(tld)) {
                    forensicScore += 45f;
                    break;
                }
            }

            // 3. Brand Mismatch (The SBI Fix)
            for (String brand : KNOWN_BRANDS) {
                if (text.toLowerCase().contains(brand) && !domain.contains(brand)) {
                    forensicScore += 75f;
                    isCritical = true;
                    break;
                }
            }
        }

        // FUSION LOGIC (Ported from your Python logic)
        float finalScore;
        if (isCritical) {
            finalScore = 100f;
            mode = "Forensic Override (Critical)";
        } else if (forensicScore >= 50) {
            finalScore = (forensicScore * 0.65f) + (aiScore * 0.35f);
            mode = "Forensic Dominant";
        } else {
            finalScore = (aiScore * 0.5f) + (forensicScore * 0.5f);
            mode = "Hybrid Consensus";
        }

        return new SmishResult(finalScore >= 45.0, finalScore, mode);
    }

    // Standard TFLite Loaders
    private MappedByteBuffer loadModelFile(Context context, String name) throws IOException {
        AssetFileDescriptor fd = context.getAssets().openFd(name);
        return new FileInputStream(fd.getFileDescriptor()).getChannel().map(FileChannel.MapMode.READ_ONLY, fd.getStartOffset(), fd.getDeclaredLength());
    }

    private void loadVocab(Context context, String name) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(context.getAssets().open(name)));
        String l; int i = 0;
        while ((l = r.readLine()) != null) vocabData.put(l.trim(), i++);
        r.close();
    }

    // Simple Result Wrapper
    public static class SmishResult {
        public boolean isPhishing;
        public float score;
        public String mode;
        public SmishResult(boolean p, float s, String m) {
            this.isPhishing = p; this.score = s; this.mode = m;
        }
    }
}