package com.novelverse.app.presentation.common.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.novelverse.app.R;

/**
 * Facebook-style skeleton shimmer view.
 * Draws an animated sweep gradient over a rounded-rect background.
 * Auto-starts on attach, auto-stops on detach.
 *
 * By default the corner radius is h/2 (full pill). Use the
 * {@code app:shimmerCornerRadius} XML attribute to set a fixed dp value
 * for non-pill shapes (e.g. cover image placeholders).
 */
public class ShimmerView extends View {

    private final Paint   paint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF   rect    = new RectF();
    private ValueAnimator animator;

    private float animValue         = 0f;
    private int   baseColor         = 0xFFE8EDF2;   // cool gray, matches app surface
    private int   shineColor        = 0xFFF6F8FA;   // near-white shine
    private float fixedCornerRadius = -1f;           // -1 = pill (h/2)

    public ShimmerView(Context c)                        { super(c); setup(c, null); }
    public ShimmerView(Context c, AttributeSet a)        { super(c, a); setup(c, a); }
    public ShimmerView(Context c, AttributeSet a, int s) { super(c, a, s); setup(c, a); }

    private void setup(Context c, AttributeSet attrs) {
        setLayerType(LAYER_TYPE_HARDWARE, null);
        if (attrs != null) {
            TypedArray ta = c.obtainStyledAttributes(attrs, R.styleable.ShimmerView);
            fixedCornerRadius = ta.getDimension(R.styleable.ShimmerView_shimmerCornerRadius, -1f);
            ta.recycle();
        }
    }

    /** Call to tint the shimmer for dark/sepia reader themes. */
    public void setShimmerColors(int base, int shine) {
        this.baseColor  = base;
        this.shineColor = shine;
        invalidate();
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startShimmer();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopShimmer();
    }

    public void startShimmer() {
        if (animator != null && animator.isRunning()) return;
        animator = ValueAnimator.ofFloat(-0.5f, 1.5f);
        animator.setDuration(1300);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            animValue = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    public void stopShimmer() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }

    // ── Draw ──────────────────────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;

        float shineW = w * 0.55f;
        float cx     = animValue * (w + shineW) - shineW * 0.5f;

        paint.setShader(new LinearGradient(
            cx,          0,
            cx + shineW, 0,
            new int[]  { baseColor, baseColor, shineColor, baseColor, baseColor },
            new float[] { 0f, 0.3f, 0.5f, 0.7f, 1f },
            Shader.TileMode.CLAMP
        ));

        float radius = fixedCornerRadius >= 0 ? fixedCornerRadius : h / 2f;
        rect.set(0, 0, w, h);
        canvas.drawRoundRect(rect, radius, radius, paint);
    }
}
