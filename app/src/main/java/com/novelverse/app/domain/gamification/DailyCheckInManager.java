package com.novelverse.app.domain.gamification;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Manages the daily check-in bonus (separate from streak — this is the
 * passive "open the app" reward, not reading activity).
 *
 * Rules:
 *   - One bonus per calendar day, max 20 Ink
 *   - Bonus scales with consecutive check-in days: day 1=5, day 2=8, day 3+=10,
 *     week milestone days (7, 14, 21, 30) = 20 Ink
 *   - Stored in SharedPreferences (not Room — doesn't need sync)
 */
public final class DailyCheckInManager {

    private static final String PREFS = "daily_checkin";
    private static final String KEY_LAST_DATE    = "last_checkin_date";
    private static final String KEY_CONSECUTIVE  = "consecutive_days";
    private static final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private DailyCheckInManager() {}

    public static final class CheckInResult {
        public final boolean isFirstToday;
        public final int     inkReward;
        public final int     consecutiveDays;

        CheckInResult(boolean first, int ink, int consecutive) {
            this.isFirstToday    = first;
            this.inkReward       = ink;
            this.consecutiveDays = consecutive;
        }
    }

    /**
     * Call once per app open from HomeActivity.
     * Returns result; caller decides whether to show UI.
     */
    public static CheckInResult checkIn(Context context, long nowMs) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String today     = FMT.format(new Date(nowMs));
        String lastDate  = sp.getString(KEY_LAST_DATE, "");
        int    consecutive = sp.getInt(KEY_CONSECUTIVE, 0);

        if (today.equals(lastDate)) {
            return new CheckInResult(false, 0, consecutive);
        }

        // Determine if yesterday's check-in maintained streak
        boolean wasYesterday = false;
        try {
            if (!lastDate.isEmpty()) {
                Date last = FMT.parse(lastDate);
                if (last != null) {
                    long diffMs = nowMs - last.getTime();
                    wasYesterday = diffMs >= 0 && diffMs < 2 * 24 * 3600_000L;
                }
            }
        } catch (Exception ignored) {}

        int newConsecutive = wasYesterday ? consecutive + 1 : 1;
        int inkReward = inkForDay(newConsecutive);

        sp.edit()
            .putString(KEY_LAST_DATE, today)
            .putInt(KEY_CONSECUTIVE, newConsecutive)
            .apply();

        return new CheckInResult(true, inkReward, newConsecutive);
    }

    /** Check if already checked in today without mutating state. */
    public static boolean alreadyCheckedInToday(Context context, long nowMs) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String today = FMT.format(new Date(nowMs));
        return today.equals(sp.getString(KEY_LAST_DATE, ""));
    }

    public static int getConsecutiveDays(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_CONSECUTIVE, 0);
    }

    private static int inkForDay(int day) {
        if (day == 7 || day == 14 || day == 21 || day == 30) return 20; // milestone
        if (day >= 3) return 10;
        if (day == 2) return 8;
        return 5; // day 1
    }
}
