package com.novelverse.app.data.local.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.novelverse.app.data.local.dao.AchievementDao;
import com.novelverse.app.data.local.dao.BookmarkDao;
import com.novelverse.app.data.local.dao.ChapterDao;
import com.novelverse.app.data.local.dao.CommentDao;
import com.novelverse.app.data.local.dao.DailyCapDao;
import com.novelverse.app.data.local.dao.NovelDao;
import com.novelverse.app.data.local.dao.ReadingProgressDao;
import com.novelverse.app.data.local.dao.TokenTransactionDao;
import com.novelverse.app.data.local.dao.UserAchievementDao;
import com.novelverse.app.data.local.dao.UserCurrencyDao;
import com.novelverse.app.data.local.dao.UserDao;
import com.novelverse.app.data.local.dao.UserLevelDao;
import com.novelverse.app.data.local.dao.UserStreakDao;
import com.novelverse.app.data.local.entities.AchievementEntity;
import com.novelverse.app.data.local.entities.BookmarkEntity;
import com.novelverse.app.data.local.entities.ChapterEntity;
import com.novelverse.app.data.local.entities.CommentEntity;
import com.novelverse.app.data.local.entities.DailyCapEntity;
import com.novelverse.app.data.local.entities.NovelEntity;
import com.novelverse.app.data.local.entities.ReadingProgressEntity;
import com.novelverse.app.data.local.entities.TokenTransactionEntity;
import com.novelverse.app.data.local.entities.UserAchievementEntity;
import com.novelverse.app.data.local.entities.UserCurrencyEntity;
import com.novelverse.app.data.local.entities.UserEntity;
import com.novelverse.app.data.local.entities.UserLevelEntity;
import com.novelverse.app.data.local.entities.UserStreakEntity;

@Database(
    entities = {
        NovelEntity.class, ChapterEntity.class, UserEntity.class,
        ReadingProgressEntity.class, BookmarkEntity.class, CommentEntity.class,
        AchievementEntity.class, UserAchievementEntity.class, UserCurrencyEntity.class,
        TokenTransactionEntity.class, UserLevelEntity.class, UserStreakEntity.class,
        DailyCapEntity.class
    },
    version = 5, // v5: added cover_url, user_status to users table (profile redesign)
    exportSchema = true
)
@TypeConverters({Converters.class})
public abstract class NovelVerseDatabase extends RoomDatabase {

    public static final String DATABASE_NAME = "novelverse_database";

    public abstract NovelDao novelDao();
    public abstract ChapterDao chapterDao();
    public abstract UserDao userDao();
    public abstract ReadingProgressDao readingProgressDao();
    public abstract BookmarkDao bookmarkDao();
    public abstract CommentDao commentDao();

    public abstract AchievementDao achievementDao();
    public abstract UserAchievementDao userAchievementDao();
    public abstract UserCurrencyDao userCurrencyDao();
    public abstract TokenTransactionDao tokenTransactionDao();
    public abstract UserLevelDao userLevelDao();
    public abstract UserStreakDao userStreakDao();
    public abstract DailyCapDao dailyCapDao();
}
