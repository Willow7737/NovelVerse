package com.novelverse.app.domain.gamification;

import com.novelverse.app.data.local.entities.UserStreakEntity;

import java.util.concurrent.TimeUnit;

/**
 * Pure streak calculation engine — no Android deps, injectable clock for testing.
 *
 * Rules:
 *   • Activity within 24h of last activity → streak extends
 *   • 25–48h gap with freeze active → streak maintained, freeze consumed
 *   • 25–48h gap with grace available (1 per 7-day window) → free pass, no cost
 *   • >48h gap with no protection → streak resets to 1
 *   • Shield Recovery: 150 Quill or free at 30+ streak → restore broken streak
 *
 * Cost constants:
 *   FREEZE_COST_INK  = 50
 *   FREEZE_COST_FREE = 1 per month (0 Ink)
 *   SHIELD_COST_QUILL = 150  (or free at currentStreak >= 30 once per ?)
 */
public final class StreakEngine {

    public static final int FREEZE_COST_INK    = 50;
    public static final int SHIELD_COST_QUILL  = 150;
    public static final int FREE_FREEZES_PER_MONTH = 1;
    private static final long DAY_MS = TimeUnit.DAYS.toMillis(1);
    private static final long SEVEN_DAYS_MS = TimeUnit.DAYS.toMillis(7);
    private static final long MONTH_MS = TimeUnit.DAYS.toMillis(30);

    private StreakEngine() {}

    /**
     * Called when the user performs a qualifying reading activity.
     * Returns a StreakResult with the new state + whether a milestone was crossed.
     */
    public static StreakResult onActivity(UserStreakEntity current, long nowMs) {
        if (current == null) {
            // First ever activity — bootstrap
            UserStreakEntity fresh = new UserStreakEntity();
            fresh.setUserId(null); // caller sets this
            fresh.setCurrentStreak(1);
            fresh.setLongestStreak(1);
            fresh.setLastActivityDate(nowMs);
            fresh.setGraceWindowStart(nowMs);
            fresh.setNeedsSync(true);
            return new StreakResult(0, 1, fresh, StreakEvent.EXTENDED, false);
        }

        long lastActivity = current.getLastActivityDate();
        long gap = nowMs - lastActivity;

        // Already recorded today — no change needed
        if (gap < DAY_MS) {
            return new StreakResult(current.getCurrentStreak(), current.getCurrentStreak(), current, StreakEvent.ALREADY_TODAY, false);
        }

        // ── Check freeze month reset ───────────────────────────────────────
        UserStreakEntity mutable = copyOf(current);
        if (nowMs - current.getFreezeMonthResetAt() >= MONTH_MS) {
            mutable.setFreeFreezesUsedThisMonth(0);
            mutable.setFreezeMonthResetAt(nowMs);
        }

        // ── Check 7-day grace window reset ────────────────────────────────
        if (nowMs - mutable.getGraceWindowStart() >= SEVEN_DAYS_MS) {
            mutable.setGraceUsedInWindow(false);
            mutable.setGraceWindowStart(nowMs);
        }

        int oldStreak = mutable.getCurrentStreak();
        StreakEvent event;

        if (gap <= 2 * DAY_MS) {
            // Within 48h — normal extend
            mutable.setCurrentStreak(oldStreak + 1);
            if (mutable.getCurrentStreak() > mutable.getLongestStreak())
                mutable.setLongestStreak(mutable.getCurrentStreak());
            mutable.setLastActivityDate(nowMs);
            mutable.setFreezeExpiresAt(0); // clear any active freeze
            mutable.setNeedsSync(true);
            event = StreakEvent.EXTENDED;
        } else {
            // Gap > 48h — check protections
            boolean freezeActive = mutable.getFreezeExpiresAt() > lastActivity;
            boolean graceAvailable = !mutable.isGraceUsedInWindow();

            if (freezeActive) {
                // Freeze absorbs the miss
                mutable.setFreezeExpiresAt(0); // consume
                mutable.setCurrentStreak(oldStreak + 1);
                if (mutable.getCurrentStreak() > mutable.getLongestStreak())
                    mutable.setLongestStreak(mutable.getCurrentStreak());
                mutable.setLastActivityDate(nowMs);
                mutable.setNeedsSync(true);
                event = StreakEvent.FREEZE_CONSUMED;
            } else if (graceAvailable) {
                mutable.setGraceUsedInWindow(true);
                mutable.setCurrentStreak(oldStreak + 1);
                if (mutable.getCurrentStreak() > mutable.getLongestStreak())
                    mutable.setLongestStreak(mutable.getCurrentStreak());
                mutable.setLastActivityDate(nowMs);
                mutable.setNeedsSync(true);
                event = StreakEvent.GRACE_USED;
            } else {
                // Streak broken
                mutable.setCurrentStreak(1);
                mutable.setLastActivityDate(nowMs);
                mutable.setGraceWindowStart(nowMs);
                mutable.setGraceUsedInWindow(false);
                mutable.setNeedsSync(true);
                event = StreakEvent.BROKEN;
            }
        }

        boolean milestone = isMilestone(mutable.getCurrentStreak());
        return new StreakResult(oldStreak, mutable.getCurrentStreak(), mutable, event, milestone);
    }

