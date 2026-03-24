package com.rideeasy.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

/**
 * Transparent overlay drawn on top of the camera preview.
 * Draws:
 *   - Yellow horizontal virtual line at LINE_POSITION of frame height
 *   - Red bounding boxes around detected persons
 *   - Entry / Exit / InBus count labels
 */
public class OverlayView extends View {

    private final Paint linePaint;
    private final Paint boxPaint;
    private final Paint textPaint;
    private final Paint countPaint;
    private final Paint bgPaint;

    private List<YoloDetector.Box> boxes = new ArrayList<>();
    private float lineY      = 0f;   // pixel position of virtual line on THIS view
    private int   entryCount = 0;
    private int   exitCount  = 0;
    private int   inBus      = 0;

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);

        linePaint = new Paint();
        linePaint.setColor(Color.YELLOW);
        linePaint.setStrokeWidth(6f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAlpha(200);

        boxPaint = new Paint();
        boxPaint.setColor(Color.RED);
        boxPaint.setStrokeWidth(4f);
        boxPaint.setStyle(Paint.Style.STROKE);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(36f);
        textPaint.setAntiAlias(true);

        countPaint = new Paint();
        countPaint.setColor(Color.WHITE);
        countPaint.setTextSize(52f);
        countPaint.setTextAlign(Paint.Align.CENTER);
        countPaint.setAntiAlias(true);
        countPaint.setFakeBoldText(true);

        bgPaint = new Paint();
        bgPaint.setColor(Color.BLACK);
        bgPaint.setAlpha(160);
        bgPaint.setStyle(Paint.Style.FILL);
    }

    public void setData(List<YoloDetector.Box> boxes, int entry, int exit, int inBus,
                        float lineYNorm, int frameW, int frameH) {
        this.boxes      = boxes != null ? boxes : new ArrayList<>();
        this.entryCount = entry;
        this.exitCount  = exit;
        this.inBus      = inBus;
        this.lineY      = lineYNorm * getHeight();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        float linePixel = AppConfig.LINE_POSITION * h;

        // ── Virtual line ───────────────────────────────────────────────────
        canvas.drawLine(0, linePixel, w, linePixel, linePaint);

        // Labels on line
        textPaint.setTextSize(32f);
        textPaint.setColor(Color.YELLOW);
        canvas.drawText("▲ ENTRY (cross upward = EXIT)", 12, linePixel - 10, textPaint);

        // ── Bounding boxes ─────────────────────────────────────────────────
        for (YoloDetector.Box box : boxes) {
            float scaleX = (float) w / AppConfig.MODEL_INPUT;
            float scaleY = (float) h / AppConfig.MODEL_INPUT;
            float left   = box.left()   * scaleX;
            float top    = box.top()    * scaleY;
            float right  = box.right()  * scaleX;
            float bottom = box.bottom() * scaleY;

            boxPaint.setColor(bottom > linePixel ? 0xFFFF4444 : 0xFF44FF88);
            canvas.drawRect(left, top, right, bottom, boxPaint);

            // Confidence label
            textPaint.setTextSize(26f);
            textPaint.setColor(Color.WHITE);
            canvas.drawText(String.format("%.0f%%", box.score * 100), left + 4, top - 6, textPaint);
        }

        // ── Counter overlay (top of screen) ───────────────────────────────
        // Entry count (left)
        canvas.drawRect(0, 0, 200, 110, bgPaint);
        textPaint.setTextSize(22f);
        textPaint.setColor(0xFF10B981); // green
        canvas.drawText("ENTERED", 10, 30, textPaint);
        countPaint.setColor(0xFF10B981);
        canvas.drawText(String.valueOf(entryCount), 100, 96, countPaint);

        // Exit count (right)
        canvas.drawRect(w - 200, 0, w, 110, bgPaint);
        textPaint.setTextSize(22f);
        textPaint.setColor(0xFFEF4444); // red
        textPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("EXITED", w - 10, 30, textPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);
        countPaint.setColor(0xFFEF4444);
        countPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(String.valueOf(exitCount), w - 10, 96, countPaint);
        countPaint.setTextAlign(Paint.Align.CENTER);

        // IN BUS count (centre, larger)
        float midX = w / 2f;
        canvas.drawRoundRect(new RectF(midX - 120, 0, midX + 120, 110), 16, 16, bgPaint);
        textPaint.setTextSize(20f);
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("IN BUS", midX, 28, textPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);
        countPaint.setTextSize(60f);
        countPaint.setColor(0xFFFFFFFF);
        canvas.drawText(String.valueOf(inBus), midX, 100, countPaint);
        countPaint.setTextSize(52f);
    }
}
