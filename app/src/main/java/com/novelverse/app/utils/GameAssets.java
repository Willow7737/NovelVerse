package com.novelverse.app.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Single source of truth for all Supabase game-assets bucket URLs.
 *
 * Bucket root: https://ztfvifgtebptxfoasrae.supabase.co/storage/v1/object/public/game-assets/
 *
 * IMPORTANT: Achievement filenames carry numeric prefixes (03_, 04_…) that do NOT
 * derive from the achievement ID. All mappings are hardcoded here. Never guess.
 *
 * Themes are .jpg — everything else is .png.
 */
public final class GameAssets {

    private GameAssets() {}

    public static final String BASE_URL =
        "https://ztfvifgtebptxfoasrae.supabase.co/storage/v1/object/public/game-assets/";

    // ── Folder constants ─────────────────────────────────────────────────────
    public static final class Folders {
        public static final String CURRENCIES    = "currencies/";
        public static final String ACHIEVEMENTS  = "achievements/";
        public static final String LEVELS        = "levels/";
        public static final String THEMES        = "themes/";
        public static final String FRAMES        = "frames/";
        public static final String MISC          = "misc/";
        public static final String STORE         = "store/";
        public static final String SUBSCRIPTIONS = "subscriptions/";
        public static final String NOTIFICATIONS = "notifications/";
        public static final String STATUS        = "status/";
        public static final String EVENTS        = "events/";
        public static final String PLACEHOLDERS  = "placeholders/";
    }

    // ── Currencies ───────────────────────────────────────────────────────────
    public static final String CURRENCY_INK   = BASE_URL + Folders.CURRENCIES + "01_ink_bottle.png";
    public static final String CURRENCY_QUILL = BASE_URL + Folders.CURRENCIES + "02_quill.png";

    // ── Misc UI ──────────────────────────────────────────────────────────────
    public static final String UI_STREAK_FLAME      = BASE_URL + Folders.MISC + "31_ui_flame_streak.png";
    public static final String UI_FREEZE            = BASE_URL + Folders.MISC + "32_ui_snowflake_freeze.png";
    public static final String UI_SHIELD            = BASE_URL + Folders.MISC + "33_ui_shield_recovery.png";
    public static final String UI_CHECKIN           = BASE_URL + Folders.MISC + "34_ui_calendar_checkin.png";
    public static final String UI_LEVEL_BADGE_FRAME = BASE_URL + Folders.MISC + "35_ui_level_badge_frame.png";
    public static final String UI_XP_STAR           = BASE_URL + Folders.MISC + "36_ui_xp_star.png";
    public static final String UI_LEVELUP_BG        = BASE_URL + Folders.MISC + "37_ui_levelup_celebration.png";
    public static final String UI_TOAST_BG          = BASE_URL + Folders.MISC + "42_toast_notification.png";

    // ── Placeholders ─────────────────────────────────────────────────────────
    public static final String PLACEHOLDER_LOCKED         = BASE_URL + Folders.PLACEHOLDERS + "61_placeholder_locked.png";
    public static final String PLACEHOLDER_EMPTY_CURRENCY = BASE_URL + Folders.PLACEHOLDERS + "62_empty_no_currency.png";
    public static final String PLACEHOLDER_LOCKED_THEME   = BASE_URL + Folders.PLACEHOLDERS + "63_locked_theme_preview.png";
    public static final String PLACEHOLDER_EMPTY_ACHIEVE  = BASE_URL + Folders.PLACEHOLDERS + "115_empty_achievements.png";
    public static final String PLACEHOLDER_EMPTY_STREAK   = BASE_URL + Folders.PLACEHOLDERS + "116_empty_streak.png";

    // ── Avatar frames ─────────────────────────────────────────────────────────
    public static final String FRAME_READER = BASE_URL + Folders.FRAMES + "49_frame_reader.png";
    public static final String FRAME_WRITER = BASE_URL + Folders.FRAMES + "50_frame_writer.png";
    public static final String FRAME_BRONZE = BASE_URL + Folders.FRAMES + "51_frame_bronze.png";
    public static final String FRAME_SILVER = BASE_URL + Folders.FRAMES + "52_frame_silver.png";
    public static final String FRAME_GOLD   = BASE_URL + Folders.FRAMES + "53_frame_gold.png";
    public static final String FRAME_STREAK = BASE_URL + Folders.FRAMES + "54_frame_flame_streak.png";
    public static final String FRAME_LEGEND = BASE_URL + Folders.FRAMES + "55_frame_legendary.png";

