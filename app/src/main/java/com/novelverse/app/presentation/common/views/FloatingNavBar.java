package com.novelverse.app.presentation.common.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.novelverse.app.R;

/**
 * FloatingNavBar — Real-time GPU-Accelerated Glassmorphism
 *
 * Key features:
 * - Real-time live background capture with smart throttling
 * - Self-exclusion during capture (setWillNotDraw) to prevent feedback loop
 * - RenderScript GPU-accelerated blur with CPU fallback
 * - Cached/reused bitmaps for zero-allocation rendering
 * - Horizontal indicator margin EQUALS vertical margin (both 14dp)
 * - Clipped rounded corners for clean blur edges
 * - All colour constants resolved from @color resources so both light and
 *   dark mode receive correct values automatically via values-night/colors.xml
 */
public class FloatingNavBar extends View {

    // ── Glass Colors — resolved from resources in init() ──────────────────
    // (see res/values/colors.xml  →  nav_*  entries)
    // (see res/values-night/colors.xml  →  dark overrides)
    private int colorGlassBase;
    private int colorGlassHaze;
    private int colorNoise;
    private int colorEdgeLight;
    private int colorEdgeShadow;
    private int colorIndicator;
    private int colorIconInactive;
    private int colorIconActive;

    // ── Dimensions ───────────────────────────────────────────────────────
    private float cornerRadius;
    private float indicatorCornerRadius;
    private float indicatorWidth;
    private float indicatorHeight;
    private float iconSize;
    private float edgeStrokeWidth;
    private float margin;           // BOTH horizontal AND vertical margin (equal)
    private float hPad;             // Horizontal padding (equals margin)

    // ── State ────────────────────────────────────────────────────────────
    private int itemCount   = 3;
    private int activeIndex = 0;
    private float indicatorX;
    private float targetIndicatorX;
    private static final float LERP_FACTOR = 0.12f;

    // ── Animation ────────────────────────────────────────────────────────
    private float scrollTranslationY = 0f;
    private ValueAnimator scrollAnimator;

    // ── Paint & Rects ────────────────────────────────────────────────────
    private final Paint basePaint       = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hazePaint       = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint noisePaint      = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint highlightPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint indicatorPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blurBitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF bgRect          = new RectF();
    private final RectF indicatorRect   = new RectF();

    // ── Noise ────────────────────────────────────────────────────────────
    private Bitmap  noiseBitmap;
    private boolean noiseReady = false;

    // ── Icons ────────────────────────────────────────────────────────────
    private Drawable[] icons;
    private int[]      iconRes;

    // ── Real-time Blur Engine ────────────────────────────────────────────
    private View    sourceView;
    private boolean isCapturing = false;
    private RenderScript         renderScript;
    private ScriptIntrinsicBlur  blurScript;

    // Cached bitmaps — reused every frame
    private Bitmap   captureBitmap;
    private Canvas   captureCanvas;
    private Bitmap   blurredBitmap;
    private Allocation allocIn, allocOut;

    private static final float BLUR_DOWNSCALE    = 0.125f;  // 8× downscale
    private static final int   BLUR_RADIUS       = 25;
    private int cachedDownW, cachedDownH;

    // Throttling for smoother performance
    private long lastBlurFrameTime = 0;
    private static final long MIN_BLUR_INTERVAL_MS = 16; // ~60 fps max

    // Clipping path for rounded corners
    private final Path clipPath = new Path();

    // ── Callback ─────────────────────────────────────────────────────────
    public interface OnItemSelectedListener {
        void onItemSelected(int index);
    }
    private OnItemSelectedListener listener;

