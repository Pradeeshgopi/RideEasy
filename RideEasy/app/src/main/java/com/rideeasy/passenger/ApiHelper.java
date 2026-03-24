package com.rideeasy.passenger;

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

    private static final String TAG = "ApiHelper";

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    private static final MediaType JSON =
            MediaType.get("application/json; charset=utf-8");

    // ─────────────────────────────────────────────────────────────────────────
    public static void get(String url, ApiCallback callback) {
        Request request = new Request.Builder().url(url).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "GET failed: " + e.getMessage());
                callback.onError(e.getMessage() != null ? e.getMessage() : "Network error");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                ResponseBody rb = response.body();
                if (rb == null) { callback.onError("Empty response"); return; }
                try {
                    callback.onSuccess(new JSONObject(rb.string()));
                } catch (Exception e) {
                    callback.onError("Parse error: " + e.getMessage());
                }
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    public static void post(String url, JSONObject body, ApiCallback callback) {
        RequestBody requestBody = RequestBody.create(body.toString(), JSON);
        Request request = new Request.Builder().url(url).post(requestBody).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "POST failed: " + e.getMessage());
                callback.onError(e.getMessage() != null ? e.getMessage() : "Network error");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                ResponseBody rb = response.body();
                if (rb == null) { callback.onError("Empty response"); return; }
                try {
                    callback.onSuccess(new JSONObject(rb.string()));
                } catch (Exception e) {
                    callback.onError("Parse error: " + e.getMessage());
                }
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }
}