    // ── Store ─────────────────────────────────────────────────────────────────
    public static final String STORE_INK_SMALL    = BASE_URL + Folders.STORE + "56_store_ink_small.png";
    public static final String STORE_INK_LARGE    = BASE_URL + Folders.STORE + "57_store_ink_large.png";
    public static final String STORE_QUILL        = BASE_URL + Folders.STORE + "58_store_quill.png";
    public static final String STORE_BUNDLE_START = BASE_URL + Folders.STORE + "101_bundle_starter.png";
    public static final String STORE_BUNDLE_WHALE = BASE_URL + Folders.STORE + "102_bundle_whale.png";
    public static final String STORE_XP_BOOST     = BASE_URL + Folders.STORE + "103_item_xp_boost.png";
    public static final String STORE_BADGE_SLOT   = BASE_URL + Folders.STORE + "104_item_badge_slot.png";
    public static final String STORE_GIFT_CARD    = BASE_URL + Folders.STORE + "105_gift_card.png";

    // ── Subscriptions ─────────────────────────────────────────────────────────
    public static final String SUB_PRO     = BASE_URL + Folders.SUBSCRIPTIONS + "59_sub_pro.png";
    public static final String SUB_PROPLUS = BASE_URL + Folders.SUBSCRIPTIONS + "60_sub_proplus.png";

    // ── Notification icons ────────────────────────────────────────────────────
    public static final String NOTIF_ACHIEVEMENT = BASE_URL + Folders.NOTIFICATIONS + "95_notif_achievement.png";
    public static final String NOTIF_STREAK      = BASE_URL + Folders.NOTIFICATIONS + "96_notif_streak.png";
    public static final String NOTIF_TIP         = BASE_URL + Folders.NOTIFICATIONS + "97_notif_tip.png";
    public static final String NOTIF_FOLLOW      = BASE_URL + Folders.NOTIFICATIONS + "98_notif_follow.png";
    public static final String NOTIF_CHAPTER     = BASE_URL + Folders.NOTIFICATIONS + "99_notif_chapter.png";
    public static final String NOTIF_SYSTEM      = BASE_URL + Folders.NOTIFICATIONS + "100_notif_system.png";

    // ── Status badges ─────────────────────────────────────────────────────────
    public static final String STATUS_BETA_TESTER   = BASE_URL + Folders.STATUS + "109_achievement_beta_tester.png";
    public static final String STATUS_VERIFIED      = BASE_URL + Folders.STATUS + "110_status_verified.png";
    public static final String STATUS_MODERATOR     = BASE_URL + Folders.STATUS + "111_status_moderator.png";
    public static final String STATUS_AUTHOR_PRO    = BASE_URL + Folders.STATUS + "112_status_author_pro.png";
    public static final String STATUS_READER_MASTER = BASE_URL + Folders.STATUS + "113_set_reader_complete.png";
    public static final String STATUS_WRITER_MASTER = BASE_URL + Folders.STATUS + "114_set_writer_complete.png";

    // ── Event badges ──────────────────────────────────────────────────────────
    public static final String EVENT_HALLOWEEN   = BASE_URL + Folders.EVENTS + "106_event_halloween_2026.png";
    public static final String EVENT_WINTER      = BASE_URL + Folders.EVENTS + "107_event_winter_2026.png";
    public static final String EVENT_ANNIVERSARY = BASE_URL + Folders.EVENTS + "108_event_anniversary.png";

