package com.novelverse.app.domain.gamification;

import com.novelverse.app.data.local.entities.AchievementEntity;
import com.novelverse.app.data.local.entities.AchievementEntity.Category;
import com.novelverse.app.data.local.entities.AchievementEntity.Rarity;

import java.util.Arrays;
import java.util.List;

/**
 * Single source of truth for all 28 achievement definitions.
 * These are seeded into the local DB on first run and checked against the catalog.
 *
 * Asset filenames map to: assets/gamification_icons/<assetName>
 * Fallback rarity colors: COMMON=gray, RARE=cyan, EPIC=purple, LEGENDARY=gold
 */
public final class AchievementDefinitions {

    private AchievementDefinitions() {}

    public static List<AchievementEntity> all() {
        return Arrays.asList(

            // ── READER (8) ───────────────────────────────────────────────────

            AchievementEntity.builder("first_page")
                .title("First Page")
                .desc("Read your first chapter")
                .asset("achievement_first_page.png")
                .rarity(Rarity.COMMON)
                .category(Category.READER)
                .xp(50).ink(20).quill(0)
                .visible(false)         // surprise
                .target(1)
                .build(),

            AchievementEntity.builder("bookworm")
                .title("Bookworm")
                .desc("Read 10 novels")
                .asset("achievement_bookworm.png")
                .rarity(Rarity.RARE)
                .category(Category.READER)
                .xp(200).ink(100).quill(0)
                .visible(true).target(10)
                .build(),

            AchievementEntity.builder("scholar")
                .title("Scholar")
                .desc("Read 25 novels")
                .asset("achievement_scholar.png")
                .rarity(Rarity.RARE)
                .category(Category.READER)
                .xp(400).ink(200).quill(0)
                .visible(true).target(25)
                .build(),

            AchievementEntity.builder("sage")
                .title("Sage")
                .desc("Read 50 novels")
                .asset("achievement_sage.png")
                .rarity(Rarity.EPIC)
                .category(Category.READER)
                .xp(750).ink(350).quill(5)
                .visible(true).target(50)
                .build(),

            AchievementEntity.builder("lorekeeper")
                .title("Lorekeeper")
                .desc("Read 100 novels")
                .asset("achievement_lorekeeper.png")
                .rarity(Rarity.LEGENDARY)
                .category(Category.READER)
                .xp(2000).ink(500).quill(20)
                .visible(true).target(100)
                .build(),

            AchievementEntity.builder("completionist")
                .title("Completionist")
                .desc("Finish a novel (read all chapters)")
                .asset("achievement_completionist.png")
                .rarity(Rarity.RARE)
                .category(Category.READER)
                .xp(300).ink(150).quill(0)
                .visible(false)         // surprise
                .target(1)
                .build(),

            AchievementEntity.builder("finished_strong")
                .title("Finished Strong")
                .desc("Complete 5 novels")
                .asset("achievement_finished_strong.png")
                .rarity(Rarity.EPIC)
                .category(Category.READER)
                .xp(600).ink(300).quill(5)
                .visible(true).target(5)
                .build(),

            AchievementEntity.builder("night_reader")
                .title("Night Reader")
                .desc("Read between midnight and 4 AM")
                .asset("achievement_night_reader.png")
                .rarity(Rarity.COMMON)
                .category(Category.READER)
                .xp(75).ink(30).quill(0)
                .visible(false)         // surprise
                .target(1)
                .build(),

            // ── WRITER (6) ───────────────────────────────────────────────────

            AchievementEntity.builder("first_words")
                .title("First Words")
                .desc("Publish your first chapter")
                .asset("achievement_first_words.png")
                .rarity(Rarity.COMMON)
                .category(Category.WRITER)
                .xp(100).ink(50).quill(0)
                .visible(false)         // surprise
                .target(1)
                .build(),

            AchievementEntity.builder("published")
                .title("Published")
                .desc("Publish a chapter with 1000+ words and 5 reads")
                .asset("achievement_published.png")
                .rarity(Rarity.RARE)
                .category(Category.WRITER)
                .xp(250).ink(100).quill(0)
                .visible(true).target(1)
                .build(),

            AchievementEntity.builder("rising_star")
                .title("Rising Star")
                .desc("Reach 100 total reads across your novels")
                .asset("achievement_rising_star.png")
                .rarity(Rarity.RARE)
                .category(Category.WRITER)
                .xp(350).ink(150).quill(0)
                .visible(true).target(100)
                .build(),

            AchievementEntity.builder("going_viral")
                .title("Going Viral")
                .desc("Reach 1,000 total reads")
                .asset("achievement_going_viral.png")
                .rarity(Rarity.EPIC)
                .category(Category.WRITER)
                .xp(800).ink(300).quill(10)
                .visible(true).target(1000)
                .build(),

            AchievementEntity.builder("bestseller")
                .title("Bestseller")
                .desc("Reach 10,000 total reads")
                .asset("achievement_bestseller.png")
                .rarity(Rarity.LEGENDARY)
                .category(Category.WRITER)
                .xp(3000).ink(500).quill(50)
                .visible(true).target(10000)
                .build(),

            AchievementEntity.builder("prolific")
                .title("Prolific")
                .desc("Publish 10 chapters")
                .asset("achievement_prolific.png")
                .rarity(Rarity.RARE)
                .category(Category.WRITER)
                .xp(400).ink(200).quill(0)
                .visible(true).target(10)
                .build(),

            // ── STREAK (4) ───────────────────────────────────────────────────

            AchievementEntity.builder("streak_3")
                .title("On a Roll")
                .desc("Read 3 days in a row")
                .asset("achievement_streak_3.png")
                .rarity(Rarity.COMMON)
                .category(Category.STREAK)
                .xp(75).ink(30).quill(0)
                .visible(true).target(3)
                .build(),

            AchievementEntity.builder("streak_7")
                .title("Week Warrior")
                .desc("Read 7 days in a row")
                .asset("achievement_streak_7.png")
                .rarity(Rarity.RARE)
                .category(Category.STREAK)
                .xp(200).ink(100).quill(0)
                .visible(true).target(7)
                .build(),

            AchievementEntity.builder("streak_30")
                .title("Monthly Devotee")
                .desc("Read 30 days in a row")
                .asset("achievement_streak_30.png")
                .rarity(Rarity.EPIC)
                .category(Category.STREAK)
                .xp(750).ink(300).quill(5)
                .visible(true).target(30)
                .build(),

            AchievementEntity.builder("streak_100")
                .title("Century Reader")
                .desc("Read 100 days in a row")
                .asset("achievement_streak_100.png")
                .rarity(Rarity.LEGENDARY)
                .category(Category.STREAK)
                .xp(5000).ink(500).quill(100)
                .visible(true).target(100)
                .build(),

            // ── SOCIAL (5) ───────────────────────────────────────────────────

            AchievementEntity.builder("bookmarked")
                .title("Bookmarked")
                .desc("Add your first bookmark")
                .asset("achievement_bookmarked.png")
                .rarity(Rarity.COMMON)
                .category(Category.SOCIAL)
                .xp(25).ink(10).quill(0)
                .visible(false)         // surprise
                .target(1)
                .build(),

            AchievementEntity.builder("critic")
                .title("Critic")
                .desc("Write 10 reviews (50+ chars each)")
                .asset("achievement_critic.png")
                .rarity(Rarity.RARE)
                .category(Category.SOCIAL)
                .xp(250).ink(100).quill(0)
                .visible(true).target(10)
                .build(),

            AchievementEntity.builder("trusted_voice")
                .title("Trusted Voice")
                .desc("Have 25 reviews liked by others")
                .asset("achievement_trusted_voice.png")
                .rarity(Rarity.EPIC)
                .category(Category.SOCIAL)
                .xp(600).ink(250).quill(5)
                .visible(true).target(25)
                .build(),

            AchievementEntity.builder("connected")
                .title("Connected")
                .desc("Follow 10 authors")
                .asset("achievement_connected.png")
                .rarity(Rarity.COMMON)
                .category(Category.SOCIAL)
                .xp(100).ink(50).quill(0)
                .visible(true).target(10)
                .build(),

            AchievementEntity.builder("trendsetter")
                .title("Trendsetter")
                .desc("Have 50 followers")
                .asset("achievement_trendsetter.png")
                .rarity(Rarity.EPIC)
                .category(Category.SOCIAL)
                .xp(800).ink(300).quill(10)
                .visible(true).target(50)
                .build(),

            // ── SUPPORTER (3) ────────────────────────────────────────────────

            AchievementEntity.builder("supporter_bronze")
                .title("Bronze Supporter")
                .desc("Subscribe to NovelVerse Pro")
                .asset("supporter_bronze_heart.png")
                .rarity(Rarity.RARE)
                .category(Category.SUPPORTER)
                .xp(500).ink(200).quill(10)
                .visible(false)         // surprise
                .target(1)
                .build(),

            AchievementEntity.builder("supporter_silver")
                .title("Silver Supporter")
                .desc("Stay subscribed for 3 months")
                .asset("supporter_silver_heart.png")
                .rarity(Rarity.EPIC)
                .category(Category.SUPPORTER)
                .xp(1000).ink(300).quill(20)
                .visible(true).target(3)
                .build(),

            AchievementEntity.builder("supporter_gold")
                .title("Gold Supporter")
                .desc("Stay subscribed for 12 months")
                .asset("supporter_gold_heart.png")
                .rarity(Rarity.LEGENDARY)
                .category(Category.SUPPORTER)
                .xp(5000).ink(500).quill(100)
                .visible(true).target(12)
                .build()
        );
    }

    // ── Lookup helpers ────────────────────────────────────────────────────────

    /** Rarity fallback color (hex string) when PNG asset is unavailable */
    public static String rarityColor(String rarity) {
        if (rarity == null) return "#9E9E9E";
        switch (rarity) {
            case "COMMON":    return "#9E9E9E"; // gray
            case "RARE":      return "#00BCD4"; // cyan
            case "EPIC":      return "#9C27B0"; // purple
            case "LEGENDARY": return "#FFD700"; // gold
            default:          return "#9E9E9E";
        }
    }

    /** Card background asset name for a given rarity */
    public static String cardBgAsset(String rarity) {
        if (rarity == null) return "card_bg_common.png";
        switch (rarity) {
            case "RARE":      return "card_bg_rare.png";
            case "EPIC":      return "card_bg_epic.png";
            case "LEGENDARY": return "card_bg_legendary.png";
            default:          return "card_bg_common.png";
        }
    }
}
