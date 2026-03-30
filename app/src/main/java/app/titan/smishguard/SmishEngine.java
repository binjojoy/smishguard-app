package app.titan.smishguard;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

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

    private boolean modelLoaded = false;
    private boolean vocabLoaded = false;

    private final int MAX_LEN = 120;
    private final float SCAM_THRESHOLD = 0.05744f;

    private final String[] SUSPICIOUS_TLDS = {
            ".xyz", ".top", ".club", ".ru", ".cn", ".live", ".click", ".link", ".cam", ".bid"
    };

    private final String[] KNOWN_BRANDS = {
            "sbi", "hdfc", "icici", "axis", "netflix", "disney", "fedex", "ups", "amazon", "yonosbi"
    };

    public SmishEngine(Context context) {
        try {
            Log.d("SmishGuard", "⏳ Initializing SmishEngine...");

            // Load model
            interpreter = new Interpreter(loadModelFile(context, "engine1.tflite"));
            modelLoaded = true;
            Log.d("SmishGuard", "✅ Model loaded");

            // Load vocab
            loadVocab(context, "vocab.txt");
            vocabLoaded = true;

            Log.d("SmishGuard", "📚 Vocab size: " + vocabData.size());

            if (isReady()) {
                Log.d("SmishGuard", "🚀 SmishEngine FULLY READY");
            } else {
                Log.e("SmishGuard", "⚠️ Engine not ready after init");
            }

        } catch (Exception e) {
            Log.e("SmishGuard", "❌ INIT ERROR", e);
        }
    }

    public boolean isReady() {
        return modelLoaded && vocabLoaded && interpreter != null && !vocabData.isEmpty();
    }

    // ---------------- PREPROCESS ----------------

    private String preprocess(String text) {
        String processed = text.toLowerCase().trim();

        processed = processed.replace("@", "a").replace("3", "e").replace("0", "o")
                .replace("1", "l").replace("5", "s").replace("!", "i");

        processed = processed.replaceAll("https?://\\S+|www\\.\\S+", " url_token ");
        processed = processed.replaceAll("\\b[\\d\\-\\.\\(\\)\\s]{9,}\\b", " phone_token ");
        processed = processed.replaceAll("[\\$\\u20b9£€]\\d+", " money_token ");

        processed = processed.replace(".", " ").replace("-", " ");

        return processed.replaceAll("\\s+", " ").trim();
    }

    // ---------------- AI INFERENCE ----------------

    public float getAiScore(String text) {

        if (!isReady()) {
            Log.e("SmishGuard", "⚠️ Engine not ready during inference");
            return 0f;
        }

        String clean = preprocess(text);
        String[] words = clean.split("\\s+");
        ArrayList<Integer> tokens = new ArrayList<>();

        for (int i = 0; i < words.length; i++) {
            tokens.add(vocabData.getOrDefault(words[i], 1));

            if (i < words.length - 1) {
                String bigram = words[i] + " " + words[i + 1];
                if (vocabData.containsKey(bigram)) {
                    tokens.add(vocabData.get(bigram));
                }
            }
        }

        int[][] inputIds = new int[1][MAX_LEN];

        for (int i = 0; i < MAX_LEN; i++) {
            inputIds[0][i] = (i < tokens.size()) ? tokens.get(i) : 0;
        }

        float[][] output = new float[1][1];

        try {
            interpreter.run(inputIds, output);
        } catch (Exception e) {
            Log.e("SmishGuard", "❌ Inference error", e);
            return 0f;
        }

        return output[0][0];
    }

    // ---------------- FORENSIC ----------------

    private double calculateEntropy(String domain) {
        if (domain == null || domain.isEmpty()) return 0;

        Map<Character, Integer> counts = new HashMap<>();

        for (char c : domain.toCharArray()) {
            counts.put(c, counts.getOrDefault(c, 0) + 1);
        }

        double entropy = 0;

        for (int count : counts.values()) {
            double p = (double) count / domain.length();
            entropy -= p * (Math.log(p) / Math.log(2));
        }

        return entropy;
    }

    public SmishResult analyze(String text) {

        if (!isReady()) {
            return new SmishResult(false, 0f, "Engine Not Ready");
        }

        float rawAiScore = getAiScore(text);
        float aiScorePercent = rawAiScore * 100f;

        float forensicScore = 0f;
        String mode = "Hybrid Consensus";
        boolean isCritical = false;

        Pattern urlPattern = Pattern.compile("https?://([^/\\s]+)");
        Matcher matcher = urlPattern.matcher(text);

        if (matcher.find()) {
            String domain = matcher.group(1).toLowerCase();

            if (calculateEntropy(domain) > 3.8) forensicScore += 30f;

            for (String tld : SUSPICIOUS_TLDS) {
                if (domain.endsWith(tld)) {
                    forensicScore += 40f;
                    break;
                }
            }

            for (String brand : KNOWN_BRANDS) {
                if (text.toLowerCase().contains(brand) && !domain.contains(brand)) {
                    forensicScore += 80f;
                    isCritical = true;
                    break;
                }
            }
        }

        float finalScore;

        if (isCritical && rawAiScore > 0.01f) {
            finalScore = Math.max(95f, aiScorePercent);
            mode = "Forensic Override (Critical)";
        } else {
            finalScore = (aiScorePercent * 0.6f) + (forensicScore * 0.4f);
        }

        boolean isPhishing = finalScore >= (SCAM_THRESHOLD * 100f);

        return new SmishResult(isPhishing, finalScore, mode);
    }

    // ---------------- FILE LOADING ----------------

    private MappedByteBuffer loadModelFile(Context context, String name) throws IOException {

        Log.d("SmishGuard", "📦 Loading model file: " + name);

        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(name);

        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();

        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();

        Log.d("SmishGuard", "📏 Model size: " + declaredLength);

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void loadVocab(Context context, String name) throws IOException {

        Log.d("SmishGuard", "📖 Loading vocab: " + name);

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open(name))
        );

        String line;
        int index = 0;

        while ((line = reader.readLine()) != null) {
            vocabData.put(line.trim(), index++);
        }

        reader.close();
    }

    // ---------------- RESULT CLASS ----------------

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