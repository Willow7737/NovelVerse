package com.novelverse.app.domain.gamification;

import com.novelverse.app.data.local.entities.UserLevelEntity;

/**
 * Pure XP/level calculation engine — no Android dependencies, fully testable.
 *
 * Level thresholds use a progressive curve:
 *   Level N requires N * 100 XP to complete (so L1=100, L2=200 … L10=1000 etc.)
 *   This means total XP to reach level N = sum(k*100 for k=1..N-1)
 *
 * Badge slots:
 *   - Default: 1 slot
 *   - Level 10+: 2 slots
 *   - Level 25+: 3 slots
 *
 * Level perks fired via LevelUpResult so callers can react:
 *   - Level 10: 2nd badge slot unlocked
 *   - Level 25: 3rd badge slot + animated avatars
 *   - Level 50: "Veteran" title + exclusive theme
 */
public final class XpLevelEngine {

    // XP per source (tunable)
    public static final int XP_CHAPTER_READ        = 10;
    public static final int XP_NOVEL_COMPLETED     = 50;
    public static final int XP_REVIEW_WRITTEN       = 15;
    public static final int XP_DAILY_STREAK         = 5;
    public static final int XP_CHAPTER_PUBLISHED    = 30;
    public static final int XP_ACHIEVEMENT_COMMON   = 50;
    public static final int XP_ACHIEVEMENT_RARE     = 200;
    public static final int XP_ACHIEVEMENT_EPIC     = 600;
    public static final int XP_ACHIEVEMENT_LEGENDARY = 2000;

    private XpLevelEngine() {}

    /** Total XP needed to start level N (1-indexed). Level 1 starts at 0. */
    public static long xpFloorForLevel(int level) {
        if (level <= 1) return 0;
        long total = 0;
        for (int i = 1; i < level; i++) total += i * 100L;
        return total;
    }

    /** XP needed to complete level N */
    public static int xpToCompleteLevel(int level) {
        return level * 100;
    }

    /** Compute updated UserLevelEntity after adding deltaXp. Returns null if no change. */
    public static LevelUpResult addXp(UserLevelEntity current, int deltaXp, long clock) {
        if (deltaXp <= 0) return null;

        long newTotal = current.getXpTotal() + deltaXp;
        int newLevel  = levelForXp(newTotal);
        int newXpIn   = (int)(newTotal - xpFloorForLevel(newLevel));
        int newTarget = xpToCompleteLevel(newLevel);
        int newSlots  = badgeSlotsForLevel(newLevel);

        boolean leveledUp = newLevel > current.getCurrentLevel();

        // Build updated entity
        UserLevelEntity updated = new UserLevelEntity();
        updated.setUserId(current.getUserId());
        updated.setXpTotal(newTotal);
        updated.setCurrentLevel(newLevel);
        updated.setXpInLevel(newXpIn);
        updated.setXpLevelTarget(newTarget);
        updated.setBadgeSlots(newSlots);
        updated.setEquippedBadgeIds(current.getEquippedBadgeIds());
        updated.setNeedsSync(true);

        return new LevelUpResult(current.getCurrentLevel(), newLevel, newSlots, leveledUp, updated);
    }

    /** Derive level from total XP. O(log N) binary-search friendly but N is small (<200). */
    public static int levelForXp(long totalXp) {
        int level = 1;
        while (xpFloorForLevel(level + 1) <= totalXp) level++;
        return level;
    }

    public static int badgeSlotsForLevel(int level) {
        if (level >= 25) return 3;
        if (level >= 10) return 2;
        return 1;
    }

    public static boolean hasAnimatedAvatars(int level)    { return level >= 25; }
    public static boolean isVeteran(int level)             { return level >= 50; }

    /** XP reward from an achievement rarity string */
    public static int xpForRarity(String rarity) {
        if (rarity == null) return XP_ACHIEVEMENT_COMMON;
        switch (rarity) {
            case "RARE":      return XP_ACHIEVEMENT_RARE;
            case "EPIC":      return XP_ACHIEVEMENT_EPIC;
            case "LEGENDARY": return XP_ACHIEVEMENT_LEGENDARY;
            default:          return XP_ACHIEVEMENT_COMMON;
        }
    }

    // ── Result ────────────────────────────────────────────────────────────────

    public static final class LevelUpResult {
        public final int oldLevel;
        public final int newLevel;
        public final int badgeSlots;
        public final boolean didLevelUp;
        public final UserLevelEntity updated;

        LevelUpResult(int oldLevel, int newLevel, int badgeSlots, boolean didLevelUp, UserLevelEntity updated) {
            this.oldLevel   = oldLevel;
            this.newLevel   = newLevel;
            this.badgeSlots = badgeSlots;
            this.didLevelUp = didLevelUp;
            this.updated    = updated;
        }
    }
}
