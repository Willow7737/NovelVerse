package com.novelverse.app.ui.banner;

import android.app.Activity;

/**
 * Convenience helper — replaces all Toast.makeText() and Snackbar.make() calls.
 *
 * Usage in Activity:   BannerHelper.success(this, "Saved!");
 * Usage in Fragment:   BannerHelper.error(requireActivity(), "Failed");
 */
public final class BannerHelper {

    private BannerHelper() {}

    // ── Quick one-liners ──────────────────────────────────────────────────────

    public static void success(Activity a, String message) {
        show(a, BannerType.SUCCESS, null, message, 3000);
    }

    public static void success(Activity a, String title, String message) {
        show(a, BannerType.SUCCESS, title, message, 3000);
    }

    public static void error(Activity a, String message) {
        show(a, BannerType.ERROR, null, message, 4000);
    }

    public static void error(Activity a, String title, String message) {
        show(a, BannerType.ERROR, title, message, 4000);
    }

    public static void warning(Activity a, String message) {
        show(a, BannerType.WARNING, null, message, 3500);
    }

    public static void warning(Activity a, String title, String message) {
        show(a, BannerType.WARNING, title, message, 3500);
    }

    public static void info(Activity a, String message) {
        show(a, BannerType.INFO, null, message, 3000);
    }

    public static void info(Activity a, String title, String message) {
        show(a, BannerType.INFO, title, message, 3000);
    }

    // ── Full config ───────────────────────────────────────────────────────────

    public static void show(Activity a, BannerType type,
                            String title, String message, long durationMs) {
        if (a == null || a.isFinishing()) return;
        BannerConfig cfg = new BannerConfig()
                .setType(type)
                .setTitle(title)
                .setMessage(message)
                .setDuration(durationMs)
                .setSwipeToDismiss(true);
        BannerManager.getInstance().show(a, cfg);
    }
}