    // ─────────────────────────────────────────────────────────────────────
    public FloatingNavBar(Context context) { super(context); init(context); }
    public FloatingNavBar(Context context, @Nullable AttributeSet attrs) { super(context, attrs); init(context); }
    public FloatingNavBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr); init(context);
    }

    private void init(Context ctx) {
        float d = ctx.getResources().getDisplayMetrics().density;

        // ── Resolve colours from resources (respects light/dark mode) ──
        colorGlassBase    = ContextCompat.getColor(ctx, R.color.nav_glass_base);
        colorGlassHaze    = ContextCompat.getColor(ctx, R.color.nav_glass_haze);
        colorNoise        = ContextCompat.getColor(ctx, R.color.nav_noise);
        colorEdgeLight    = ContextCompat.getColor(ctx, R.color.nav_edge_light);
        colorEdgeShadow   = ContextCompat.getColor(ctx, R.color.nav_edge_shadow);
        colorIndicator    = ContextCompat.getColor(ctx, R.color.nav_indicator);
        colorIconInactive = ContextCompat.getColor(ctx, R.color.nav_icon_inactive);
        colorIconActive   = ContextCompat.getColor(ctx, R.color.nav_icon_active);

        // ── Dimensions ─────────────────────────────────────────────────
        cornerRadius          = 28f * d;
        indicatorCornerRadius = 20f * d;
        indicatorWidth        = 64f * d;
        indicatorHeight       = 48f * d;
        iconSize              = 26f * d;
        edgeStrokeWidth       = 1.0f * d;

        float navHeight = 76f * d;
        margin = (navHeight - indicatorHeight) / 2f; // 14dp
        hPad   = margin;

        // ── Apply colours to paints ─────────────────────────────────────
        basePaint.setColor(colorGlassBase);
        hazePaint.setColor(colorGlassHaze);
        noisePaint.setColor(colorNoise);

        highlightPaint.setColor(colorEdgeLight);
        highlightPaint.setStyle(Paint.Style.STROKE);
        highlightPaint.setStrokeWidth(edgeStrokeWidth);

        shadowPaint.setColor(colorEdgeShadow);
        shadowPaint.setStyle(Paint.Style.STROKE);
        shadowPaint.setStrokeWidth(edgeStrokeWidth);

        indicatorPaint.setColor(colorIndicator);

        iconRes = new int[]{ R.drawable.ic_home, R.drawable.ic_library, R.drawable.ic_avatar_placeholder };
        loadIcons(ctx);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setElevation(8f * d);
        }

        setLayerType(LAYER_TYPE_SOFTWARE, null);

        // ── RenderScript blur ───────────────────────────────────────────
        try {
            renderScript = RenderScript.create(ctx);
            blurScript   = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
            blurScript.setRadius(BLUR_RADIUS);
        } catch (Exception e) {
            renderScript = null;
            blurScript   = null;
        }
    }

    // ── Source View (for hide/show scroll wiring) ────────────────────────

    public void setSourceView(View view) { this.sourceView = view; }

    public void attachToScrollable(View scrollable) {
        if (scrollable == null) return;
        if (scrollable instanceof androidx.core.widget.NestedScrollView) {
            ((androidx.core.widget.NestedScrollView) scrollable).setOnScrollChangeListener(
                (androidx.core.widget.NestedScrollView.OnScrollChangeListener)
                (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                    int dy = scrollY - oldScrollY;
                    if (Math.abs(dy) < 6) return;
                    if (dy > 0) hide(); else show();
                });
        }
    }

    public void attachToScroll(androidx.core.widget.NestedScrollView nsv) {
        attachToScrollable(nsv);
    }

    public void attachToRecyclerView(androidx.recyclerview.widget.RecyclerView rv) {
        if (rv == null) return;
        rv.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@androidx.annotation.NonNull
                    androidx.recyclerview.widget.RecyclerView recyclerView, int dx, int dy) {
                if (Math.abs(dy) < 6) return;
                if (dy > 0) hide(); else show();
            }
        });
    }

    // ── Real-time Blur ───────────────────────────────────────────────────

    private void ensureBlurBuffers(int viewW, int viewH) {
        int downW = Math.max(1, (int)(viewW * BLUR_DOWNSCALE));
        int downH = Math.max(1, (int)(viewH * BLUR_DOWNSCALE));

        if (downW == cachedDownW && downH == cachedDownH
                && captureBitmap != null && blurredBitmap != null) return;

        cachedDownW = downW;
        cachedDownH = downH;

        if (captureBitmap != null) captureBitmap.recycle();
        captureBitmap = Bitmap.createBitmap(downW, downH, Bitmap.Config.ARGB_8888);
        captureCanvas = new Canvas(captureBitmap);

        if (blurredBitmap != null) blurredBitmap.recycle();
        blurredBitmap = Bitmap.createBitmap(downW, downH, Bitmap.Config.ARGB_8888);

        if (renderScript != null) {
            if (allocIn  != null) allocIn.destroy();
            if (allocOut != null) allocOut.destroy();
            allocIn  = Allocation.createFromBitmap(renderScript, captureBitmap);
            allocOut = Allocation.createFromBitmap(renderScript, blurredBitmap);
        }
    }

    private void captureAndBlur() {
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;

        long now = System.currentTimeMillis();
        if (now - lastBlurFrameTime < MIN_BLUR_INTERVAL_MS) return;
        lastBlurFrameTime = now;

        View rootView = getRootView();
        if (rootView == null || rootView.getWidth() == 0) return;

        ensureBlurBuffers(w, h);

        int[] loc = new int[2];
        getLocationOnScreen(loc);
        captureBitmap.eraseColor(Color.TRANSPARENT);

        isCapturing = true;
        boolean wasWillNotDraw = willNotDraw();
        setWillNotDraw(true);

        captureCanvas.save();
        captureCanvas.scale(BLUR_DOWNSCALE, BLUR_DOWNSCALE);
        captureCanvas.translate(-loc[0], -loc[1]);
        try {
            rootView.draw(captureCanvas);
        } catch (Exception ignored) { }
        captureCanvas.restore();

        setWillNotDraw(wasWillNotDraw);
        isCapturing = false;

        if (renderScript != null && blurScript != null && allocIn != null && allocOut != null) {
            try {
                allocIn.copyFrom(captureBitmap);
                blurScript.setInput(allocIn);
                blurScript.forEach(allocOut);
                allocOut.copyTo(blurredBitmap);
            } catch (Exception e) {
                fastBlurFallback(captureBitmap, blurredBitmap, BLUR_RADIUS / 4);
            }
        } else {
            fastBlurFallback(captureBitmap, blurredBitmap, BLUR_RADIUS / 4);
        }
    }

    private void fastBlurFallback(Bitmap src, Bitmap dst, int radius) {
        if (radius < 1) {
            new Canvas(dst).drawBitmap(src, 0, 0, null);
            return;
        }
        int w = src.getWidth(), h = src.getHeight();
        int[] pixels = new int[w * h];
        src.getPixels(pixels, 0, w, 0, 0, w, h);
        int[] temp = new int[w * h];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int r = 0, g = 0, b = 0, a = 0, count = 0;
                for (int k = -radius; k <= radius; k++) {
                    int px = x + k;
                    if (px >= 0 && px < w) {
                        int p = pixels[y * w + px];
                        a += (p >>> 24) & 0xFF; r += (p >>> 16) & 0xFF;
                        g += (p >>> 8)  & 0xFF; b += p & 0xFF; count++;
                    }
                }
                temp[y * w + x] = ((a/count)<<24)|((r/count)<<16)|((g/count)<<8)|(b/count);
            }
        }
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int r = 0, g = 0, b = 0, a = 0, count = 0;
                for (int k = -radius; k <= radius; k++) {
                    int py = y + k;
                    if (py >= 0 && py < h) {
                        int p = temp[py * w + x];
                        a += (p >>> 24) & 0xFF; r += (p >>> 16) & 0xFF;
                        g += (p >>> 8)  & 0xFF; b += p & 0xFF; count++;
                    }
                }
                pixels[y * w + x] = ((a/count)<<24)|((r/count)<<16)|((g/count)<<8)|(b/count);
            }
        }
        dst.setPixels(pixels, 0, w, 0, 0, w, h);
    }

    // ── Noise & Icons ────────────────────────────────────────────────────

    private void generateNoise(int w, int h) {
        if (noiseReady || w <= 0 || h <= 0) return;
        int nw = w / 4, nh = h / 4;
        noiseBitmap = Bitmap.createBitmap(nw, nh, Bitmap.Config.ALPHA_8);
        Canvas c = new Canvas(noiseBitmap);
        Paint  p = new Paint();
        for (int x = 0; x < nw; x++) {
            for (int y = 0; y < nh; y++) {
                int noise = (int)(Math.random() * 30);
                p.setColor(Color.argb(noise, 0, 0, 0));
                c.drawPoint(x, y, p);
            }
        }
        noiseReady = true;
    }

    private void loadIcons(Context ctx) {
        icons = new Drawable[iconRes.length];
        for (int i = 0; i < iconRes.length; i++) {
            Drawable d = ContextCompat.getDrawable(ctx, iconRes[i]);
            icons[i] = d != null ? d.mutate() : null;
        }
    }

    // ── Public API ───────────────────────────────────────────────────────

    public void setOnItemSelectedListener(OnItemSelectedListener l) { this.listener = l; }

    public void setActiveIndex(int index) {
        if (index < 0 || index >= itemCount) return;
        float startX = indicatorX;
        activeIndex  = index;
        targetIndicatorX = getConstrainedItemCenterX(index);
        if (indicatorX == 0) indicatorX = targetIndicatorX;

        ValueAnimator animator = ValueAnimator.ofFloat(startX, targetIndicatorX);
        animator.setDuration(300);
        animator.setInterpolator(new FastOutSlowInInterpolator());
        animator.addUpdateListener(a -> { indicatorX = (float) a.getAnimatedValue(); invalidate(); });
        animator.start();
    }

    public int getActiveIndex() { return activeIndex; }

    public void setIcons(int[] resIds) {
        iconRes   = resIds;
        itemCount = resIds.length;
        loadIcons(getContext());
        requestLayout();
        setActiveIndex(Math.min(activeIndex, itemCount - 1));
    }

    public void setProfileIcon(Drawable drawable) {
        if (icons != null && icons.length > 0 && drawable != null) {
            icons[icons.length - 1] = drawable.mutate();
            invalidate();
        }
    }

    // ── Scroll Handling ──────────────────────────────────────────────────

    public void hide() { animateTo(getHeight() + dp(40)); }
    public void show() { animateTo(0f); }

    private void animateTo(float targetY) {
        if (Math.abs(scrollTranslationY - targetY) < 0.5f) return;
        if (scrollAnimator != null) scrollAnimator.cancel();
        scrollAnimator = ValueAnimator.ofFloat(scrollTranslationY, targetY);
        scrollAnimator.setDuration(250);
        scrollAnimator.setInterpolator(new DecelerateInterpolator(1.8f));
        scrollAnimator.addUpdateListener(anim -> {
            scrollTranslationY = (float) anim.getAnimatedValue();
            setTranslationY(scrollTranslationY);
        });
        scrollAnimator.start();
    }

    // ── Measurement ───────────────────────────────────────────────────────

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float d = getResources().getDisplayMetrics().density;
        float slotW = 76f * d;
        float computedMargin = (76f * d - 48f * d) / 2f;
        int w = (int)(itemCount * slotW + computedMargin * 2);
        int h = (int)(76f * d);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        bgRect.set(0, 0, w, h);
        margin = (h - indicatorHeight) / 2f;
        hPad   = margin;
        indicatorX       = getConstrainedItemCenterX(activeIndex);
        targetIndicatorX = indicatorX;
        noiseReady = false;
        clipPath.reset();
        clipPath.addRoundRect(bgRect, cornerRadius, cornerRadius, Path.Direction.CW);
    }

    // ── Drawing ───────────────────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isCapturing) return;

        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;

        // 1. Real-time blurred background
        captureAndBlur();
        canvas.save();
        canvas.clipPath(clipPath);
        if (blurredBitmap != null && !blurredBitmap.isRecycled()) {
            canvas.drawBitmap(blurredBitmap, null, new RectF(0, 0, w, h), blurBitmapPaint);
        }
        canvas.restore();

        // 2. Glass layers
        generateNoise(w, h);
        canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, basePaint);
        canvas.drawRoundRect(bgRect, cornerRadius, cornerRadius, hazePaint);
        if (noiseBitmap != null) {
            Paint p = new Paint(); p.setAlpha(40);
            canvas.drawBitmap(noiseBitmap, null, new RectF(0, 0, w, h), p);
        }

        // Edge highlights
        float inset = edgeStrokeWidth / 2f;
        RectF edgeRect = new RectF(bgRect.left + inset, bgRect.top + inset,
                                   bgRect.right - inset, bgRect.bottom - inset);
        canvas.drawRoundRect(edgeRect, cornerRadius - inset, cornerRadius - inset, highlightPaint);
        canvas.drawRoundRect(edgeRect, cornerRadius - inset, cornerRadius - inset, shadowPaint);

        // 3. Indicator
        if (Math.abs(indicatorX - targetIndicatorX) > 0.5f) {
            indicatorX += (targetIndicatorX - indicatorX) * LERP_FACTOR;
            postInvalidateOnAnimation();
        } else {
            indicatorX = targetIndicatorX;
        }

        float indLeft = indicatorX - indicatorWidth / 2f;
        float indTop  = (h - indicatorHeight) / 2f;
        indLeft = Math.max(margin, Math.min(indLeft, w - indicatorWidth - margin));
        indicatorRect.set(indLeft, indTop, indLeft + indicatorWidth, indTop + indicatorHeight);
        canvas.drawRoundRect(indicatorRect, indicatorCornerRadius, indicatorCornerRadius, indicatorPaint);

        // 4. Icons
        for (int i = 0; i < itemCount && i < icons.length; i++) {
            Drawable icon = icons[i];
            if (icon == null) continue;
            float cx  = getItemCenterX(i);
            int left  = (int)(cx - iconSize / 2f);
            int top   = (int)((h - iconSize) / 2f);
            icon.setBounds(left, top, (int)(left + iconSize), (int)(top + iconSize));
            boolean active = (i == activeIndex);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                icon.setTint(active ? colorIconActive : colorIconInactive);
            }
            icon.draw(canvas);
        }

        // 5. Schedule next frame
        if (isAttachedToWindow() && getVisibility() == VISIBLE) {
            postInvalidateOnAnimation();
        }
    }

    // ── Touch Handling ───────────────────────────────────────────────────

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            performClick();
            float x   = event.getX();
            int   idx = getIndexForX(x);
            if (idx >= 0 && idx != activeIndex) {
                setActiveIndex(idx);
                if (listener != null) listener.onItemSelected(idx);
            } else if (idx == activeIndex) {
                show();
            }
        }
        return true;
    }

    @Override public boolean performClick() { return super.performClick(); }

    // ── Helpers ───────────────────────────────────────────────────────────

    private float getConstrainedItemCenterX(int i) {
        float cx   = getItemCenterX(i);
        float minX = margin + indicatorWidth / 2f;
        float maxX = getWidth() - margin - indicatorWidth / 2f;
        return Math.max(minX, Math.min(cx, maxX));
    }

    private float getItemCenterX(int i) {
        float slot = (getWidth() - margin * 2f) / itemCount;
        return margin + (slot * i) + (slot / 2f);
    }

    private int getIndexForX(float x) {
        if (x < margin) return 0;
        float slot = (getWidth() - margin * 2f) / itemCount;
        int idx = (int)((x - margin) / slot);
        return Math.max(0, Math.min(idx, itemCount - 1));
    }

    private int dp(int px) {
        return Math.round(px * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (allocIn  != null) { allocIn.destroy();  allocIn  = null; }
        if (allocOut != null) { allocOut.destroy(); allocOut = null; }
        if (blurScript   != null) { blurScript.destroy();   blurScript   = null; }
        if (renderScript != null) { renderScript.destroy(); renderScript = null; }
        if (captureBitmap != null && !captureBitmap.isRecycled()) { captureBitmap.recycle(); captureBitmap = null; }
        if (blurredBitmap != null && !blurredBitmap.isRecycled()) { blurredBitmap.recycle(); blurredBitmap = null; }
        if (noiseBitmap   != null && !noiseBitmap.isRecycled())   { noiseBitmap.recycle();   noiseBitmap   = null; }
        captureCanvas = null;
        cachedDownW = 0; cachedDownH = 0;
    }
}
