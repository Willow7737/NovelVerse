package com.novelverse.app.presentation.common;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.novelverse.app.R;

/**
 * Full-screen image viewer with production-grade swipe-to-dismiss physics.
 *
 * Features:
 *   • Vertical drag to dismiss with parallax translation
 *   • Dynamic scale (1.0 → 0.85) and alpha (1.0 → 0) as you drag
 *   • Velocity fling — fast flick exits regardless of distance
 *   • Spring-back animation when drag is incomplete
 *   • Touch-slop gate — only fires on deliberate vertical drags
 *   • Multi-touch safety — second pointer cancels and resets drag
 *   • Tap toggles system UI visibility
 *   • Back button triggers dismiss animation
 *
 * Launch via {@link #start(Context, String, String)}.
 */
public class ImageViewerActivity extends AppCompatActivity {

    private static final String EXTRA_URL   = "extra_url";
    private static final String EXTRA_LABEL = "extra_label";

    // Dismiss thresholds
    private static final float DISMISS_DISTANCE_FRACTION = 0.25f; // 25% of screen height
    private static final float DISMISS_VELOCITY_PX_S     = 800f;
    private static final float DISMISS_ALPHA_THRESHOLD   = 0.50f;

    // Scale range during drag
    private static final float MIN_SCALE = 0.85f;

    // Spring-back / exit animation durations (ms)
    private static final int ANIM_SPRING_MS  = 280;
    private static final int ANIM_DISMISS_MS = 220;

    // Touch state
    private float   dragStartY      = 0f;
    private float   currentDragY    = 0f;
    private boolean isDragging       = false;
    private boolean isDismissing     = false;
    private int     activePointerId  = MotionEvent.INVALID_POINTER_ID;
    private int     touchSlop;

    // System UI
    private boolean systemUiVisible = true;

    // Views
    private ImageView   imageView;
    private View        scrimView;
    private FrameLayout rootContainer;

    // Physics
    private VelocityTracker   velocityTracker;
    private GestureDetector   gestureDetector;
    private ValueAnimator     activeAnimator;
    private float             screenHeight;

    // ─── Launch ──────────────────────────────────────────────────────────────

