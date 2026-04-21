package com.novelverse.app.presentation.common.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Task 10 — Thin donut arc drawn over a novel cover image showing reading progress.
 */
public class DonutProgressView extends View {
    private Paint trackPaint, progressPaint;
    private float progress = 0f; // 0.0 to 1.0

    public DonutProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DonutProgressView(Context context) {
        super(context);
        init();
    }

    private void init() {
        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(6f);
        trackPaint.setColor(0x22000000);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(6f);
        progressPaint.setColor(Color.parseColor("#6366F1")); // primary
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setProgress(float p) { this.progress = p; invalidate(); }
    public float getProgress() { return progress; }

    @Override
    protected void onDraw(Canvas canvas) {
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float radius = Math.min(cx, cy) - 4f;
        RectF oval = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);
        canvas.drawArc(oval, -90, 360, false, trackPaint);
        canvas.drawArc(oval, -90, 360 * progress, false, progressPaint);
    }
}
