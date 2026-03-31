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
    }

    // 3. THE RETROFIT INTERFACE
    public interface SmishService {
        @POST("predict")
        Call<SmishResponse> getPrediction(@Body SmishRequest request);
    }
}