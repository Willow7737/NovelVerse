package com.novelverse.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.novelverse.app.data.local.entities.AchievementEntity;

import java.util.List;

@Dao
public interface AchievementDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<AchievementEntity> achievements);

    @Query("SELECT * FROM achievements ORDER BY category, rarity DESC")
    LiveData<List<AchievementEntity>> getAllLive();

    @Query("SELECT * FROM achievements ORDER BY category, rarity DESC")
    List<AchievementEntity> getAll();

    @Query("SELECT * FROM achievements WHERE id = :id")
    AchievementEntity getById(String id);

    @Query("SELECT COUNT(*) FROM achievements")
    int count();
}
