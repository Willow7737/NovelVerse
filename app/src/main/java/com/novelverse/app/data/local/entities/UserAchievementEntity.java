package com.novelverse.app.data.local.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "user_achievements",
    foreignKeys = {
        @ForeignKey(
            entity = AchievementEntity.class,
            parentColumns = "id",
            childColumns = "achievement_id",
            onDelete = ForeignKey.CASCADE
        )
    },
    indices = {
        @Index(value = {"user_id", "achievement_id"}, unique = true),
        @Index(value = "achievement_id"),
        @Index(value = "user_id")
    }
)
public class UserAchievementEntity {

    @PrimaryKey
    @ColumnInfo(name = "id")
    private String id;

    @ColumnInfo(name = "user_id")
    private String userId;

    @ColumnInfo(name = "achievement_id")
    private String achievementId;

    @ColumnInfo(name = "is_unlocked")
    private boolean isUnlocked;

    @ColumnInfo(name = "unlocked_at")
    private long unlockedAt;

    @ColumnInfo(name = "current_progress")
    private int currentProgress;

    @ColumnInfo(name = "reward_claimed")
    private boolean rewardClaimed;

    @ColumnInfo(name = "needs_sync")
    private boolean needsSync;

    public UserAchievementEntity() {}

    public String  getId()              { return id; }
    public String  getUserId()          { return userId; }
    public String  getAchievementId()   { return achievementId; }
    public boolean isUnlocked()         { return isUnlocked; }
    public long    getUnlockedAt()      { return unlockedAt; }
    public int     getCurrentProgress() { return currentProgress; }
    public int     getProgress()        { return currentProgress; }
    public boolean isRewardClaimed()    { return rewardClaimed; }
    public boolean isNeedsSync()        { return needsSync; }

    public void setId(String id)                           { this.id = id; }
    public void setUserId(String userId)                   { this.userId = userId; }
    public void setAchievementId(String achievementId)     { this.achievementId = achievementId; }
    public void setIsUnlocked(boolean isUnlocked)          { this.isUnlocked = isUnlocked; }
    public void setUnlockedAt(long unlockedAt)             { this.unlockedAt = unlockedAt; }
    public void setCurrentProgress(int currentProgress)    { this.currentProgress = currentProgress; }
    public void setProgress(int progress)                  { this.currentProgress = progress; }
    public void setRewardClaimed(boolean rewardClaimed)    { this.rewardClaimed = rewardClaimed; }
    public void setNeedsSync(boolean needsSync)            { this.needsSync = needsSync; }
}
