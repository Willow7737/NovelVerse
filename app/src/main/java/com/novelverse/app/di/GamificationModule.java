package com.novelverse.app.di;

import com.novelverse.app.data.local.dao.AchievementDao;
import com.novelverse.app.data.local.dao.DailyCapDao;
import com.novelverse.app.data.local.dao.TokenTransactionDao;
import com.novelverse.app.data.local.dao.UserAchievementDao;
import com.novelverse.app.data.local.dao.UserCurrencyDao;
import com.novelverse.app.data.local.dao.UserLevelDao;
import com.novelverse.app.data.local.dao.UserStreakDao;
import com.novelverse.app.data.local.database.NovelVerseDatabase;
import com.novelverse.app.data.local.preferences.UserPreferences;
import com.novelverse.app.data.remote.supabase.SupabaseDatabaseService;
import com.novelverse.app.data.repository.GamificationRepository;
import com.novelverse.app.domain.gamification.AchievementEngine;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class GamificationModule {

    // ── DAO providers ─────────────────────────────────────────────────────────

    @Provides @Singleton
    public AchievementDao provideAchievementDao(NovelVerseDatabase db) {
        return db.achievementDao();
    }

    @Provides @Singleton
    public UserAchievementDao provideUserAchievementDao(NovelVerseDatabase db) {
        return db.userAchievementDao();
    }

    @Provides @Singleton
    public UserCurrencyDao provideUserCurrencyDao(NovelVerseDatabase db) {
        return db.userCurrencyDao();
    }

    @Provides @Singleton
    public TokenTransactionDao provideTokenTransactionDao(NovelVerseDatabase db) {
        return db.tokenTransactionDao();
    }

    @Provides @Singleton
    public UserLevelDao provideUserLevelDao(NovelVerseDatabase db) {
        return db.userLevelDao();
    }

    @Provides @Singleton
    public UserStreakDao provideUserStreakDao(NovelVerseDatabase db) {
        return db.userStreakDao();
    }

    @Provides @Singleton
    public DailyCapDao provideDailyCapDao(NovelVerseDatabase db) {
        return db.dailyCapDao();
    }

    // ── Repository ────────────────────────────────────────────────────────────

    @Provides @Singleton
    public GamificationRepository provideGamificationRepository(
            AchievementDao achievementDao,
            UserAchievementDao userAchievementDao,
            UserCurrencyDao userCurrencyDao,
            TokenTransactionDao tokenTransactionDao,
            UserLevelDao userLevelDao,
            UserStreakDao userStreakDao,
            DailyCapDao dailyCapDao,
            SupabaseDatabaseService supabase,
            UserPreferences userPreferences) {              
        return new GamificationRepository(
            achievementDao, userAchievementDao, userCurrencyDao,
            tokenTransactionDao, userLevelDao, userStreakDao, dailyCapDao,
            supabase, userPreferences                       
        );
    }

    // ── Engine ────────────────────────────────────────────────────────────────

    @Provides @Singleton
    public AchievementEngine provideAchievementEngine(GamificationRepository repo) {
        return new AchievementEngine(repo);
    }
}