    // ═════════════════════════════════════════════════════════════════════════
    // ACHIEVEMENT URL — hardcoded map (numeric prefix varies; never derive blindly)
    // ═════════════════════════════════════════════════════════════════════════
    private static final Map<String, String> ACHIEVEMENT_FILES = new HashMap<>(64);
    static {
        // READER
        ACHIEVEMENT_FILES.put("first_page",           "03_achievement_first_page.png");
        ACHIEVEMENT_FILES.put("bookworm",             "04_achievement_bookworm.png");
        ACHIEVEMENT_FILES.put("scholar",              "05_achievement_scholar.png");
        ACHIEVEMENT_FILES.put("sage",                 "06_achievement_sage.png");
        ACHIEVEMENT_FILES.put("lorekeeper",           "07_achievement_lorekeeper.png");
        ACHIEVEMENT_FILES.put("completionist",        "08_achievement_completionist.png");
        ACHIEVEMENT_FILES.put("finished_strong",      "09_achievement_finished_strong.png");
        ACHIEVEMENT_FILES.put("night_reader",         "10_achievement_night_reader.png");
        ACHIEVEMENT_FILES.put("speed_reader",         "11_achievement_speed_reader.png");
        ACHIEVEMENT_FILES.put("deep_dive",            "12_achievement_deep_dive.png");
        // WRITER
        ACHIEVEMENT_FILES.put("first_words",          "13_achievement_first_words.png");
        ACHIEVEMENT_FILES.put("published",            "14_achievement_published.png");
        ACHIEVEMENT_FILES.put("rising_star",          "15_achievement_rising_star.png");
        ACHIEVEMENT_FILES.put("going_viral",          "16_achievement_going_viral.png");
        ACHIEVEMENT_FILES.put("bestseller",           "17_achievement_bestseller.png");
        ACHIEVEMENT_FILES.put("prolific",             "18_achievement_prolific.png");
        ACHIEVEMENT_FILES.put("chapter_50",           "79_achievement_chapter_50.png");
        ACHIEVEMENT_FILES.put("novels_3",             "80_achievement_novels_3.png");
        ACHIEVEMENT_FILES.put("words_100k",           "81_achievement_words_100k.png");
        ACHIEVEMENT_FILES.put("words_1m",             "82_achievement_words_1m.png");
        ACHIEVEMENT_FILES.put("followers_100",        "83_achievement_followers_100.png");
        ACHIEVEMENT_FILES.put("followers_1k",         "84_achievement_followers_1k.png");
        ACHIEVEMENT_FILES.put("tipped",               "85_achievement_tipped.png");
        ACHIEVEMENT_FILES.put("earnings_100",         "86_achievement_earnings_100.png");
        // STREAK
        ACHIEVEMENT_FILES.put("streak_3",             "19_achievement_streak_3.png");
        ACHIEVEMENT_FILES.put("streak_7",             "20_achievement_streak_7.png");
        ACHIEVEMENT_FILES.put("streak_14",            "75_achievement_streak_14.png");
        ACHIEVEMENT_FILES.put("streak_30",            "21_achievement_streak_30.png");
        ACHIEVEMENT_FILES.put("streak_60",            "76_achievement_streak_60.png");
        ACHIEVEMENT_FILES.put("streak_100",           "22_achievement_streak_100.png");
        ACHIEVEMENT_FILES.put("streak_180",           "77_achievement_streak_180.png");
        ACHIEVEMENT_FILES.put("streak_365",           "78_achievement_streak_365.png");
        // SOCIAL
        ACHIEVEMENT_FILES.put("bookmarked",           "23_achievement_bookmarked.png");
        ACHIEVEMENT_FILES.put("critic",               "24_achievement_critic.png");
        ACHIEVEMENT_FILES.put("trusted_voice",        "25_achievement_trusted_voice.png");
        ACHIEVEMENT_FILES.put("connected",            "26_achievement_connected.png");
        ACHIEVEMENT_FILES.put("trendsetter",          "27_achievement_trendsetter.png");
        ACHIEVEMENT_FILES.put("comments_50",          "87_achievement_comments_50.png");
        ACHIEVEMENT_FILES.put("replies_100",          "88_achievement_replies_100.png");
        ACHIEVEMENT_FILES.put("liked_500",            "89_achievement_liked_500.png");
        // GENRE READER
        ACHIEVEMENT_FILES.put("genre_fantasy_master", "90_genre_fantasy_master.png");
        ACHIEVEMENT_FILES.put("genre_romance_master", "91_genre_romance_master.png");
        ACHIEVEMENT_FILES.put("genre_horror_master",  "92_genre_horror_master.png");
        ACHIEVEMENT_FILES.put("genre_scifi_master",   "93_genre_scifi_master.png");
        ACHIEVEMENT_FILES.put("genre_mystery_master", "94_genre_mystery_master.png");
        // SUPPORTER (no "achievement_" prefix in their filenames)
        ACHIEVEMENT_FILES.put("supporter_bronze",     "28_supporter_bronze.png");
        ACHIEVEMENT_FILES.put("supporter_silver",     "29_supporter_silver.png");
        ACHIEVEMENT_FILES.put("supporter_gold",       "30_supporter_gold.png");
    }

