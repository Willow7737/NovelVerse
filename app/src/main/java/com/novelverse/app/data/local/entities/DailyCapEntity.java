package com.novelverse.app.data.local.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Tracks the amount of Ink earned by a user today (resets at midnight).
 * Primary key = userId + dateKey ("2026-04-03") composite → represented as single PK string.
 */
@Entity(
    tableName = "daily_caps",
    indices = { @Index(value = {"user_id", "date_key"}, unique = true) }
)
public class DailyCapEntity {

    @PrimaryKey
    @ColumnInfo(name = "id")
    private String id; // userId + "_" + dateKey

    @ColumnInfo(name = "user_id")
    private String userId;

    /** ISO date string yyyy-MM-dd */
    @ColumnInfo(name = "date_key")
    private String dateKey;

    /** Ink earned from reading so far today */
    @ColumnInfo(name = "ink_from_reading")
    private int inkFromReading;

    /** Maximum Ink earnable from reading per day */
    @ColumnInfo(name = "reading_cap")
    private int readingCap;

    /** Number of achievements unlocked today (used for rate-limit check) */
    @ColumnInfo(name = "achievements_today")
    private int achievementsToday;

    /** Epoch millis of the last achievement unlock (for 10/min rate limit) */
    @ColumnInfo(name = "last_achievement_minute_ts")
    private long lastAchievementMinuteTs;

    /** How many achievements unlocked in the current minute window */
    @ColumnInfo(name = "achievements_in_minute")
    private int achievementsInMinute;

    public DailyCapEntity() { this.readingCap = 500; }

    public String getId()                     { return id; }
    public String getUserId()                 { return userId; }
    public String getDateKey()                { return dateKey; }
    public int    getInkFromReading()         { return inkFromReading; }
    public int    getReadingCap()             { return readingCap; }
    public int    getAchievementsToday()      { return achievementsToday; }
    public long   getLastAchievementMinuteTs(){ return lastAchievementMinuteTs; }
    public int    getAchievementsInMinute()   { return achievementsInMinute; }

    public void setId(String id)                           { this.id = id; }
    public void setUserId(String userId)                   { this.userId = userId; }
    public void setDateKey(String dateKey)                 { this.dateKey = dateKey; }
    public void setInkFromReading(int inkFromReading)       { this.inkFromReading = inkFromReading; }
    public void setReadingCap(int readingCap)               { this.readingCap = readingCap; }
    public void setAchievementsToday(int achievementsToday) { this.achievementsToday = achievementsToday; }
    public void setLastAchievementMinuteTs(long ts)        { this.lastAchievementMinuteTs = ts; }
    public void setAchievementsInMinute(int achievementsInMinute) { this.achievementsInMinute = achievementsInMinute; }
}