    /**
     * Apply a streak freeze. Returns updated entity or null if freeze not applicable.
     * @param useFreeFreeze true = use the monthly free freeze; false = spend 50 Ink
     */
    public static UserStreakEntity applyFreeze(UserStreakEntity current, boolean useFreeFreeze, long nowMs) {
        if (useFreeFreeze && current.getFreeFreezesUsedThisMonth() >= FREE_FREEZES_PER_MONTH) {
            return null; // no free freezes left
        }
        UserStreakEntity mutable = copyOf(current);
        // Freeze expires 48h from now
        mutable.setFreezeExpiresAt(nowMs + 2 * DAY_MS);
        if (useFreeFreeze) {
            mutable.setFreeFreezesUsedThisMonth(current.getFreeFreezesUsedThisMonth() + 1);
        }
        mutable.setNeedsSync(true);
        return mutable;
    }

    /**
     * Apply a shield recovery (restores streak after a break).
     * Cost: 150 Quill OR free if streak was >= 30 at time of break.
     */
    public static UserStreakEntity applyShieldRecovery(UserStreakEntity current, int recoveredStreak, long nowMs) {
        UserStreakEntity mutable = copyOf(current);
        mutable.setCurrentStreak(recoveredStreak);
        if (recoveredStreak > mutable.getLongestStreak())
            mutable.setLongestStreak(recoveredStreak);
        mutable.setLastActivityDate(nowMs);
        mutable.setNeedsSync(true);
        return mutable;
    }

    public static boolean isMilestone(int streak) {
        return streak == 3 || streak == 7 || streak == 30 || streak == 100;
    }

    public static boolean hasFreeFreeze(UserStreakEntity e) {
        return e.getFreeFreezesUsedThisMonth() < FREE_FREEZES_PER_MONTH;
    }

    public static boolean shieldFree(int streakBeforeBroken) {
        return streakBeforeBroken >= 30;
    }

    private static UserStreakEntity copyOf(UserStreakEntity src) {
        UserStreakEntity e = new UserStreakEntity();
        e.setUserId(src.getUserId());
        e.setCurrentStreak(src.getCurrentStreak());
        e.setLongestStreak(src.getLongestStreak());
        e.setLastActivityDate(src.getLastActivityDate());
        e.setGraceWindowStart(src.getGraceWindowStart());
        e.setGraceUsedInWindow(src.isGraceUsedInWindow());
        e.setFreezeExpiresAt(src.getFreezeExpiresAt());
        e.setFreeFreezesUsedThisMonth(src.getFreeFreezesUsedThisMonth());
        e.setFreezeMonthResetAt(src.getFreezeMonthResetAt());
        return e;
    }

    // ── Enums & Result ────────────────────────────────────────────────────────

    public enum StreakEvent {
        EXTENDED, ALREADY_TODAY, FREEZE_CONSUMED, GRACE_USED, BROKEN
    }

    public static final class StreakResult {
        public final int oldStreak;
        public final int newStreak;
        public final UserStreakEntity updated;
        public final StreakEvent event;
        public final boolean isMilestone;

        StreakResult(int old, int newS, UserStreakEntity updated, StreakEvent event, boolean milestone) {
            this.oldStreak   = old;
            this.newStreak   = newS;
            this.updated     = updated;
            this.event       = event;
            this.isMilestone = milestone;
        }
    }
}
