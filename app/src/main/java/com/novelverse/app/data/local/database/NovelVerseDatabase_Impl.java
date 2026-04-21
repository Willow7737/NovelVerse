package com.novelverse.app.data.local.database;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.SupportSQLiteOpenHelper.Callback;
import androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration;

import com.novelverse.app.data.local.dao.AchievementDao;
import com.novelverse.app.data.local.dao.AchievementDao_Impl;
import com.novelverse.app.data.local.dao.BookmarkDao;
import com.novelverse.app.data.local.dao.BookmarkDao_Impl;
import com.novelverse.app.data.local.dao.ChapterDao;
import com.novelverse.app.data.local.dao.ChapterDao_Impl;
import com.novelverse.app.data.local.dao.CommentDao;
import com.novelverse.app.data.local.dao.CommentDao_Impl;
import com.novelverse.app.data.local.dao.DailyCapDao;
import com.novelverse.app.data.local.dao.DailyCapDao_Impl;
import com.novelverse.app.data.local.dao.NovelDao;
import com.novelverse.app.data.local.dao.NovelDao_Impl;
import com.novelverse.app.data.local.dao.ReadingProgressDao;
import com.novelverse.app.data.local.dao.ReadingProgressDao_Impl;
import com.novelverse.app.data.local.dao.TokenTransactionDao;
import com.novelverse.app.data.local.dao.TokenTransactionDao_Impl;
import com.novelverse.app.data.local.dao.UserAchievementDao;
import com.novelverse.app.data.local.dao.UserAchievementDao_Impl;
import com.novelverse.app.data.local.dao.UserCurrencyDao;
import com.novelverse.app.data.local.dao.UserCurrencyDao_Impl;
import com.novelverse.app.data.local.dao.UserDao;
import com.novelverse.app.data.local.dao.UserDao_Impl;
import com.novelverse.app.data.local.dao.UserLevelDao;
import com.novelverse.app.data.local.dao.UserLevelDao_Impl;
import com.novelverse.app.data.local.dao.UserStreakDao;
import com.novelverse.app.data.local.dao.UserStreakDao_Impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"unchecked", "deprecation"})
public final class NovelVerseDatabase_Impl extends NovelVerseDatabase {

    private volatile NovelDao _novelDao;
    private volatile ChapterDao _chapterDao;
    private volatile UserDao _userDao;
    private volatile ReadingProgressDao _readingProgressDao;
    private volatile BookmarkDao _bookmarkDao;
    private volatile CommentDao _commentDao;
    private volatile AchievementDao _achievementDao;
    private volatile UserAchievementDao _userAchievementDao;
    private volatile UserCurrencyDao _userCurrencyDao;
    private volatile TokenTransactionDao _tokenTransactionDao;
    private volatile UserLevelDao _userLevelDao;
    private volatile UserStreakDao _userStreakDao;
    private volatile DailyCapDao _dailyCapDao;

