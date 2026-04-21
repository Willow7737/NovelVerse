package com.novelverse.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.novelverse.app.data.local.entities.UserAchievementEntity;

import java.util.List;

@Dao
public interface UserAchievementDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(UserAchievementEntity entity);

    @Update
    void update(UserAchievementEntity entity);

    @Query("SELECT * FROM user_achievements WHERE user_id = :userId")
    LiveData<List<UserAchievementEntity>> getAllForUserLive(String userId);

    @Query("SELECT * FROM user_achievements WHERE user_id = :userId")
    List<UserAchievementEntity> getAllForUser(String userId);

    @Query("SELECT * FROM user_achievements WHERE user_id = :userId AND achievement_id = :achievementId")
    UserAchievementEntity get(String userId, String achievementId);

    @Query("SELECT * FROM user_achievements WHERE user_id = :userId AND is_unlocked = 1 ORDER BY unlocked_at DESC")
    LiveData<List<UserAchievementEntity>> getUnlockedLive(String userId);

    @Query("SELECT COUNT(*) FROM user_achievements WHERE user_id = :userId AND is_unlocked = 1")
    int countUnlocked(String userId);

    @Query("UPDATE user_achievements SET current_progress = :progress WHERE user_id = :userId AND achievement_id = :achievementId")
    void updateProgress(String userId, String achievementId, int progress);

    @Query("UPDATE user_achievements SET is_unlocked = 1, unlocked_at = :ts WHERE user_id = :userId AND achievement_id = :achievementId")
    void markUnlocked(String userId, String achievementId, long ts);

    @Query("UPDATE user_achievements SET reward_claimed = 1 WHERE user_id = :userId AND achievement_id = :achievementId")
    void markRewardClaimed(String userId, String achievementId);

    @Query("SELECT * FROM user_achievements WHERE needs_sync = 1")
    List<UserAchievementEntity> getPendingSync();

    @Query("UPDATE user_achievements SET needs_sync = 0 WHERE id = :id")
    void markSynced(String id);
}
