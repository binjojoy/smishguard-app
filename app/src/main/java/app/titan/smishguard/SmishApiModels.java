package app.titan.smishguard;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SmishApiModels {

    // Request Object
    public static class SmishRequest {
        public String text;
        public SmishRequest(String text) { this.text = text; }
    }

    // Response Object
    public static class SmishResponse {
        @SerializedName("is_phishing") public boolean isPhishing;
        @SerializedName("final_risk_score") public String finalRiskScore;
        @SerializedName("ai_score") public String aiScore;
        @SerializedName("logic_mode") public String logicMode;
        @SerializedName("link_warnings") public String linkWarnings;
    }

    // Retrofit Interface
    public interface SmishService {
        @androidx.annotation.NonNull
        @retrofit2.http.POST("predict")
        retrofit2.Call<SmishResponse> getPrediction(@retrofit2.http.Body SmishRequest request);
    }
}