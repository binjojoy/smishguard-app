package app.titan.smishguard;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * SmishEngine handles all network communication with the Hugging Face API.
 * It supports both message prediction (checkMessage) and user feedback (submitReport).
 */
public class SmishEngine {

    private final SmishApiModels.SmishService service;

    public SmishEngine() {
        // Initialize Retrofit with the Hugging Face Space URL
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://chrisbinsibi-smishguard-api.hf.space/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        service = retrofit.create(SmishApiModels.SmishService.class);
    }

    // --- CALLBACK INTERFACES ---

    /**
     * Callback for message analysis/prediction
     */
    public interface ApiCallback {
        void onSuccess(SmishApiModels.SmishResponse result);
        void onError(String error);
    }

    /**
     * Callback for user feedback reports
     */
    public interface ReportCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    // --- API METHODS ---

    /**
     * Sends message text to the /predict endpoint for AI and Forensic analysis.
     */
    public void checkMessage(String text, ApiCallback callback) {
        SmishApiModels.SmishRequest request = new SmishApiModels.SmishRequest(text);

        service.getPrediction(request).enqueue(new Callback<SmishApiModels.SmishResponse>() {
            @Override
            public void onResponse(Call<SmishApiModels.SmishResponse> call, Response<SmishApiModels.SmishResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Server Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<SmishApiModels.SmishResponse> call, Throwable t) {
                callback.onError("Network Failed: " + t.getMessage());
            }
        });
    }

    /**
     * Sends user-corrected data to the /report endpoint to help train future models.
     * * @param text The original SMS message content.
     * @param label The corrected label (0 for Safe, 1 for Scam).
     * @param score The risk score the model originally gave.
     */
    public void submitReport(String text, int label, int score, ReportCallback callback) {
        SmishApiModels.ReportRequest request = new SmishApiModels.ReportRequest(text, label, score);

        service.submitReport(request).enqueue(new Callback<SmishApiModels.ReportResponse>() {
            @Override
            public void onResponse(Call<SmishApiModels.ReportResponse> call, Response<SmishApiModels.ReportResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Pass the "Feedback safely queued" message back to the UI
                    callback.onSuccess(response.body().message);
                } else {
                    callback.onError("Report failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<SmishApiModels.ReportResponse> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
}