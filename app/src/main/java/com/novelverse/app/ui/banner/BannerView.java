package com.novelverse.app.ui.banner;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.novelverse.app.R;

/**
 * BannerView — iOS-style drop-down notification banner.
 *
 * <p>Slides in from above the status bar, auto-dismisses, swipe-up to dismiss, tap-X to dismiss.
 * Uses Inter font, accent colour strip per type.
 */
public class BannerView extends FrameLayout {

    private ImageView iconView;
    private TextView titleView;
    private TextView messageView;
    private ImageView dismissBtn;
    private ProgressBar progressView;
    private BannerConfig config;
    private ValueAnimator progressAnimator;
    private boolean isDismissing = false;

    public BannerView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public BannerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context ctx) {
        inflate(ctx, R.layout.view_banner, this);
        iconView = findViewById(R.id.banner_icon);
        titleView = findViewById(R.id.banner_title);
        messageView = findViewById(R.id.banner_message);
        dismissBtn = findViewById(R.id.banner_dismiss);
        progressView = findViewById(R.id.banner_progress);
        setVisibility(GONE);
    }

    // ── Configuration ─────────────────────────────────────────────────────────

    public void setConfig(BannerConfig config) {
        this.config = config;
        applyConfig();
    }

    private void applyConfig() {
        if (config == null) return;
        BannerType type = config.getType();
        int color = ContextCompat.getColor(getContext(), type.getColorRes());

        // Accent strip colour
        GradientDrawable strip = new GradientDrawable();
        strip.setColor(color);
        strip.setCornerRadius(dp(2));

        // Icon
        iconView.setImageResource(type.getIconRes());
        iconView.setColorFilter(color);

        // Title — use provided or type default
        String title = config.getTitle() != null ? config.getTitle() : type.getDefaultTitle();
        titleView.setText(title);

        // Message
        if (config.getMessage() != null && !config.getMessage().isEmpty()) {
            messageView.setText(config.getMessage());
            messageView.setVisibility(VISIBLE);
        } else {
            messageView.setVisibility(GONE);
        }

        // Progress
        if (progressView != null) {
            progressView.setVisibility(config.isShowProgress() ? VISIBLE : GONE);
        }

        // Dismiss button
        dismissBtn.setOnClickListener(v -> dismiss());

        // Banner tap
        if (config.getOnClickListener() != null) {
            setOnClickListener(v -> config.getOnClickListener().onClick(this));
        }

        // Swipe up to dismiss
        if (config.isSwipeToDismiss()) {
            setupSwipe();
        }
    }

    // ── Display ───────────────────────────────────────────────────────────────

    public void show(ViewGroup parent) {
        if (getParent() != null) ((ViewGroup) getParent()).removeView(this);

        // Position: below status bar
        MarginLayoutParams lp =
                new MarginLayoutParams(
                        MarginLayoutParams.MATCH_PARENT, MarginLayoutParams.WRAP_CONTENT);
        int statusBarH = getStatusBarHeight();
        lp.topMargin = statusBarH + dp(10);
        lp.leftMargin = dp(12);
        lp.rightMargin = dp(12);
        parent.addView(this, lp);

        setVisibility(VISIBLE);
        performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);

        Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.banner_slide_down);
        startAnimation(anim);

        if (config.isShowProgress() && progressView != null) {
            startProgressAnimation();
        }

        postDelayed(this::dismiss, config.getDuration());
    }

    private void startProgressAnimation() {
        progressAnimator = ValueAnimator.ofInt(100, 0);
        progressAnimator.setDuration(config.getDuration());
        progressAnimator.setInterpolator(new LinearInterpolator());
        progressAnimator.addUpdateListener(
                a -> progressView.setProgress((Integer) a.getAnimatedValue()));
        progressAnimator.start();
    }

    public void dismiss() {
        if (isDismissing) return;
        isDismissing = true;
        if (progressAnimator != null) progressAnimator.cancel();

        Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.banner_slide_up);
        anim.setAnimationListener(
                new Animation.AnimationListener() {
                    public void onAnimationStart(Animation a) {}

                    public void onAnimationRepeat(Animation a) {}

                    public void onAnimationEnd(Animation a) {
                        setVisibility(GONE);
                        ViewGroup parent = (ViewGroup) getParent();
                        if (parent != null) parent.removeView(BannerView.this);
                        if (config.getOnDismissListener() != null)
                            config.getOnDismissListener().run();
                    }
                });
        startAnimation(anim);
    }

    // ── Swipe gesture ─────────────────────────────────────────────────────────

    private void setupSwipe() {
        GestureDetector gd =
                new GestureDetector(
                        getContext(),
                        new GestureDetector.SimpleOnGestureListener() {
                            private static final int THRESHOLD = 80;
                            private static final int VELOCITY = 100;

                            @Override
                            public boolean onFling(
                                    MotionEvent e1, MotionEvent e2, float vX, float vY) {
                                if (e1 == null || e2 == null) return false;
                                float dy = e2.getY() - e1.getY();
                                if (dy < -THRESHOLD && Math.abs(vY) > VELOCITY) {
                                    dismiss();
                                    return true;
                                }
                                return false;
                            }
                        });

        setOnTouchListener(
                (v, event) -> {
                    boolean handled = gd.onTouchEvent(event);
                    if (event.getAction() == MotionEvent.ACTION_UP && !handled) {
                        performClick();
                    }
                    return true;
                });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int getStatusBarHeight() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : dp(24);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