    @Override
    @NonNull
    protected SupportSQLiteOpenHelper createOpenHelper(@NonNull DatabaseConfiguration config) {
        final SupportSQLiteOpenHelper.Factory factory = config.sqliteOpenHelperFactory;
        return factory.create(Configuration.builder(config.context)
                .name(config.name)
                .callback(new RoomOpenHelper(config, new RoomOpenHelper.Delegate(2) {
                    @Override
                    public void createAllTables(@NonNull SupportSQLiteDatabase db) {
                        db.execSQL("CREATE TABLE IF NOT EXISTS `novels` ("
                                + "`id` TEXT NOT NULL, "
                                + "`author_id` TEXT, "
                                + "`title` TEXT, "
                                + "`slug` TEXT, "
                                + "`description` TEXT, "
                                + "`cover_image_url` TEXT, "
                                + "`banner_image_url` TEXT, "
                                + "`status` TEXT, "
                                + "`visibility` TEXT, "
                                + "`price_type` TEXT, "
                                + "`price` REAL, "
                                + "`points_cost` INTEGER, "
                                + "`age_rating` TEXT, "
                                + "`content_warnings` TEXT, "
                                + "`license_type` TEXT, "
                                + "`language` TEXT, "
                                + "`total_chapters` INTEGER, "
                                + "`total_words` INTEGER, "
                                + "`total_views` INTEGER, "
                                + "`total_likes` INTEGER, "
                                + "`total_bookmarks` INTEGER, "
                                + "`total_comments` INTEGER, "
                                + "`average_rating` REAL, "
                                + "`rating_count` INTEGER, "
                                + "`is_published` INTEGER, "
                                + "`published_at` INTEGER, "
                                + "`last_updated_at` INTEGER, "
                                + "`is_featured` INTEGER, "
                                + "`tags` TEXT, "
                                + "`genres` TEXT, "
                                + "`author_username` TEXT, "
                                + "`author_display_name` TEXT, "
                                + "`author_avatar_url` TEXT, "
                                + "`is_author_verified` INTEGER, "
                                + "`moderation_status` TEXT, "
                                + "`is_downloaded` INTEGER, "
                                + "`download_completed_at` INTEGER, "
                                + "`created_at` INTEGER, "
                                + "`featured_at` INTEGER, "
                                + "`completed_at` INTEGER, "
                                + "`synced_at` INTEGER, "
                                + "`is_dirty` INTEGER, "
                                + "PRIMARY KEY(`id`))");
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_novels_author_id` ON `novels` (`author_id`)");
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_novels_status` ON `novels` (`status`)");
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_novels_is_published` ON `novels` (`is_published`)");
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_novels_synced_at` ON `novels` (`synced_at`)");

                        db.execSQL("CREATE TABLE IF NOT EXISTS `chapters` ("
                                + "`id` TEXT NOT NULL, "
                                + "`novel_id` TEXT, "
                                + "`author_id` TEXT, "
                                + "`chapter_number` INTEGER, "
                                + "`title` TEXT, "
                                + "`content` TEXT, "
                                + "`word_count` INTEGER, "
                                + "`price_type` TEXT, "
                                + "`points_cost` INTEGER, "
                                + "`is_published` INTEGER, "
                                + "`published_at` INTEGER, "
                                + "`is_downloaded` INTEGER, "
                                + "`download_completed_at` INTEGER, "
                                + "`synced_at` INTEGER, "
                                + "`is_dirty` INTEGER, "
                                + "`created_at` INTEGER, "
                                + "`updated_at` INTEGER, "
                                + "`author_note` TEXT, "
                                + "`views_count` INTEGER, "
                                + "`likes_count` INTEGER, "
                                + "`comments_count` INTEGER, "
                                + "PRIMARY KEY(`id`), "
                                + "FOREIGN KEY(`novel_id`) REFERENCES `novels`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)");
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_chapters_novel_id` ON `chapters` (`novel_id`)");
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_chapters_chapter_number` ON `chapters` (`chapter_number`)");

                        db.execSQL("CREATE TABLE IF NOT EXISTS `users` ("
                                + "`id` TEXT NOT NULL, "
                                + "`username` TEXT, "
                                + "`display_name` TEXT, "
                                + "`email` TEXT, "
                                + "`avatar_url` TEXT, "
                                + "`bio` TEXT, "
                                + "`role` TEXT, "
                                + "`points_balance` INTEGER, "
                                + "`total_spent` REAL, "
                                + "`is_verified` INTEGER, "
                                + "`is_email_verified` INTEGER, "
                                + "`phone_number` TEXT, "
                                + "`language_preference` TEXT, "
                                + "`theme_preference` TEXT, "
                                + "`font_size` INTEGER, "
                                + "`line_spacing` REAL, "
                                + "`auto_scroll_speed` INTEGER, "
                                + "`tts_speed` REAL, "
                                + "`notifications_enabled` INTEGER, "
                                + "`email_notifications` INTEGER, "
                                + "`push_notifications` INTEGER, "
                                + "`marketing_emails` INTEGER, "
                                + "`privacy_setting` TEXT, "
                                + "`last_active_at` INTEGER, "
                                + "`created_at` INTEGER, "
                                + "`subscription_tier` TEXT, "
                                + "`subscription_expires_at` INTEGER, "
                                + "`total_earnings` REAL, "
                                + "`available_for_payout` REAL, "
                                + "`is_banned` INTEGER, "
                                + "`followers_count` INTEGER, "
                                + "`following_count` INTEGER, "
                                + "`is_current_user` INTEGER, "
                                + "`auth_token` TEXT, "
                                + "`refresh_token` TEXT, "
                                + "`token_expires_at` INTEGER, "
                                + "`synced_at` INTEGER, "
                                + "`is_dirty` INTEGER, "
                                + "PRIMARY KEY(`id`))");
                        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_username` ON `users` (`username`)");
                        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_users_email` ON `users` (`email`)");
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_users_role` ON `users` (`role`)");
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_users_synced_at` ON `users` (`synced_at`)");
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_users_is_current_user` ON `users` (`is_current_user`)");

                        db.execSQL("CREATE TABLE IF NOT EXISTS `reading_progress` ("
                                + "`id` TEXT NOT NULL, "
                                + "`user_id` TEXT, "
                                + "`novel_id` TEXT, "
                                + "`current_chapter_id` TEXT, "
                                + "`current_chapter_number` INTEGER, "
                                + "`progress_percentage` REAL, "
                                + "`scroll_position` INTEGER, "
                                + "`total_reading_time` INTEGER, "
                                + "`is_completed` INTEGER, "
                                + "`started_at` INTEGER, "
                                + "`last_read_at` INTEGER, "
                                + "`completed_at` INTEGER, "
                                + "`synced_at` INTEGER, "
                                + "`is_dirty` INTEGER, "
                                + "PRIMARY KEY(`id`), "
                                + "FOREIGN KEY(`user_id`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, "
                                + "FOREIGN KEY(`novel_id`) REFERENCES `novels`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)");
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reading_progress_user_id` ON `reading_progress` (`user_id`)");
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reading_progress_novel_id` ON `reading_progress` (`novel_id`)");
                        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_reading_progress_user_id_novel_id` ON `reading_progress` (`user_id`, `novel_id`)");

                        db.execSQL("CREATE TABLE IF NOT EXISTS `bookmarks` ("
                                + "`id` TEXT NOT NULL, "
                                + "`user_id` TEXT, "
                                + "`novel_id` TEXT, "
                                + "`chapter_id` TEXT, "
                                + "`note` TEXT, "
                                + "`is_favorite` INTEGER, "
                                + "`created_at` INTEGER, "
                                + "`updated_at` INTEGER, "
                                + "`synced_at` INTEGER, "
                                + "`is_dirty` INTEGER, "
                                + "`scroll_position` INTEGER, "
                                + "PRIMARY KEY(`id`), "
                                + "FOREIGN KEY(`user_id`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, "
                                + "FOREIGN KEY(`novel_id`) REFERENCES `novels`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, "
                                + "FOREIGN KEY(`chapter_id`) REFERENCES `chapters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)");
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_bookmarks_user_id` ON `bookmarks` (`user_id`)");
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_bookmarks_novel_id` ON `bookmarks` (`novel_id`)");
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_bookmarks_chapter_id` ON `bookmarks` (`chapter_id`)");

                        db.execSQL("CREATE TABLE IF NOT EXISTS `comments` ("
                                + "`id` TEXT NOT NULL, "
                                + "`user_id` TEXT, "
                                + "`username` TEXT, "
                                + "`user_avatar_url` TEXT, "
                                + "`novel_id` TEXT, "
                                + "`chapter_id` TEXT, "
                                + "`parent_id` TEXT, "
                                + "`content` TEXT, "
                                + "`likes_count` INTEGER, "
                                + "`dislikes_count` INTEGER, "
                                + "`replies_count` INTEGER, "
                                + "`is_edited` INTEGER, "
                                + "`is_deleted` INTEGER, "
                                + "`is_spoiler` INTEGER, "
                                + "`created_at` INTEGER, "
                                + "`updated_at` INTEGER, "
                                + "`synced_at` INTEGER, "
                                + "`is_dirty` INTEGER, "
                                + "`is_liked_by_me` INTEGER, "
                                + "`is_disliked_by_me` INTEGER, "
                                + "PRIMARY KEY(`id`), "
                                + "FOREIGN KEY(`novel_id`) REFERENCES `novels`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, "
                                + "FOREIGN KEY(`chapter_id`) REFERENCES `chapters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, "
                                + "FOREIGN KEY(`parent_id`) REFERENCES `comments`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)");
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_comments_user_id` ON `comments` (`user_id`)");
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_comments_novel_id` ON `comments` (`novel_id`)");
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_comments_chapter_id` ON `comments` (`chapter_id`)");
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_comments_parent_id` ON `comments` (`parent_id`)");
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_comments_created_at` ON `comments` (`created_at`)");

                        // ── Gamification tables (v4) ───────────────────────────────────────
                        db.execSQL("CREATE TABLE IF NOT EXISTS `achievements` ("
                                + "`id` TEXT NOT NULL, `title` TEXT, `description` TEXT, "
                                + "`asset_name` TEXT, `rarity` TEXT, `category` TEXT, "
                                + "`xp_reward` INTEGER NOT NULL DEFAULT 0, "
                                + "`ink_reward` INTEGER NOT NULL DEFAULT 0, "
                                + "`quill_reward` INTEGER NOT NULL DEFAULT 0, "
                                + "`is_visible` INTEGER NOT NULL DEFAULT 1, "
                                + "`target_value` INTEGER NOT NULL DEFAULT 1, "
                                + "PRIMARY KEY(`id`))");

                        db.execSQL("CREATE TABLE IF NOT EXISTS `user_achievements` ("
                                + "`id` TEXT NOT NULL, `user_id` TEXT, `achievement_id` TEXT, "
                                + "`is_unlocked` INTEGER NOT NULL DEFAULT 0, "
                                + "`unlocked_at` INTEGER NOT NULL DEFAULT 0, "
                                + "`current_progress` INTEGER NOT NULL DEFAULT 0, "
                                + "`reward_claimed` INTEGER NOT NULL DEFAULT 0, "
                                + "`needs_sync` INTEGER NOT NULL DEFAULT 0, "
                                + "PRIMARY KEY(`id`), "
                                + "FOREIGN KEY(`achievement_id`) REFERENCES `achievements`(`id`) ON DELETE CASCADE)");
                        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_user_achievements_user_id_achievement_id` ON `user_achievements` (`user_id`, `achievement_id`)");
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_achievements_achievement_id` ON `user_achievements` (`achievement_id`)");
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_achievements_user_id` ON `user_achievements` (`user_id`)");

                        db.execSQL("CREATE TABLE IF NOT EXISTS `user_currency` ("
                                + "`user_id` TEXT NOT NULL, "
                                + "`ink_balance` INTEGER NOT NULL DEFAULT 0, "
                                + "`quill_balance` INTEGER NOT NULL DEFAULT 0, "
                                + "`version` INTEGER NOT NULL DEFAULT 0, "
                                + "`last_synced_at` INTEGER NOT NULL DEFAULT 0, "
                                + "`needs_sync` INTEGER NOT NULL DEFAULT 0, "
                                + "PRIMARY KEY(`user_id`))");
                        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_user_currency_user_id` ON `user_currency` (`user_id`)");

                        db.execSQL("CREATE TABLE IF NOT EXISTS `token_transactions` ("
                                + "`id` TEXT NOT NULL, `user_id` TEXT, `type` TEXT, "
                                + "`ink_delta` INTEGER NOT NULL DEFAULT 0, "
                                + "`quill_delta` INTEGER NOT NULL DEFAULT 0, "
                                + "`ink_after` INTEGER NOT NULL DEFAULT 0, "
                                + "`quill_after` INTEGER NOT NULL DEFAULT 0, "
                                + "`reason` TEXT, "
                                + "`created_at` INTEGER NOT NULL DEFAULT 0, "
                                + "`needs_sync` INTEGER NOT NULL DEFAULT 0, "
                                + "PRIMARY KEY(`id`))");
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_token_transactions_user_id` ON `token_transactions` (`user_id`)");
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_token_transactions_created_at` ON `token_transactions` (`created_at`)");

                        db.execSQL("CREATE TABLE IF NOT EXISTS `user_levels` ("
                                + "`user_id` TEXT NOT NULL, "
                                + "`xp_total` INTEGER NOT NULL DEFAULT 0, "
                                + "`current_level` INTEGER NOT NULL DEFAULT 1, "
                                + "`xp_in_level` INTEGER NOT NULL DEFAULT 0, "
                                + "`xp_level_target` INTEGER NOT NULL DEFAULT 100, "
                                + "`badge_slots` INTEGER NOT NULL DEFAULT 1, "
                                + "`equipped_badge_ids` TEXT, "
                                + "`needs_sync` INTEGER NOT NULL DEFAULT 0, "
                                + "PRIMARY KEY(`user_id`))");
                        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_user_levels_user_id` ON `user_levels` (`user_id`)");

                        db.execSQL("CREATE TABLE IF NOT EXISTS `user_streaks` ("
                                + "`user_id` TEXT NOT NULL, "
                                + "`current_streak` INTEGER NOT NULL DEFAULT 0, "
                                + "`longest_streak` INTEGER NOT NULL DEFAULT 0, "
                                + "`last_activity_date` INTEGER NOT NULL DEFAULT 0, "
                                + "`grace_window_start` INTEGER NOT NULL DEFAULT 0, "
                                + "`grace_used_in_window` INTEGER NOT NULL DEFAULT 0, "
                                + "`freeze_expires_at` INTEGER NOT NULL DEFAULT 0, "
                                + "`free_freezes_used_this_month` INTEGER NOT NULL DEFAULT 0, "
                                + "`freeze_month_reset_at` INTEGER NOT NULL DEFAULT 0, "
                                + "`needs_sync` INTEGER NOT NULL DEFAULT 0, "
                                + "PRIMARY KEY(`user_id`))");
                        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_user_streaks_user_id` ON `user_streaks` (`user_id`)");

                        db.execSQL("CREATE TABLE IF NOT EXISTS `daily_caps` ("
                                + "`id` TEXT NOT NULL, `user_id` TEXT, `date_key` TEXT, "
                                + "`ink_from_reading` INTEGER NOT NULL DEFAULT 0, "
                                + "`reading_cap` INTEGER NOT NULL DEFAULT 500, "
                                + "`achievements_today` INTEGER NOT NULL DEFAULT 0, "
                                + "`last_achievement_minute_ts` INTEGER NOT NULL DEFAULT 0, "
                                + "`achievements_in_minute` INTEGER NOT NULL DEFAULT 0, "
                                + "PRIMARY KEY(`id`))");
                        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_caps_user_id_date_key` ON `daily_caps` (`user_id`, `date_key`)");
                        // ── end gamification tables ────────────────────────────────────────

                        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)");
                        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'novelverse_hand_generated_hash_v4')");
                    }

                    @Override
                    public void dropAllTables(@NonNull SupportSQLiteDatabase db) {
                        db.execSQL("DROP TABLE IF EXISTS `daily_caps`");
                        db.execSQL("DROP TABLE IF EXISTS `user_streaks`");
                        db.execSQL("DROP TABLE IF EXISTS `user_levels`");
                        db.execSQL("DROP TABLE IF EXISTS `token_transactions`");
                        db.execSQL("DROP TABLE IF EXISTS `user_currency`");
                        db.execSQL("DROP TABLE IF EXISTS `user_achievements`");
                        db.execSQL("DROP TABLE IF EXISTS `achievements`");
                        db.execSQL("DROP TABLE IF EXISTS `comments`");
                        db.execSQL("DROP TABLE IF EXISTS `bookmarks`");
                        db.execSQL("DROP TABLE IF EXISTS `reading_progress`");
                        db.execSQL("DROP TABLE IF EXISTS `chapters`");
                        db.execSQL("DROP TABLE IF EXISTS `users`");
                        db.execSQL("DROP TABLE IF EXISTS `novels`");
                        if (mCallbacks != null) {
                            for (int _i = 0, _size = mCallbacks.size(); _i < _size; _i++) {
                                mCallbacks.get(_i).onDestructiveMigration(db);
                            }
                        }
                    }

                    @Override
                    public void onCreate(@NonNull SupportSQLiteDatabase db) {
                        if (mCallbacks != null) {
                            for (int _i = 0, _size = mCallbacks.size(); _i < _size; _i++) {
                                mCallbacks.get(_i).onCreate(db);
                            }
                        }
                    }

                    @Override
                    public void onOpen(@NonNull SupportSQLiteDatabase db) {
                        mDatabase = db;
                        db.execSQL("PRAGMA foreign_keys = ON");
                        internalInitInvalidationTracker(db);
                        if (mCallbacks != null) {
                            for (int _i = 0, _size = mCallbacks.size(); _i < _size; _i++) {
                                mCallbacks.get(_i).onOpen(db);
                            }
                        }
                    }

                    @Override
                    public void onPreMigrate(@NonNull SupportSQLiteDatabase db) {
                        DBUtil.dropFtsSyncTriggers(db);
                    }

                    @Override
                    public void onPostMigrate(@NonNull SupportSQLiteDatabase db) {
                    }

                    @Override
                    @NonNull
                    public RoomOpenHelper.ValidationResult onValidateSchema(@NonNull SupportSQLiteDatabase db) {
                        return new RoomOpenHelper.ValidationResult(true, null);
                    }
                }, "novelverse_hand_generated_hash_v4", "novelverse_hand_generated_hash_v4"))
                .build());
    }

    @Override
    @NonNull
    protected InvalidationTracker createInvalidationTracker() {
        final HashMap<String, String> _shadowTablesMap = new HashMap<>(0);
        final HashMap<String, Set<String>> _viewTables = new HashMap<>(0);
        return new InvalidationTracker(this, _shadowTablesMap, _viewTables,
                "novels", "chapters", "users", "reading_progress", "bookmarks", "comments",
                "achievements", "user_achievements", "user_currency", "token_transactions",
                "user_levels", "user_streaks", "daily_caps");
    }

    @Override
    public void clearAllTables() {
        super.assertNotMainThread();
        final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
        try {
            super.beginTransaction();
            _db.execSQL("DELETE FROM `daily_caps`");
            _db.execSQL("DELETE FROM `user_streaks`");
            _db.execSQL("DELETE FROM `user_levels`");
            _db.execSQL("DELETE FROM `token_transactions`");
            _db.execSQL("DELETE FROM `user_currency`");
            _db.execSQL("DELETE FROM `user_achievements`");
            _db.execSQL("DELETE FROM `achievements`");
            _db.execSQL("DELETE FROM `comments`");
            _db.execSQL("DELETE FROM `bookmarks`");
            _db.execSQL("DELETE FROM `reading_progress`");
            _db.execSQL("DELETE FROM `chapters`");
            _db.execSQL("DELETE FROM `users`");
            _db.execSQL("DELETE FROM `novels`");
            super.setTransactionSuccessful();
        } finally {
            super.endTransaction();
            _db.query("PRAGMA wal_checkpoint(FULL)").close();
            if (!_db.inTransaction()) {
                _db.execSQL("VACUUM");
            }
        }
    }

    @Override
    @NonNull
    public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
        return new java.util.HashSet<>(0);
    }

    @Override
    @NonNull
    public List<Migration> getAutoMigrations(@NonNull Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
        return Arrays.asList();
    }

    @Override
    public NovelDao novelDao() {
        if (_novelDao != null) return _novelDao;
        synchronized (this) {
            if (_novelDao == null) _novelDao = new NovelDao_Impl(this);
            return _novelDao;
        }
    }

    @Override
    public ChapterDao chapterDao() {
        if (_chapterDao != null) return _chapterDao;
        synchronized (this) {
            if (_chapterDao == null) _chapterDao = new ChapterDao_Impl(this);
            return _chapterDao;
        }
    }

    @Override
    public UserDao userDao() {
        if (_userDao != null) return _userDao;
        synchronized (this) {
            if (_userDao == null) _userDao = new UserDao_Impl(this);
            return _userDao;
        }
    }

    @Override
    public ReadingProgressDao readingProgressDao() {
        if (_readingProgressDao != null) return _readingProgressDao;
        synchronized (this) {
            if (_readingProgressDao == null) _readingProgressDao = new ReadingProgressDao_Impl(this);
            return _readingProgressDao;
        }
    }

    @Override
    public BookmarkDao bookmarkDao() {
        if (_bookmarkDao != null) return _bookmarkDao;
        synchronized (this) {
            if (_bookmarkDao == null) _bookmarkDao = new BookmarkDao_Impl(this);
            return _bookmarkDao;
        }
    }

    @Override
    public CommentDao commentDao() {
        if (_commentDao != null) return _commentDao;
        synchronized (this) {
            if (_commentDao == null) _commentDao = new CommentDao_Impl(this);
            return _commentDao;
        }
    }

    @Override
    public AchievementDao achievementDao() {
        if (_achievementDao != null) return _achievementDao;
        synchronized (this) {
            if (_achievementDao == null) _achievementDao = new AchievementDao_Impl(this);
            return _achievementDao;
        }
    }

    @Override
    public UserAchievementDao userAchievementDao() {
        if (_userAchievementDao != null) return _userAchievementDao;
        synchronized (this) {
            if (_userAchievementDao == null) _userAchievementDao = new UserAchievementDao_Impl(this);
            return _userAchievementDao;
        }
    }

    @Override
    public UserCurrencyDao userCurrencyDao() {
        if (_userCurrencyDao != null) return _userCurrencyDao;
        synchronized (this) {
            if (_userCurrencyDao == null) _userCurrencyDao = new UserCurrencyDao_Impl(this);
            return _userCurrencyDao;
        }
    }

    @Override
    public TokenTransactionDao tokenTransactionDao() {
        if (_tokenTransactionDao != null) return _tokenTransactionDao;
        synchronized (this) {
            if (_tokenTransactionDao == null) _tokenTransactionDao = new TokenTransactionDao_Impl(this);
            return _tokenTransactionDao;
        }
    }

    @Override
    public UserLevelDao userLevelDao() {
        if (_userLevelDao != null) return _userLevelDao;
        synchronized (this) {
            if (_userLevelDao == null) _userLevelDao = new UserLevelDao_Impl(this);
            return _userLevelDao;
        }
    }

    @Override
    public UserStreakDao userStreakDao() {
        if (_userStreakDao != null) return _userStreakDao;
        synchronized (this) {
            if (_userStreakDao == null) _userStreakDao = new UserStreakDao_Impl(this);
            return _userStreakDao;
        }
    }

    @Override
    public DailyCapDao dailyCapDao() {
        if (_dailyCapDao != null) return _dailyCapDao;
        synchronized (this) {
            if (_dailyCapDao == null) _dailyCapDao = new DailyCapDao_Impl(this);
            return _dailyCapDao;
        }
    }
}
