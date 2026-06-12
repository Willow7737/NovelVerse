package com.novelverse.app.presentation.home;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;

/**
 * Drives the Quests FAB slide animation.
 *
 * Collapse: the pill slides RIGHT off the screen edge — only the icon portion
 *           stays visible, matching the way FloatingNavBar slides off the bottom.
 * Expand:   the pill slides LEFT back to its natural resting position with a
 *           light overshoot, and the text label fades back in.
 *
 * No width / layout changes are made. Everything is driven by translationX so
 * the layout tree is never invalidated during the animation.
 */
public class QuestsFabAnimator {

    // ── Views ─────────────────────────────────────────────────────────────

    private final ViewGroup container;
    private final View      textView;

    // ── Geometry ──────────────────────────────────────────────────────────

    /**
     * How far to slide right so only the icon section remains on-screen.
     *
     * Icon section = icon(20dp) + paddingStart(14dp) + paddingEnd(14dp) = 48dp.
     * slideDistance = containerWidth − 48dp.
     *
     * Populated lazily on first use so we are safe even if the view has not
     * gone through a layout pass when the animator is constructed.
     */
    private float slideDistance = -1f;

    // ── State ─────────────────────────────────────────────────────────────

    private enum State { EXPANDED, COLLAPSED }
    private State       currentState = State.EXPANDED;
    private AnimatorSet runningSet;

    // ── Timing ────────────────────────────────────────────────────────────

    private static final long COLLAPSE_MS = 260;
    private static final long EXPAND_MS   = 260;

    /** Standard ease-out — matches FloatingNavBar's DecelerateInterpolator feel. */
    private static final Interpolator EASE_OUT      = new DecelerateInterpolator(1.8f);

    /** Slight overshoot on the way back in — lively without being bouncy. */
    private static final Interpolator EASE_OUT_BACK = new PathInterpolator(0.34f, 1.4f, 0.64f, 1f);

    // ── Constructor ───────────────────────────────────────────────────────

    public QuestsFabAnimator(ViewGroup container, View textView, View iconView) {
        this.container = container;
        this.textView  = textView;
        // iconView rides along with the container — no separate animation needed
    }

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Slide the pill to the right so only the icon portion is visible.
     * Mirrors FloatingNavBar.hide() → animateTo(getHeight() + dp(40)).
     */
    public void collapse() {
        if (currentState == State.COLLAPSED) return;
        currentState = State.COLLAPSED;
        cancelRunning();
        ensureSlideDistance();

        // Slide the whole container rightward — text goes off-screen, icon stays
        ObjectAnimator slideOut = ObjectAnimator.ofFloat(
                container, "translationX",
                container.getTranslationX(), slideDistance);
        slideOut.setDuration(COLLAPSE_MS);
        slideOut.setInterpolator(EASE_OUT);

        // Fade text out early — cleaner than letting it get clipped raw
        ObjectAnimator textFade = ObjectAnimator.ofFloat(textView, "alpha", textView.getAlpha(), 0f);
        textFade.setDuration(COLLAPSE_MS / 2);
        textFade.setInterpolator(EASE_OUT);

        runningSet = new AnimatorSet();
        runningSet.playTogether(slideOut, textFade);
        runningSet.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                textView.setVisibility(View.INVISIBLE);
            }
        });
        runningSet.start();
    }

    /**
     * Slide the pill back in from the right to its natural resting position.
     * Mirrors FloatingNavBar.show() → animateTo(0f).
     */
    public void expand() {
        if (currentState == State.EXPANDED) return;
        currentState = State.EXPANDED;
        cancelRunning();
        ensureSlideDistance();

        textView.setVisibility(View.VISIBLE);
        textView.setAlpha(0f);

        // Slide back in with a light overshoot for a lively feel
        ObjectAnimator slideIn = ObjectAnimator.ofFloat(
                container, "translationX",
                container.getTranslationX(), 0f);
        slideIn.setDuration(EXPAND_MS);
        slideIn.setInterpolator(EASE_OUT_BACK);

        // Text fades in once the pill is most of the way back
        ObjectAnimator textFade = ObjectAnimator.ofFloat(textView, "alpha", 0f, 1f);
        textFade.setDuration(180);
        textFade.setStartDelay(EXPAND_MS / 2);
        textFade.setInterpolator(EASE_OUT);

        runningSet = new AnimatorSet();
        runningSet.playTogether(slideIn, textFade);
        runningSet.start();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void cancelRunning() {
        if (runningSet != null && runningSet.isRunning()) {
            runningSet.cancel();
        }
    }

    /**
     * Compute slideDistance lazily, after the view has been laid out.
     * Falls back gracefully to 77dp (125dp − 48dp) if measure returns 0.
     */
    private void ensureSlideDistance() {
        if (slideDistance >= 0f) return;

        float density    = container.getResources().getDisplayMetrics().density;
        int   iconOnlyPx = Math.round(40f * density); // icon(20) + start(14) + end(14)

        int w = container.getWidth();
        if (w == 0) {
            container.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            w = container.getMeasuredWidth();
        }
        if (w == 0) {
            // Last resort: use the hardcoded 125dp from the XML
            w = Math.round(125f * density);
        }

        slideDistance = Math.max(0f, w - iconOnlyPx);
    }
}