    /**
     * @param url   Full image URL to display.
     * @param label Content description (accessibility). Pass null for generic label.
     */
    public static void start(Context ctx, String url, @Nullable String label) {
        Intent i = new Intent(ctx, ImageViewerActivity.class);
        i.putExtra(EXTRA_URL, url);
        if (label != null) i.putExtra(EXTRA_LABEL, label);
        ctx.startActivity(i);
        if (ctx instanceof android.app.Activity) {
            ((android.app.Activity) ctx).overridePendingTransition(
                android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // True edge-to-edge — image fills behind status bar and nav bar
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);

        setContentView(R.layout.activity_image_viewer);

        rootContainer = findViewById(R.id.root_container);
        imageView     = findViewById(R.id.image_view);
        scrimView     = findViewById(R.id.scrim_view);

        screenHeight = getResources().getDisplayMetrics().heightPixels;
        touchSlop    = ViewConfiguration.get(this).getScaledTouchSlop();

        // Hide system UI immediately for a clean full-screen experience
        hideSystemUi();

        // Load image
        String url   = getIntent().getStringExtra(EXTRA_URL);
        String label = getIntent().getStringExtra(EXTRA_LABEL);
        if (label != null) imageView.setContentDescription(label);

        if (url != null && !url.isEmpty()) {
            Glide.with(this)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.bg_profile_banner)
                .into(imageView);
        }

        // Gesture detector — single tap toggles UI; we handle drag ourselves
        gestureDetector = new GestureDetector(this,
            new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    toggleSystemUi();
                    return true;
                }
            });

        // Close button
        View btnClose = findViewById(R.id.btn_close);
        if (btnClose != null) btnClose.setOnClickListener(v -> animateDismiss(1));

        // Wire touch to the root so it covers the entire screen
        rootContainer.setOnTouchListener((v, event) -> {
            handleTouch(event);
            return true;
        });
    }

    @Override
    public void onBackPressed() {
        if (!isDismissing) animateDismiss(1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelActiveAnimator();
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }

    // ─── Touch handling ──────────────────────────────────────────────────────

    private void handleTouch(MotionEvent event) {
        if (isDismissing) return;

        // Forward to gesture detector for tap detection
        gestureDetector.onTouchEvent(event);

        // Track velocity across all events
        if (velocityTracker == null) velocityTracker = VelocityTracker.obtain();
        velocityTracker.addMovement(event);

        switch (event.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:
                activePointerId = event.getPointerId(0);
                dragStartY      = event.getY(event.findPointerIndex(activePointerId));
                currentDragY    = 0f;
                isDragging      = false;
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                // Second finger landed — cancel drag and reset image position
                if (isDragging) {
                    cancelDragAndReset();
                }
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                break;

            case MotionEvent.ACTION_MOVE:
                if (activePointerId == MotionEvent.INVALID_POINTER_ID) break;
                int idx = event.findPointerIndex(activePointerId);
                if (idx < 0) break;

                float dy = event.getY(idx) - dragStartY;

                if (!isDragging) {
                    // Gate: only commit to drag after touch slop in vertical direction
                    if (Math.abs(dy) > touchSlop) {
                        isDragging = true;
                        // Suppress tap detection once drag starts
                        gestureDetector.onTouchEvent(
                            MotionEvent.obtain(event.getDownTime(), event.getEventTime(),
                                MotionEvent.ACTION_CANCEL, 0, 0, 0));
                    } else {
                        break;
                    }
                }

                currentDragY = dy;
                applyDragTransform(currentDragY);
                break;

            case MotionEvent.ACTION_POINTER_UP:
                int pointerIndex = event.getActionIndex();
                int pointerId    = event.getPointerId(pointerIndex);
                if (pointerId == activePointerId) {
                    // Active finger lifted — transfer to remaining finger
                    int newIndex = (pointerIndex == 0) ? 1 : 0;
                    activePointerId = event.getPointerId(newIndex);
                    dragStartY      = event.getY(newIndex) - currentDragY;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!isDragging) {
                    activePointerId = MotionEvent.INVALID_POINTER_ID;
                    break;
                }

                velocityTracker.computeCurrentVelocity(1000);
                float velocityY = velocityTracker.getYVelocity(activePointerId);

                boolean pastDistance = Math.abs(currentDragY) > screenHeight * DISMISS_DISTANCE_FRACTION;
                boolean pastVelocity = Math.abs(velocityY) > DISMISS_VELOCITY_PX_S;
                float   alpha        = computeAlpha(currentDragY);
                boolean pastAlpha    = alpha < DISMISS_ALPHA_THRESHOLD;

                if (pastDistance || pastVelocity || pastAlpha) {
                    // Dismiss in the direction of travel
                    int direction = currentDragY >= 0 ? 1 : -1;
                    animateDismiss(direction);
                } else {
                    springBack();
                }

                activePointerId = MotionEvent.INVALID_POINTER_ID;
                isDragging      = false;
                velocityTracker.recycle();
                velocityTracker = null;
                break;
        }
    }

    // ─── Drag physics ────────────────────────────────────────────────────────

    /**
     * Applies translation, scale, and alpha to both image and scrim as user drags.
     *
     * Drag math:
     *   progress  = |dy| / (screenHeight * 0.5)  — clamped 0..1
     *   scale     = 1.0  → 0.85  (linear with progress)
     *   alpha     = 1.0  → 0.0   (linear with progress, applied to scrim)
     *   imageAlpha= 1.0  → 0.75  (mild fade on image itself)
     */
    private void applyDragTransform(float dy) {
        imageView.setTranslationY(dy);

        float scale = computeScale(dy);
        imageView.setScaleX(scale);
        imageView.setScaleY(scale);

        float alpha = computeAlpha(dy);
        scrimView.setAlpha(alpha);
        imageView.setAlpha(0.75f + 0.25f * alpha); // image stays mostly opaque
    }

    private float computeScale(float dy) {
        float progress = Math.min(1f, Math.abs(dy) / (screenHeight * 0.5f));
        return 1f - (1f - MIN_SCALE) * progress;
    }

    private float computeAlpha(float dy) {
        float progress = Math.min(1f, Math.abs(dy) / (screenHeight * 0.5f));
        return 1f - progress;
    }

    // ─── Animations ──────────────────────────────────────────────────────────

    /**
     * Spring back to center when drag threshold not reached.
     * Uses DecelerateInterpolator for iOS-like natural bounce.
     */
    private void springBack() {
        cancelActiveAnimator();
        float startY     = imageView.getTranslationY();
        float startScale = imageView.getScaleX();
        float startAlpha = scrimView.getAlpha();

        activeAnimator = ValueAnimator.ofFloat(1f, 0f);
        activeAnimator.setDuration(ANIM_SPRING_MS);
        activeAnimator.setInterpolator(new DecelerateInterpolator(2f));
        activeAnimator.addUpdateListener(anim -> {
            float t = (float) anim.getAnimatedValue();
            imageView.setTranslationY(startY * t);
            float sc = startScale + (1f - startScale) * (1f - t);
            imageView.setScaleX(sc);
            imageView.setScaleY(sc);
            scrimView.setAlpha(startAlpha + (1f - startAlpha) * (1f - t));
            imageView.setAlpha(0.75f + 0.25f * scrimView.getAlpha());
        });
        activeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                // Ensure perfect reset
                imageView.setTranslationY(0f);
                imageView.setScaleX(1f);
                imageView.setScaleY(1f);
                imageView.setAlpha(1f);
                scrimView.setAlpha(1f);
                activeAnimator = null;
            }
        });
        activeAnimator.start();
    }

    /**
     * Animates image off screen then finishes the activity.
     * @param direction +1 = drag down/off bottom, -1 = fling up/off top
     */
    private void animateDismiss(int direction) {
        if (isDismissing) return;
        isDismissing = true;
        cancelActiveAnimator();

        float startY     = imageView.getTranslationY();
        float targetY    = direction * screenHeight;
        float startScale = imageView.getScaleX();
        float startAlpha = scrimView.getAlpha();

        activeAnimator = ValueAnimator.ofFloat(0f, 1f);
        activeAnimator.setDuration(ANIM_DISMISS_MS);
        activeAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
        activeAnimator.addUpdateListener(anim -> {
            float t = (float) anim.getAnimatedValue();
            imageView.setTranslationY(startY + (targetY - startY) * t);
            float sc = startScale * (1f - 0.1f * t);
            imageView.setScaleX(sc);
            imageView.setScaleY(sc);
            float a = startAlpha * (1f - t);
            scrimView.setAlpha(a);
            imageView.setAlpha(Math.max(0f, 1f - t));
        });
        activeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                activeAnimator = null;
                finish();
                overridePendingTransition(0, 0);
            }
        });
        activeAnimator.start();
    }

    /** Cancels drag and springs image back to rest without dismissing. */
    private void cancelDragAndReset() {
        isDragging = false;
        springBack();
    }

    private void cancelActiveAnimator() {
        if (activeAnimator != null) {
            activeAnimator.cancel();
            activeAnimator = null;
        }
    }

    // ─── System UI ───────────────────────────────────────────────────────────

    private void toggleSystemUi() {
        if (systemUiVisible) hideSystemUi();
        else                 showSystemUi();
        systemUiVisible = !systemUiVisible;
    }

    private void hideSystemUi() {
        WindowInsetsControllerCompat controller =
            new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        View btnClose = findViewById(R.id.btn_close);
        if (btnClose != null) btnClose.animate().alpha(0f).setDuration(200).start();
    }

    private void showSystemUi() {
        WindowInsetsControllerCompat controller =
            new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.show(WindowInsetsCompat.Type.systemBars());
        View btnClose = findViewById(R.id.btn_close);
        if (btnClose != null) btnClose.animate().alpha(1f).setDuration(200).start();
    }
}
