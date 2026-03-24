package com.rideeasy.camera;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Wraps the YOLOv8n_float32.tflite model.
 *
 * Model specs:
 *   Input:  [1, 640, 640, 3] float32  (RGB, normalised 0.0–1.0)
 *   Output: [1, 84, 8400]  float32
 *            rows 0–3 = cx, cy, w, h  (in model pixel space)
 *            row  4   = person score  (class 0)
 *            rows 5–83 = other class scores (ignored)
 */
public class YoloDetector {

    private static final String TAG   = "YoloDetector";
    private static final int    INPUT = AppConfig.MODEL_INPUT;   // 640
    private static final float  CONF  = AppConfig.CONF_THRESHOLD;

    private Interpreter interpreter;
    private final ByteBuffer inputBuffer;

    public static class Box {
        public float cx, cy, w, h, score;
        // Pixel coords relative to INPUT_SIZE
        public float top()    { return cy - h / 2f; }
        public float bottom() { return cy + h / 2f; }
        public float left()   { return cx - w / 2f; }
        public float right()  { return cx + w / 2f; }
    }

    public YoloDetector(Context context) {
        // Input buffer: 1 × 640 × 640 × 3 floats = 4 bytes each
        inputBuffer = ByteBuffer.allocateDirect(1 * INPUT * INPUT * 3 * 4);
        inputBuffer.order(ByteOrder.nativeOrder());

        try {
            android.content.res.AssetFileDescriptor afd =
                    context.getAssets().openFd("yolov8n_float32.tflite");
            FileInputStream fis = new FileInputStream(afd.getFileDescriptor());
            FileChannel fc      = fis.getChannel();
            java.nio.MappedByteBuffer modelBuffer = fc.map(
                    FileChannel.MapMode.READ_ONLY,
                    afd.getStartOffset(),
                    afd.getDeclaredLength());
            interpreter = new Interpreter(modelBuffer);
            Log.d(TAG, "YOLOv8n model loaded successfully");
        } catch (IOException e) {
            Log.e(TAG, "Failed to load model: " + e.getMessage());
        }
    }

    /**
     * Run inference on a scaled Bitmap (should be 640×640).
     * Returns list of person detections above CONF_THRESHOLD, after NMS.
     */
    public List<Box> detect(Bitmap bitmap) {
        if (interpreter == null) return new ArrayList<>();

        // Fill input buffer — RGB, normalised
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, INPUT, INPUT, false);
        inputBuffer.rewind();
        int[] pixels = new int[INPUT * INPUT];
        scaled.getPixels(pixels, 0, INPUT, 0, 0, INPUT, INPUT);

        for (int pixel : pixels) {
            inputBuffer.putFloat(((pixel >> 16) & 0xFF) / 255.0f);  // R
            inputBuffer.putFloat(((pixel >>  8) & 0xFF) / 255.0f);  // G
            inputBuffer.putFloat(( pixel        & 0xFF) / 255.0f);  // B
        }

        // Output: [1][84][8400]
        float[][][] output = new float[1][84][8400];
        interpreter.run(inputBuffer, output);

        // Parse detections: only class 0 (person)
        List<Box> detections = new ArrayList<>();
        for (int i = 0; i < 8400; i++) {
            float personScore = output[0][4][i];
            if (personScore < CONF) continue;

            Box box   = new Box();
            box.cx    = output[0][0][i];
            box.cy    = output[0][1][i];
            box.w     = output[0][2][i];
            box.h     = output[0][3][i];
            box.score = personScore;
            detections.add(box);
        }

        return nms(detections, 0.45f);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Non-Maximum Suppression
    // ─────────────────────────────────────────────────────────────────────────
    private List<Box> nms(List<Box> boxes, float iouThreshold) {
        // Sort by confidence descending
        boxes.sort((a, b) -> Float.compare(b.score, a.score));
        List<Box> result  = new ArrayList<>();
        boolean[] removed = new boolean[boxes.size()];

        for (int i = 0; i < boxes.size(); i++) {
            if (removed[i]) continue;
            result.add(boxes.get(i));
            for (int j = i + 1; j < boxes.size(); j++) {
                if (!removed[j] && iou(boxes.get(i), boxes.get(j)) > iouThreshold) {
                    removed[j] = true;
                }
            }
        }
        return result;
    }

    private float iou(Box a, Box b) {
        float interLeft   = Math.max(a.left(),   b.left());
        float interTop    = Math.max(a.top(),     b.top());
        float interRight  = Math.min(a.right(),   b.right());
        float interBottom = Math.min(a.bottom(), b.bottom());

        float interArea = Math.max(0, interRight - interLeft)
                * Math.max(0, interBottom - interTop);
        float unionArea = a.w * a.h + b.w * b.h - interArea;
        return unionArea > 0 ? interArea / unionArea : 0;
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
    }
}
