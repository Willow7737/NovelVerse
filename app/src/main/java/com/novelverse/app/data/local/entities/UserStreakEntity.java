package com.novelverse.app.data.local.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Streak state. One row per user.
 *
 * Freeze logic:
 *   - freeFreezesUsedThisMonth: increments when the user spends 0 Ink for a freeze
 *   - freeFreezesPerMonth = 1
 *   - A paid freeze costs 50 Ink
 *
 * Grace period:
 *   - 1 break per 7-day window (no cost, no freeze consumed)
 *   - graceUsedInWindow tracks whether it has been used in the current 7-day window
 */
@Entity(
    tableName = "user_streaks",
    indices = { @Index(value = "user_id", unique = true) }
)
public class UserStreakEntity {

    @PrimaryKey
    @ColumnInfo(name = "user_id")
    private String userId;

    @ColumnInfo(name = "current_streak")
    private int currentStreak;

    @ColumnInfo(name = "longest_streak")
    private int longestStreak;

    /** Epoch millis of the last activity that extended or maintained the streak */
    @ColumnInfo(name = "last_activity_date")
    private long lastActivityDate;

    /** Epoch millis of window start (for 7-day grace tracking) */
    @ColumnInfo(name = "grace_window_start")
    private long graceWindowStart;

    @ColumnInfo(name = "grace_used_in_window")
    private boolean graceUsedInWindow;

    /** Active freeze: epoch millis when the freeze expires (0 = no freeze) */
    @ColumnInfo(name = "freeze_expires_at")
    private long freezeExpiresAt;

    /** Free freezes used in the current calendar month (reset monthly) */
    @ColumnInfo(name = "free_freezes_used_this_month")
    private int freeFreezesUsedThisMonth;

    /** Epoch millis of the month-reset boundary */
    @ColumnInfo(name = "freeze_month_reset_at")
    private long freezeMonthResetAt;

    @ColumnInfo(name = "needs_sync")
    private boolean needsSync;

    public UserStreakEntity() {}

    public String  getUserId()                  { return userId; }
    public int     getCurrentStreak()           { return currentStreak; }
    public int     getLongestStreak()           { return longestStreak; }
    public long    getLastActivityDate()         { return lastActivityDate; }
    public long    getGraceWindowStart()        { return graceWindowStart; }
    public boolean isGraceUsedInWindow()        { return graceUsedInWindow; }
    public long    getFreezeExpiresAt()         { return freezeExpiresAt; }
    public int     getFreeFreezesUsedThisMonth(){ return freeFreezesUsedThisMonth; }
    public long    getFreezeMonthResetAt()      { return freezeMonthResetAt; }
    public boolean isNeedsSync()                { return needsSync; }

    public void setUserId(String userId)                           { this.userId = userId; }
    public void setCurrentStreak(int currentStreak)               { this.currentStreak = currentStreak; }
    public void setLongestStreak(int longestStreak)               { this.longestStreak = longestStreak; }
    public void setLastActivityDate(long lastActivityDate)         { this.lastActivityDate = lastActivityDate; }
    public void setGraceWindowStart(long graceWindowStart)         { this.graceWindowStart = graceWindowStart; }
    public void setGraceUsedInWindow(boolean graceUsedInWindow)   { this.graceUsedInWindow = graceUsedInWindow; }
    public void setFreezeExpiresAt(long freezeExpiresAt)          { this.freezeExpiresAt = freezeExpiresAt; }
    public void setFreeFreezesUsedThisMonth(int freeFreezesUsedThisMonth) { this.freeFreezesUsedThisMonth = freeFreezesUsedThisMonth; }
    public void setFreezeMonthResetAt(long freezeMonthResetAt)    { this.freezeMonthResetAt = freezeMonthResetAt; }
    public void setNeedsSync(boolean needsSync)                    { this.needsSync = needsSync; }
}
