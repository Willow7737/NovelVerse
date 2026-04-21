package com.novelverse.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.novelverse.app.data.local.entities.UserLevelEntity;

import java.util.List;

@Dao
public interface UserLevelDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(UserLevelEntity entity);

    @Update
    void update(UserLevelEntity entity);

    @Query("SELECT * FROM user_levels WHERE user_id = :userId")
    UserLevelEntity get(String userId);

    @Query("SELECT * FROM user_levels WHERE user_id = :userId")
    LiveData<UserLevelEntity> getLive(String userId);

    @Query("UPDATE user_levels SET xp_total = :xpTotal, current_level = :level, xp_in_level = :xpInLevel, xp_level_target = :target, badge_slots = :slots, needs_sync = 1 WHERE user_id = :userId")
    void updateProgress(String userId, long xpTotal, int level, int xpInLevel, int target, int slots);

    @Query("UPDATE user_levels SET equipped_badge_ids = :badgeIds, needs_sync = 1 WHERE user_id = :userId")
    void updateEquippedBadges(String userId, String badgeIds);

    @Query("SELECT * FROM user_levels WHERE needs_sync = 1")
    List<UserLevelEntity> getPendingSync();

    @Query("UPDATE user_levels SET needs_sync = 0 WHERE user_id = :userId")
    void markSynced(String userId);
}
