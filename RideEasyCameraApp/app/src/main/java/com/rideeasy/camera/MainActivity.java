package com.rideeasy.camera;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import androidx.camera.core.ImageProxy;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {

    private static final String TAG  = "CameraMain";
    private static final int    PERM = 100;

    // ── Views ──────────────────────────────────────────────────────────────
    PreviewView previewView;
    OverlayView overlayView;
    LinearLayout loginScreen, cameraScreen;
    EditText conductorIdInput, busNumberInput;
    TextView countDisplay, statusText;

    // ── State ──────────────────────────────────────────────────────────────
    String conductorId = "";
    String busNumber   = "";

    int entryCount = 0;
    int exitCount  = 0;
    int totalInBus = 0;        // Camera-based real count

    // Previous centroid Ys for line-crossing detection
    // Key = approximation of bounding box centre X (used as rough person ID)
    Map<Float, Float> previousCentroids = new HashMap<>();

    // ── Detection ──────────────────────────────────────────────────────────
    YoloDetector detector;
    ExecutorService cameraExecutor;
    long lastBackendSend = 0;

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cameraExecutor = Executors.newSingleThreadExecutor();

        // Load saved prefs
        SharedPreferences prefs = getSharedPreferences(AppConfig.PREFS_NAME, MODE_PRIVATE);
        conductorId = prefs.getString(AppConfig.KEY_CONDUCTOR, "");
        busNumber   = prefs.getString(AppConfig.KEY_BUS,       "");
        String token = prefs.getString(AppConfig.KEY_TOKEN, "");
        if (!token.isEmpty()) ApiHelper.authToken = token;

        if (!conductorId.isEmpty() && !busNumber.isEmpty()) {
            buildCameraScreen();
            startCamera();
        } else {
            buildLoginScreen();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOGIN SCREEN — minimal: just Conductor ID + Bus Number
    // ─────────────────────────────────────────────────────────────────────────
    private void buildLoginScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0A0E1A);
        root.setGravity(android.view.Gravity.CENTER);
        root.setPadding(dp(32), dp(64), dp(32), dp(32));

        TextView title = new TextView(this);
        title.setText("📷  RideEasy Camera");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(24);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(android.view.Gravity.CENTER);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Passenger counting app\nMount this phone at the bus door");
        sub.setTextColor(0xFF94A3B8);
        sub.setTextSize(13);
        sub.setGravity(android.view.Gravity.CENTER);
        sub.setPadding(0, dp(8), 0, dp(40));
        root.addView(sub);

        conductorIdInput = makeInput("Conductor ID (e.g. CON01)");
        root.addView(conductorIdInput);
        spacer(root, 12);

        busNumberInput = makeInput("Bus Number (e.g. 99A)");
        root.addView(busNumberInput);
        spacer(root, 24);

        com.google.android.material.button.MaterialButton startBtn =
                new com.google.android.material.button.MaterialButton(this);
        startBtn.setText("Start Counting");
        startBtn.setTextColor(0xFFFFFFFF);
        startBtn.setTextSize(16);
        LinearLayout.LayoutParams sbp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56));
        startBtn.setLayoutParams(sbp);
        startBtn.setBackgroundColor(0xFF4F8EF7);
        startBtn.setCornerRadius(dp(14));
        startBtn.setOnClickListener(v -> {
            String cId = conductorIdInput.getText().toString().trim();
            String bNum = busNumberInput.getText().toString().trim();
            if (cId.isEmpty() || bNum.isEmpty()) {
                Toast.makeText(this, "Please enter both fields", Toast.LENGTH_SHORT).show();
                return;
            }
            conductorId = cId;
            busNumber   = bNum;
            getSharedPreferences(AppConfig.PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putString(AppConfig.KEY_CONDUCTOR, conductorId)
                    .putString(AppConfig.KEY_BUS, busNumber)
                    .apply();
            buildCameraScreen();
            startCamera();
        });
        root.addView(startBtn);

        setContentView(root);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CAMERA SCREEN
    // ─────────────────────────────────────────────────────────────────────────
    private void buildCameraScreen() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF000000);

        previewView = new PreviewView(this);
        root.addView(previewView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        overlayView = new OverlayView(this, null);
        root.addView(overlayView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        // Status bar at bottom
        LinearLayout statusBar = new LinearLayout(this);
        statusBar.setBackgroundColor(0xCC000000);
        statusBar.setOrientation(LinearLayout.VERTICAL);
        statusBar.setPadding(dp(16), dp(12), dp(16), dp(16));
        FrameLayout.LayoutParams sbp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        sbp.gravity = android.view.Gravity.BOTTOM;
        statusBar.setLayoutParams(sbp);

        statusText = new TextView(this);
        statusText.setText("🔄 Initialising camera...");
        statusText.setTextColor(0xFF94A3B8);
        statusText.setTextSize(12);
        statusBar.addView(statusText);

        TextView busInfo = new TextView(this);
        busInfo.setText("🚌 Bus " + busNumber + " · " + conductorId + " · Camera counting active");
        busInfo.setTextColor(0xFF4F8EF7);
        busInfo.setTextSize(12);
        busInfo.setTypeface(null, android.graphics.Typeface.BOLD);
        statusBar.addView(busInfo);

        // Reset button
        com.google.android.material.button.MaterialButton resetBtn =
                new com.google.android.material.button.MaterialButton(this);
        resetBtn.setText("🔄 Reset Count");
        resetBtn.setTextColor(0xFFFFFFFF);
        resetBtn.setTextSize(13);
        LinearLayout.LayoutParams rbp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rbp.setMargins(0, dp(8), 0, 0);
        resetBtn.setLayoutParams(rbp);
        resetBtn.setBackgroundColor(0xFF1E293B);
        resetBtn.setOnClickListener(v -> resetCount());
        statusBar.addView(resetBtn);

        root.addView(statusBar);
        setContentView(root);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CAMERAX SETUP
    // ─────────────────────────────────────────────────────────────────────────
    private void startCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, PERM);
            return;
        }

        detector = new YoloDetector(this);

        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(640, 480))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                analysis.setAnalyzer(cameraExecutor, this::analyseFrame);

                provider.unbindAll();
                provider.bindToLifecycle(this,
                        CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis);

                if (statusText != null) {
                    runOnUiThread(() -> statusText.setText("✅ Camera active — detecting persons"));
                }
            } catch (Exception e) {
                Log.e(TAG, "Camera bind failed: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FRAME ANALYSIS + LINE CROSSING DETECTION
    // ─────────────────────────────────────────────────────────────────────────
    private void analyseFrame(ImageProxy image) {
        Bitmap bitmap = imageProxyToBitmap(image);
        image.close();
        if (bitmap == null || detector == null) return;

        List<YoloDetector.Box> detections = detector.detect(bitmap);

        // Line position in model-pixel space
        float lineY = AppConfig.LINE_POSITION * AppConfig.MODEL_INPUT; // 0.5 × 640 = 320

        // Track crossings
        Map<Float, Float> newCentroids = new HashMap<>();
        for (YoloDetector.Box box : detections) {
            float cx = box.cx;
            float cy = box.cy;

            // Find nearest previous centroid within ±60px (rough person tracking)
            Float matchedKey = null;
            for (Map.Entry<Float, Float> entry : previousCentroids.entrySet()) {
                if (Math.abs(entry.getKey() - cx) < 60) {
                    matchedKey = entry.getKey();
                    break;
                }
            }

            if (matchedKey != null) {
                float prevY = previousCentroids.get(matchedKey);

                // Person moved from ABOVE line to BELOW → ENTERED
                if (prevY < lineY && cy >= lineY) {
                    entryCount++;
                    totalInBus++;
                    sendCountToBackend();
                }
                // Person moved from BELOW line to ABOVE → EXITED
                else if (prevY >= lineY && cy < lineY) {
                    exitCount++;
                    totalInBus = Math.max(0, totalInBus - 1);
                    sendCountToBackend();
                }
                previousCentroids.remove(matchedKey);
            }
            newCentroids.put(cx, cy);
        }
        previousCentroids.clear();
        previousCentroids.putAll(newCentroids);

        // Update overlay
        final List<YoloDetector.Box> finalDetections = detections;
        final int e = entryCount, x = exitCount, b = totalInBus;
        runOnUiThread(() -> {
            if (overlayView != null) {
                overlayView.setData(finalDetections, e, x, b,
                        AppConfig.LINE_POSITION, AppConfig.MODEL_INPUT, AppConfig.MODEL_INPUT);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SEND COUNT TO BACKEND
    // ─────────────────────────────────────────────────────────────────────────
    private void sendCountToBackend() {
        // Throttle: max 1 send per 2 seconds
        long now = System.currentTimeMillis();
        if (now - lastBackendSend < 2000) return;
        lastBackendSend = now;

        try {
            JSONObject body = new JSONObject();
            body.put("conductorId", conductorId);
            body.put("busNumber",   busNumber);
            body.put("inBusCount",  totalInBus);
            body.put("entryCount",  entryCount);
            body.put("exitCount",   exitCount);

            ApiHelper.post(AppConfig.CAMERA_URL, body, new ApiHelper.ApiCallback() {
                @Override public void onSuccess(JSONObject r) {
                    Log.d(TAG, "Count sent: inBus=" + totalInBus);
                }
                @Override public void onError(String err) {
                    Log.w(TAG, "Count send failed: " + err);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Send error: " + e.getMessage());
        }
    }

    private void resetCount() {
        entryCount = 0;
        exitCount  = 0;
        totalInBus = 0;
        previousCentroids.clear();
        if (overlayView != null) {
            overlayView.setData(null, 0, 0, 0,
                    AppConfig.LINE_POSITION, AppConfig.MODEL_INPUT, AppConfig.MODEL_INPUT);
        }
        Toast.makeText(this, "Count reset to 0", Toast.LENGTH_SHORT).show();
        sendCountToBackend();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // IMAGE CONVERSION: ImageProxy → Bitmap
    // ─────────────────────────────────────────────────────────────────────────
    private Bitmap imageProxyToBitmap(ImageProxy image) {
        try {
            ImageProxy.PlaneProxy[] planes = image.getPlanes();
            ByteBuffer yBuf  = planes[0].getBuffer();
            ByteBuffer uBuf  = planes[1].getBuffer();
            ByteBuffer vBuf  = planes[2].getBuffer();

            byte[] yBytes = new byte[yBuf.remaining()];
            byte[] uBytes = new byte[uBuf.remaining()];
            byte[] vBytes = new byte[vBuf.remaining()];
            yBuf.get(yBytes);
            uBuf.get(uBytes);
            vBuf.get(vBytes);

            byte[] nv21 = new byte[yBytes.length + uBytes.length + vBytes.length];
            System.arraycopy(yBytes, 0, nv21, 0, yBytes.length);
            // Interleave V and U for NV21
            for (int i = 0; i < vBytes.length && i < uBytes.length; i++) {
                nv21[yBytes.length + i * 2]     = vBytes[i];
                if (yBytes.length + i * 2 + 1 < nv21.length)
                    nv21[yBytes.length + i * 2 + 1] = uBytes[i];
            }

            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21,
                    image.getWidth(), image.getHeight(), null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 85, out);
            byte[] jpegBytes = out.toByteArray();
            return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);
        } catch (Exception e) {
            Log.e(TAG, "Bitmap conversion failed: " + e.getMessage());
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERM && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            buildCameraScreen();
            startCamera();
        } else {
            Toast.makeText(this, "Camera permission required!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        if (detector != null) detector.close();
    }

    // ── UI helpers ─────────────────────────────────────────────────────────
    private EditText makeInput(String hint) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setHintTextColor(0xFF475569);
        et.setTextColor(0xFFFFFFFF);
        et.setBackgroundColor(0xFF0F1420);
        et.setTextSize(15);
        et.setPadding(dp(16), dp(14), dp(16), dp(14));
        et.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        return et;
    }

    private void spacer(LinearLayout parent, int dpSize) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(dpSize)));
        parent.addView(v);
    }

    private int dp(int val) {
        return (int)(val * getResources().getDisplayMetrics().density);
    }
}
