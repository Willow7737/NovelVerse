package com.novelverse.app.presentation.onboarding;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages which onboarding bottom sheets to show and when.
 *
 * <h3>Design rules</h3>
 * <ul>
 *   <li>State is stored in a plain (non-encrypted) {@link SharedPreferences} file
 *       whose sole purpose is UI-side onboarding state — nothing sensitive.</li>
 *   <li>"Dismissed" entries persist <em>only for the current login session</em>.
 *       {@link #clearSession()} is called on logout, so the next sign-in resets
 *       all sheets back to "not yet shown".</li>
 *   <li>Each sheet has a unique string key defined as a constant on this class.
 *       Add new sheets here; the rest of the system picks them up automatically.</li>
 * </ul>
 *
 * <h3>Adding a new sheet</h3>
 * <ol>
 *   <li>Add a {@code public static final String KEY_MY_SHEET = "my_sheet";} constant.</li>
 *   <li>Create a class that extends {@link BaseOnboardingSheet} and returns that key.</li>
 *   <li>Call {@code OnboardingManager.get(ctx).shouldShow(KEY_MY_SHEET)} to gate the show.</li>
 * </ol>
 */
public class OnboardingManager {

    // ── Sheet keys ────────────────────────────────────────────────────────
    /** Streak tutorial shown after the user reaches the homepage. */
    public static final String KEY_STREAK_TUTORIAL    = "streak_tutorial";
    /** Enable-notifications prompt. */
    public static final String KEY_ENABLE_NOTIFICATIONS = "enable_notifications";
    // Add future sheets here ↓

    // ── Internals ─────────────────────────────────────────────────────────
    private static final String PREFS_NAME         = "nv_onboarding_state";
    private static final String KEY_DISMISSED_SET  = "dismissed_sheets";

    private static OnboardingManager instance;

    private final SharedPreferences prefs;

    private OnboardingManager(Context ctx) {
        prefs = ctx.getApplicationContext()
                   .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** Get (or create) the singleton instance. */
    public static OnboardingManager get(Context ctx) {
        if (instance == null) {
            instance = new OnboardingManager(ctx.getApplicationContext());
        }
        return instance;
    }

    // ── Query ─────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if the sheet with this key should be shown —
     * i.e. it hasn't been dismissed during the current session.
     */
    public boolean shouldShow(String key) {
        return !getDismissedSet().contains(key);
    }

    // ── Actions ───────────────────────────────────────────────────────────

    /**
     * Mark a sheet as dismissed for this session.
     * Called automatically by {@link BaseOnboardingSheet} when the user taps "Got it"
     * (and optionally also when "Don't show again" is ticked).
     */
    public void dismiss(String key) {
        Set<String> set = getDismissedSet();
        set.add(key);
        prefs.edit().putStringSet(KEY_DISMISSED_SET, set).apply();
    }

    /**
     * Clear all session state.
     * <strong>Call this from your logout flow.</strong>
     *
     * <pre>{@code
     *   OnboardingManager.get(this).clearSession();
     * }</pre>
     */
    public void clearSession() {
        prefs.edit().remove(KEY_DISMISSED_SET).apply();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /** Returns a mutable copy of the persisted dismissed-keys set. */
    private Set<String> getDismissedSet() {
        Set<String> persisted = prefs.getStringSet(KEY_DISMISSED_SET, null);
        return persisted != null ? new HashSet<>(persisted) : new HashSet<>();
    }
}
