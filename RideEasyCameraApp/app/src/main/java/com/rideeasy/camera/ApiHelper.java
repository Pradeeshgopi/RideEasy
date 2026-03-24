package com.rideeasy.camera;

import android.util.Log;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ApiHelper {

    private static final String TAG = "CameraApiHelper";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    public static String authToken = "";

    public static void post(String url, JSONObject body, ApiCallback callback) {
        RequestBody rb = RequestBody.create(body.toString(), JSON);
        Request.Builder builder = new Request.Builder().url(url).post(rb);
        if (!authToken.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + authToken);
        }

        client.newCall(builder.build()).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Log.e(TAG, "POST failed: " + e.getMessage());
                if (callback != null) callback.onError(e.getMessage());
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    if (callback != null) callback.onError("Empty response");
                    return;
                }
                try {
                    JSONObject json = new JSONObject(responseBody.string());
                    if (callback != null) callback.onSuccess(json);
                } catch (Exception e) {
                    if (callback != null) callback.onError("Parse error");
                }
            }
        });
    }

    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }
}
