package com.novelverse.app.ui.banner;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.ViewGroup;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Singleton queue-based banner manager.
 *
 * KEY FIX: Tracks the current foreground Activity via ActivityLifecycleCallbacks.
 *
 * Use show() for normal banners.
 * Use queueForNextActivity() when the calling activity is about to finish() —
 * the banner will display on whichever activity resumes next (the parent/caller).
 *
 * Call BannerManager.getInstance().init(application) once in Application.onCreate().
 */
public class BannerManager {

    private static volatile BannerManager instance;

    private final Queue<BannerConfig> queue   = new LinkedList<>();
    private BannerView                current;
    private boolean                   showing = false;

    /** Always the most recently RESUMED (foreground) activity. */
    private Activity currentActivity;

    private BannerManager() {}

    public static BannerManager getInstance() {
        if (instance == null) {
            synchronized (BannerManager.class) {
                if (instance == null) instance = new BannerManager();
            }
        }
        return instance;
    }

    // ── Lifecycle registration ────────────────────────────────────────────────

    public void init(Application app) {
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityResumed(Activity activity) {
                currentActivity = activity;
                // Drain any queued banners now that a valid activity is in the foreground.
                // This is what makes queueForNextActivity() work — the queue was sitting
                // idle and fires the moment the parent activity surfaces.
                if (!showing && !queue.isEmpty()) showNext();
            }
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivityCreated(Activity a, Bundle b) {}
            @Override public void onActivityStarted(Activity a) {}
            @Override public void onActivityStopped(Activity a) {}
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
            @Override public void onActivityDestroyed(Activity a) {
                if (currentActivity == a) currentActivity = null;
            }
        });
    }

    // ── Public entry points ───────────────────────────────────────────────────

    /**
     * Show a banner now, on the current foreground activity.
     * Use this from long-lived activities/fragments that won't finish immediately.
     */
    public void show(Activity activity, BannerConfig config) {
        if (activity != null && !activity.isFinishing()) {
            currentActivity = activity;
        }
        queue.offer(config);
        if (!showing) showNext();
    }

    /**
     * Queue a banner to appear on the NEXT activity that resumes.
     *
     * Use this instead of show() when the calling activity is about to call finish().
     * The banner will display on the parent/caller activity when it comes to the foreground.
     *
     * Pattern:
     *   BannerManager.getInstance().queueForNextActivity(config);
     *   finish(); // banner shows on whatever activity resumes next
     */
    public void queueForNextActivity(BannerConfig config) {
        queue.offer(config);
        // Do NOT call showNext() here. We leave the queue idle.
        // onActivityResumed() on the parent activity will drain it.
    }

    public void dismissCurrent() {
        if (current != null) current.dismiss();
    }

    public void clearAll() {
        queue.clear();
        dismissCurrent();
    }

    // ── Internal queue logic ──────────────────────────────────────────────────

    private void showNext() {
        if (queue.isEmpty()) { showing = false; return; }

        Activity act = currentActivity;
        if (act == null || act.isFinishing()) {
            // No valid foreground activity. Leave the queue intact;
            // onActivityResumed() will retry when the next activity surfaces.
            showing = false;
            return;
        }

        showing = true;
        BannerConfig config = queue.poll();

        Runnable original = config.getOnDismissListener();
        config.setOnDismissListener(() -> {
            if (original != null) original.run();
            current = null;
            showNext();
        });

        act.runOnUiThread(() -> {
            // Re-check: the activity could have finished between the check above
            // and the time the UI thread runs this.
            Activity safeAct = currentActivity;
            if (safeAct == null || safeAct.isFinishing()) {
                showing = false;
                // Put it back so we don't lose it
                if (config.getOnDismissListener() != null) {
                    // Unwrap our chain so the original dismiss listener is preserved
                }
                queue.offer(config);
                return;
            }
            ViewGroup root = safeAct.findViewById(android.R.id.content);
            current = new BannerView(safeAct);
            current.setConfig(config);
            current.show(root);
        });
    }
}
