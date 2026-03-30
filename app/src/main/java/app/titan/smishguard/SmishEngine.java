package app.titan.smishguard;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SmishEngine {

    private final SmishApiModels.SmishService service;

    public SmishEngine() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://chrisbinsibi-smishguard-api.hf.space/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        service = retrofit.create(SmishApiModels.SmishService.class);
    }

    // Callback interface for the Activity to hear back from the API
    public interface ApiCallback {
        void onSuccess(SmishApiModels.SmishResponse result);
        void onError(String error);
    }

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
}