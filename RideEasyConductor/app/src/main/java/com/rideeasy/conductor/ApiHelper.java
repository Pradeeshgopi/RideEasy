package com.rideeasy.conductor;

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
            .writeTimeout(15, TimeUnit.SECONDS)
            .build();

    private static final MediaType JSON =
            MediaType.get("application/json; charset=utf-8");

    /** JWT token — set after login, cleared on logout */
    public static String authToken = "";

    // ─────────────────────────────────────────────────────────────────────────
    // POST
    // ─────────────────────────────────────────────────────────────────────────
    public static void post(String url, JSONObject body, ApiCallback callback) {
        RequestBody requestBody = RequestBody.create(body.toString(), JSON);

        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(requestBody);

        if (!authToken.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + authToken);
        }

        client.newCall(builder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "POST failed: " + e.getMessage());
                callback.onError(e.getMessage() != null ? e.getMessage() : "Network error");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                ResponseBody rb = response.body();
                if (rb == null) {
                    callback.onError("Empty response from server");
                    return;
                }
                try {
                    String bodyStr = rb.string();
                    JSONObject json = new JSONObject(bodyStr);
                    callback.onSuccess(json);
                } catch (Exception e) {
                    Log.e(TAG, "Parse error: " + e.getMessage());
                    callback.onError("Response parse error: " + e.getMessage());
                }
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET
    // ─────────────────────────────────────────────────────────────────────────
    public static void get(String url, ApiCallback callback) {
        Request.Builder builder = new Request.Builder()
                .url(url)
                .get();

        if (!authToken.isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + authToken);
        }

        client.newCall(builder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "GET failed: " + e.getMessage());
                callback.onError(e.getMessage() != null ? e.getMessage() : "Network error");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                ResponseBody rb = response.body();
                if (rb == null) {
                    callback.onError("Empty response from server");
                    return;
                }
                try {
                    String bodyStr = rb.string();
                    JSONObject json = new JSONObject(bodyStr);
                    callback.onSuccess(json);
                } catch (Exception e) {
                    Log.e(TAG, "Parse error: " + e.getMessage());
                    callback.onError("Response parse error: " + e.getMessage());
                }
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Callback interface
    // ─────────────────────────────────────────────────────────────────────────
    public interface ApiCallback {
        void onSuccess(JSONObject response);
        void onError(String error);
    }
}