package com.novelverse.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.novelverse.app.data.local.entities.UserStreakEntity;

import java.util.List;

@Dao
public interface UserStreakDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(UserStreakEntity entity);

    @Update
    void update(UserStreakEntity entity);

    @Query("SELECT * FROM user_streaks WHERE user_id = :userId")
    UserStreakEntity get(String userId);

    @Query("SELECT * FROM user_streaks WHERE user_id = :userId")
    LiveData<UserStreakEntity> getLive(String userId);

    @Query("UPDATE user_streaks SET current_streak = :streak, longest_streak = :longest, last_activity_date = :lastActivity, needs_sync = 1 WHERE user_id = :userId")
    void updateStreak(String userId, int streak, int longest, long lastActivity);

    @Query("UPDATE user_streaks SET freeze_expires_at = :expiresAt, free_freezes_used_this_month = :freeUsed, needs_sync = 1 WHERE user_id = :userId")
    void applyFreeze(String userId, long expiresAt, int freeUsed);

    @Query("UPDATE user_streaks SET grace_used_in_window = :used, grace_window_start = :windowStart WHERE user_id = :userId")
    void updateGrace(String userId, boolean used, long windowStart);

    @Query("UPDATE user_streaks SET free_freezes_used_this_month = 0, freeze_month_reset_at = :resetAt WHERE user_id = :userId")
    void resetMonthlyFreezes(String userId, long resetAt);

    @Query("SELECT * FROM user_streaks WHERE needs_sync = 1")
    List<UserStreakEntity> getPendingSync();

    @Query("UPDATE user_streaks SET needs_sync = 0 WHERE user_id = :userId")
    void markSynced(String userId);
}
