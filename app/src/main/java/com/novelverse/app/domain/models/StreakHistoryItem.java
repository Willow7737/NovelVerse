package com.novelverse.app.domain.models;

/**
 * Represents one completed (broken) streak run fetched from Supabase streak_history.
 *
 * Timestamps are epoch-milliseconds (0 means unknown / null from the server).
 */
public final class StreakHistoryItem {

    public final String id;
    public final int    streakLength;  // how many consecutive days
    public final long   startedAtMs;   // epoch ms, 0 if unknown
    public final long   endedAtMs;     // epoch ms when the streak broke

    public StreakHistoryItem(String id, int streakLength, long startedAtMs, long endedAtMs) {
        this.id           = id;
        this.streakLength = streakLength;
        this.startedAtMs  = startedAtMs;
        this.endedAtMs    = endedAtMs;
    }

    /** True if this break happened within the last 48 hours (still recoverable via ad). */
    public boolean isRecentlyBroken(long nowMs) {
        return (nowMs - endedAtMs) < 2L * 24 * 60 * 60 * 1000L;
    }
}
