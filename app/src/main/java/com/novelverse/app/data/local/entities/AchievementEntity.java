package com.novelverse.app.data.local.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Catalog row for a single achievement definition.
 * Seeded once at DB creation; never modified at runtime.
 */
@Entity(tableName = "achievements")
public class AchievementEntity {

    public enum Rarity { COMMON, RARE, EPIC, LEGENDARY }
    public enum Category { READER, WRITER, STREAK, SOCIAL, SUPPORTER }

    @PrimaryKey
    @ColumnInfo(name = "id")
    private String id;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "description")
    private String description;

    /** Asset filename inside assets/gamification_icons/ (e.g. "achievement_bookworm.png") */
    @ColumnInfo(name = "asset_name")
    private String assetName;

    @ColumnInfo(name = "rarity")
    private String rarity; // Rarity.name()

    @ColumnInfo(name = "category")
    private String category; // Category.name()

    /** XP rewarded on unlock */
    @ColumnInfo(name = "xp_reward")
    private int xpReward;

    /** Ink (soft currency) rewarded on unlock */
    @ColumnInfo(name = "ink_reward")
    private int inkReward;

    /** Quill (hard currency) rewarded on unlock */
    @ColumnInfo(name = "quill_reward")
    private int quillReward;

    /**
     * true = progress bar shown (e.g. 9/10 novels).
     * false = hidden surprise achievement.
     */
    @ColumnInfo(name = "is_visible")
    private boolean isVisible;

    /** Threshold value for progress-bar achievements (e.g. 10 for Bookworm) */
    @ColumnInfo(name = "target_value")
    private int targetValue;

    public AchievementEntity() {}

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getId()          { return id; }
    public String getTitle()       { return title; }
    public String getDescription() { return description; }
    public String getAssetName()   { return assetName; }
    public String getRarity()      { return rarity; }
    public String getCategory()    { return category; }
    public int    getXpReward()    { return xpReward; }
    public int    getInkReward()   { return inkReward; }
    public int    getQuillReward() { return quillReward; }
    public boolean isVisible()     { return isVisible; }
    public int    getTargetValue() { return targetValue; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setId(String id)                 { this.id = id; }
    public void setTitle(String title)           { this.title = title; }
    public void setDescription(String d)         { this.description = d; }
    public void setAssetName(String a)           { this.assetName = a; }
    public void setRarity(String r)              { this.rarity = r; }
    public void setCategory(String c)            { this.category = c; }
    public void setXpReward(int xp)              { this.xpReward = xp; }
    public void setInkReward(int ink)            { this.inkReward = ink; }
    public void setQuillReward(int q)            { this.quillReward = q; }
    public void setIsVisible(boolean v)          { this.isVisible = v; }
    public void setTargetValue(int t)            { this.targetValue = t; }

    // ── Builder helper ────────────────────────────────────────────────────────

    public static Builder builder(String id) { return new Builder(id); }

    public static final class Builder {
        private final AchievementEntity e = new AchievementEntity();
        Builder(String id) { e.id = id; }
        public Builder title(String v)       { e.title = v; return this; }
        public Builder desc(String v)        { e.description = v; return this; }
        public Builder asset(String v)       { e.assetName = v; return this; }
        public Builder rarity(Rarity v)      { e.rarity = v.name(); return this; }
        public Builder category(Category v)  { e.category = v.name(); return this; }
        public Builder xp(int v)             { e.xpReward = v; return this; }
        public Builder ink(int v)            { e.inkReward = v; return this; }
        public Builder quill(int v)          { e.quillReward = v; return this; }
        public Builder visible(boolean v)    { e.isVisible = v; return this; }
        public Builder target(int v)         { e.targetValue = v; return this; }
        public AchievementEntity build()     { return e; }
    }
}
