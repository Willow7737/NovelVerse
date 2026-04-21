package com.novelverse.app.presentation.common.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

/**
 * iOS Control-Center–style thick pill slider.
 * The track is a tall pill; the fill grows from the left; a white circle thumb
 * sits at the leading edge of the fill — all inside the pill, no overhang.
 */
public class ReaderSliderView extends View {

    public interface OnProgressChangedListener {
        void onProgressChanged(int progress, boolean fromUser);
    }

    private static final float TRACK_HEIGHT_DP  = 40f;
    private static final float THUMB_DIAM_DP    = 30f;
    private static final float THUMB_SHADOW_R   = 8f;

    private final Paint trackPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF trackRect   = new RectF();
    private final RectF fillRect    = new RectF();

    private int   maxValue      = 100;
    private int   currentValue  = 50;
    private int   fillColor     = 0xFF0085FF;
    private int   trackColor    = 0x18000000;   // translucent dark tint
    private float trackH, thumbR, halfThumb;
    private boolean hapticLeft  = false;
    private boolean hapticRight = false;

    private OnProgressChangedListener listener;

    public ReaderSliderView(Context c) { super(c); init(c); }
    public ReaderSliderView(Context c, AttributeSet a) { super(c, a); init(c); }

    private void init(Context c) {
        float d = c.getResources().getDisplayMetrics().density;
        trackH   = TRACK_HEIGHT_DP  * d;
        thumbR   = (THUMB_DIAM_DP / 2f) * d;
        halfThumb = thumbR;

        trackPaint.setColor(trackColor);

        fillPaint.setColor(fillColor);

        thumbPaint.setColor(0xFFFFFFFF);
        thumbPaint.setShadowLayer(THUMB_SHADOW_R * d / 3f, 0, 2f * d / 3f, 0x40000000);

        setLayerType(LAYER_TYPE_SOFTWARE, null); // required for setShadowLayer
    }

    // ── Public API ────────────────────────────────────────────────────────

    public void setMax(int max)   { this.maxValue = max;  invalidate(); }
    public void setValue(int val) { currentValue = clamp(val); invalidate(); }
    public int  getValue()        { return currentValue; }

    public void setFillColor(int color) {
        fillColor = color;
        fillPaint.setColor(color);
        invalidate();
    }

    public void setTrackColor(int color) {
        trackColor = color;
        trackPaint.setColor(color);
        invalidate();
    }

    public void setOnProgressChangedListener(OnProgressChangedListener l) {
        this.listener = l;
    }

    // ── Measure ───────────────────────────────────────────────────────────

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int h = (int) trackH;
        // Ensure shadow is not clipped: add padding equal to thumbR
        setPadding((int) halfThumb, 0, (int) halfThumb, 0);
        setMeasuredDimension(
            resolveSize(getSuggestedMinimumWidth(), wSpec),
            resolveSize(h, hSpec));
    }

    // ── Draw ──────────────────────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        float left  = halfThumb;
        float right = w - halfThumb;
        float top   = (h - trackH) / 2f;
        float bot   = top + trackH;
        float r     = trackH / 2f;

        // 1. Draw track background (full pill)
        trackRect.set(left, top, right, bot);
        canvas.drawRoundRect(trackRect, r, r, trackPaint);

        // 2. Draw fill (pill from left to thumb x)
        float progress = (maxValue > 0) ? (float) currentValue / maxValue : 0f;
        float thumbCx  = left + progress * (right - left);
        float fillRight = Math.max(thumbCx, left + r * 2); // at least a full circle
        fillRect.set(left, top, fillRight, bot);

        // Gradient fill: slightly lighter on right edge
        LinearGradient grad = new LinearGradient(
            left, 0, fillRight, 0,
            fillColor, lighten(fillColor, 0.15f),
            Shader.TileMode.CLAMP
        );
        fillPaint.setShader(grad);
        canvas.drawRoundRect(fillRect, r, r, fillPaint);

        // 3. Draw thumb circle
        canvas.drawCircle(thumbCx, h / 2f, thumbR, thumbPaint);
    }

    // ── Touch ─────────────────────────────────────────────────────────────

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        float left  = halfThumb;
        float right = getWidth() - halfThumb;
        float span  = right - left;
        if (span <= 0) return super.onTouchEvent(ev);

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                hapticLeft  = false;
                hapticRight = false;
                // fall through
            case MotionEvent.ACTION_MOVE: {
                float x = ev.getX();
                float progress = (x - left) / span;
                int newVal = clamp(Math.round(progress * maxValue));
                if (newVal != currentValue) {
                    currentValue = newVal;
                    invalidate();
                    if (listener != null) listener.onProgressChanged(currentValue, true);
                    fireEdgeHaptic();
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                return true;
        }
        return super.onTouchEvent(ev);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private int clamp(int v) { return Math.max(0, Math.min(maxValue, v)); }

    private void fireEdgeHaptic() {
        if (currentValue == 0 && !hapticLeft) {
            hapticLeft = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY_RELEASE);
            } else {
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            }
        } else if (currentValue == maxValue && !hapticRight) {
            hapticRight = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY_RELEASE);
            } else {
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            }
        } else if (currentValue > 0 && currentValue < maxValue) {
            hapticLeft  = false;
            hapticRight = false;
        }
    }

    /** Lighten a color by mixing toward white by 'amount' (0..1). */
    private static int lighten(int color, float amount) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >>  8) & 0xFF;
        int b = (color)       & 0xFF;
        r = (int) (r + (255 - r) * amount);
        g = (int) (g + (255 - g) * amount);
        b = (int) (b + (255 - b) * amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
