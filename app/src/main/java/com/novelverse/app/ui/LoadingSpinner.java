package com.novelverse.app.ui;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

/**
 * LoadingSpinner — a black translucent overlay with a centred spinner.
 *
 * Usage:
 *   LoadingSpinner.show(this);   // show overlay
 *   LoadingSpinner.hide(this);   // remove overlay
 *
 * The overlay is attached to android.R.id.content so it sits above all views,
 * including the FloatingNavBar.  It is identified by the tag "loading_overlay"
 * so show() is idempotent and hide() safely no-ops if already gone.
 */
public final class LoadingSpinner {

    private static final String TAG = "loading_overlay";

    private LoadingSpinner() {}

    public static void show(Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        ViewGroup root = activity.findViewById(android.R.id.content);
        if (root == null) return;

        // Idempotent — don't stack multiple overlays
        if (root.findViewWithTag(TAG) != null) return;

        activity.runOnUiThread(() -> {
            // Semi-transparent full-screen scrim
            FrameLayout scrim = new FrameLayout(activity);
            scrim.setTag(TAG);
            scrim.setBackgroundColor(0x66000000); // 40% black
            scrim.setClickable(true);             // block touches below
            scrim.setFocusable(true);

            // Rounded dark card behind the spinner
            FrameLayout card = new FrameLayout(activity);
            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setColor(0xCC1A1A2E);          // ~80% dark navy
            cardBg.setCornerRadius(dp(activity, 20));
            card.setBackground(cardBg);

            int cardSize = dp(activity, 88);
            FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(cardSize, cardSize);
            cardLp.gravity = Gravity.CENTER;
            card.setLayoutParams(cardLp);

            // Spinner inside the card
            ProgressBar spinner = new ProgressBar(activity);
            spinner.setIndeterminate(true);
            spinner.getIndeterminateDrawable()
                   .setColorFilter(0xFFFFFFFF,
                                   android.graphics.PorterDuff.Mode.SRC_IN);
            int spinSize = dp(activity, 40);
            FrameLayout.LayoutParams spinLp = new FrameLayout.LayoutParams(spinSize, spinSize);
            spinLp.gravity = Gravity.CENTER;
            spinner.setLayoutParams(spinLp);

            card.addView(spinner);
            scrim.addView(card);

            root.addView(scrim, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
        });
    }

    public static void hide(Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        activity.runOnUiThread(() -> {
            ViewGroup root = activity.findViewById(android.R.id.content);
            if (root == null) return;
            View overlay = root.findViewWithTag(TAG);
            if (overlay != null) root.removeView(overlay);
        });
    }

    private static int dp(Activity a, int v) {
        return Math.round(v * a.getResources().getDisplayMetrics().density);
    }
}
