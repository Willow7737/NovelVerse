package com.novelverse.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.novelverse.app.data.local.entities.DailyCapEntity;

@Dao
public interface DailyCapDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(DailyCapEntity entity);

    @Query("SELECT * FROM daily_caps WHERE user_id = :userId AND date_key = :dateKey")
    DailyCapEntity get(String userId, String dateKey);

    @Query("UPDATE daily_caps SET ink_from_reading = ink_from_reading + :delta WHERE user_id = :userId AND date_key = :dateKey")
    void addReadingInk(String userId, String dateKey, int delta);

    @Query("UPDATE daily_caps SET achievements_today = achievements_today + 1 WHERE user_id = :userId AND date_key = :dateKey")
    void incrementAchievements(String userId, String dateKey);

    @Query("UPDATE daily_caps SET last_achievement_minute_ts = :ts, achievements_in_minute = :count WHERE user_id = :userId AND date_key = :dateKey")
    void updateMinuteWindow(String userId, String dateKey, long ts, int count);

    @Query("DELETE FROM daily_caps WHERE date_key < :dateKey")
    void pruneOlderThan(String dateKey);
}
