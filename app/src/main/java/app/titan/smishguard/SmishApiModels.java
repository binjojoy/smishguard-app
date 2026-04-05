package app.titan.smishguard;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public class SmishApiModels {

    // 1. THE REQUEST OBJECT
    public static class SmishRequest {
        String text;
        public SmishRequest(String text) { this.text = text; }
    }

    // 2. THE RESPONSE OBJECT
    public static class SmishResponse {
        @SerializedName("is_phishing")
        public boolean isPhishing;

        @SerializedName("final_risk_score")
        public String finalRiskScore;

        @SerializedName("logic_mode")
        public String logicMode;

        @SerializedName("link_warnings")
        public String linkWarnings;

        // THIS WAS THE MISSING PIECE
        @SerializedName("entities_detected")
        public List<String> entitiesDetected;

        @SerializedName("ai_score")
        public String aiScore;

        @SerializedName("forensic_score")
        public String forensicScore;

        @SerializedName("model_version")
        public String modelVersion;
    }
    public static class ReportRequest {
        String text;
        @SerializedName("user_corrected_label") int userCorrectedLabel;
        @SerializedName("model_score") int modelScore;

        public ReportRequest(String text, int label, int score) {
            this.text = text;
            this.userCorrectedLabel = label;
            this.modelScore = score;
        }
    }

    // 5. THE REPORT RESPONSE
    public static class ReportResponse {
        public String status;
        public String message;
        @SerializedName("recorded_label") int recordedLabel;
    }

    // 3. THE RETROFIT INTERFACE
    public interface SmishService {
        @POST("predict")
        Call<SmishResponse> getPrediction(@Body SmishRequest request);

        @POST("report")
        Call<ReportResponse> submitReport(@Body ReportRequest request);
    }
}