    /**
     * Returns the full CDN URL for an achievement badge PNG.
     * Falls back to the locked placeholder if the ID is unknown.
     *
     * @param achievementId e.g. "bookworm", "streak_30", "supporter_gold"
     */
    public static String getAchievementUrl(String achievementId) {
        if (achievementId == null) return PLACEHOLDER_LOCKED;
        String filename = ACHIEVEMENT_FILES.get(achievementId);
        return (filename != null)
            ? BASE_URL + Folders.ACHIEVEMENTS + filename
            : PLACEHOLDER_LOCKED;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // LEVEL BADGE
    // Files:  64_level_1.png (L1-9), 65_level_10.png (L10-19) … 74_level_100.png
    // ═════════════════════════════════════════════════════════════════════════

    // [minLevel, fileNumber, tierSuffix]
    private static final int[][] LEVEL_TIERS = {
        {100, 74, 100},
        { 90, 73,  90},
        { 80, 72,  80},
        { 70, 71,  70},
        { 60, 70,  60},
        { 50, 69,  50},
        { 40, 68,  40},
        { 30, 67,  30},
        { 20, 66,  20},
        { 10, 65,  10},
        {  1, 64,   1},
    };

    /**
     * Returns the full CDN URL for the level badge image for the given level.
     *
     * @param level user's current level (1 – ∞)
     */
    public static String getLevelBadgeUrl(int level) {
        for (int[] tier : LEVEL_TIERS) {
            if (level >= tier[0]) {
                return BASE_URL + Folders.LEVELS + tier[1] + "_level_" + tier[2] + ".png";
            }
        }
        return BASE_URL + Folders.LEVELS + "64_level_1.png";
    }

    /**
     * Returns the display tier name for a given level number.
     *
     * @param level user's current level
     */
    public static String getLevelTierName(int level) {
        if (level >= 100) return "Legendary Sage";
        if (level >= 90)  return "Amethyst Reader";
        if (level >= 80)  return "Sapphire Reader";
        if (level >= 70)  return "Ruby Reader";
        if (level >= 60)  return "Emerald Reader";
        if (level >= 50)  return "Diamond Reader";
        if (level >= 40)  return "Platinum Reader";
        if (level >= 30)  return "Gold Reader";
        if (level >= 20)  return "Silver Reader";
        if (level >= 10)  return "Bronze Reader";
        return "Copper Reader";
    }

    // ═════════════════════════════════════════════════════════════════════════
    // RARITY CARD FRAMES
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Returns the full CDN URL for the achievement card rarity frame.
     *
     * @param rarity "COMMON" | "RARE" | "EPIC" | "LEGENDARY"
     */
    public static String getRarityFrameUrl(String rarity) {
        if (rarity == null) return BASE_URL + Folders.FRAMES + "38_card_common.png";
        switch (rarity.toUpperCase()) {
            case "RARE":      return BASE_URL + Folders.FRAMES + "39_card_rare.png";
            case "EPIC":      return BASE_URL + Folders.FRAMES + "40_card_epic.png";
            case "LEGENDARY": return BASE_URL + Folders.FRAMES + "41_card_legendary.png";
            default:          return BASE_URL + Folders.FRAMES + "38_card_common.png";
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // THEMES  (.jpg — everything else is .png)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Returns the full CDN URL for a theme background JPG.
     *
     * @param themeName "fantasy" | "scifi" | "romance" | "ocean" | "galaxy" | "gold"
     */
    public static String getThemeUrl(String themeName) {
        if (themeName == null) return null;
        switch (themeName.toLowerCase()) {
            case "fantasy": return BASE_URL + Folders.THEMES + "43_theme_fantasy.jpg";
            case "scifi":   return BASE_URL + Folders.THEMES + "44_theme_scifi.jpg";
            case "romance": return BASE_URL + Folders.THEMES + "45_theme_romance.jpg";
            case "ocean":   return BASE_URL + Folders.THEMES + "46_theme_ocean.jpg";
            case "galaxy":  return BASE_URL + Folders.THEMES + "47_theme_galaxy.jpg";
            case "gold":    return BASE_URL + Folders.THEMES + "48_theme_gold.jpg";
            default:        return null;
        }
    }
}
