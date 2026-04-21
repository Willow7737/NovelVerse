package com.novelverse.app.data.local.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * XP / level state for a single user. One row per user.
 */
@Entity(
    tableName = "user_levels",
    indices = { @Index(value = "user_id", unique = true) }
)
public class UserLevelEntity {

    @PrimaryKey
    @ColumnInfo(name = "user_id")
    private String userId;

    @ColumnInfo(name = "xp_total")
    private long xpTotal;

    @ColumnInfo(name = "current_level")
    private int currentLevel;

    /** XP within the current level bucket */
    @ColumnInfo(name = "xp_in_level")
    private int xpInLevel;

    /** XP needed to complete this level */
    @ColumnInfo(name = "xp_level_target")
    private int xpLevelTarget;

    /** Badge slots unlocked: 1 by default, 2 at L10, 3 at L25 */
    @ColumnInfo(name = "badge_slots")
    private int badgeSlots;

    /** IDs of chosen badges (comma-separated, max badgeSlots) */
    @ColumnInfo(name = "equipped_badge_ids")
    private String equippedBadgeIds;

    @ColumnInfo(name = "needs_sync")
    private boolean needsSync;

    public UserLevelEntity() { this.badgeSlots = 1; this.currentLevel = 1; }

    public String getUserId()           { return userId; }
    public long   getXpTotal()          { return xpTotal; }
    public int    getCurrentLevel()     { return currentLevel; }
    public int    getXpInLevel()        { return xpInLevel; }
    public int    getXpLevelTarget()    { return xpLevelTarget; }
    public int    getBadgeSlots()       { return badgeSlots; }
    public String getEquippedBadgeIds() { return equippedBadgeIds; }
    public boolean isNeedsSync()        { return needsSync; }

    public void setUserId(String userId)                { this.userId = userId; }
    public void setXpTotal(long xpTotal)                { this.xpTotal = xpTotal; }
    public void setCurrentLevel(int currentLevel)       { this.currentLevel = currentLevel; }
    public void setXpInLevel(int xpInLevel)             { this.xpInLevel = xpInLevel; }
    public void setXpLevelTarget(int xpLevelTarget)     { this.xpLevelTarget = xpLevelTarget; }
    public void setBadgeSlots(int badgeSlots)           { this.badgeSlots = badgeSlots; }
    public void setEquippedBadgeIds(String v)           { this.equippedBadgeIds = v; }
    public void setNeedsSync(boolean needsSync)         { this.needsSync = needsSync; }
}
