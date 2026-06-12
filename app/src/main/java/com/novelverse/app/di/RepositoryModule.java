package com.novelverse.app.di;

import com.novelverse.app.data.local.dao.BookmarkDao;
import com.novelverse.app.data.local.dao.ChapterDao;
import com.novelverse.app.data.local.dao.CommentDao;
import com.novelverse.app.data.local.dao.NovelDao;
import com.novelverse.app.data.local.dao.ReadingProgressDao;
import com.novelverse.app.data.local.dao.UserDao;
import com.novelverse.app.data.local.preferences.UserPreferences;
import com.novelverse.app.data.remote.api.NovelApi;
import com.novelverse.app.data.remote.api.PaymentApi;
import com.novelverse.app.data.remote.api.UserApi;
import com.novelverse.app.data.remote.supabase.SupabaseAuthService;
import com.novelverse.app.data.remote.supabase.SupabaseDatabaseService;
import com.novelverse.app.data.repository.ChapterRepository;
import com.novelverse.app.data.repository.CommentRepository;
import com.novelverse.app.data.repository.FollowsRepository;
import com.novelverse.app.data.repository.LibraryRepository;
import com.novelverse.app.data.repository.NovelRepository;
import com.novelverse.app.data.repository.PaymentRepository;
import com.novelverse.app.data.repository.SearchRepository;
import com.novelverse.app.data.repository.UserRepository;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class RepositoryModule {

    @Provides @Singleton
    public UserRepository provideUserRepository(
            UserDao userDao, SupabaseAuthService authService,
            UserPreferences userPreferences, UserApi userApi,
            SupabaseDatabaseService dbService) {
        return new UserRepository(userDao, authService, userPreferences, userApi, dbService);
    }

    @Provides @Singleton
    public NovelRepository provideNovelRepository(
            NovelDao novelDao, SupabaseDatabaseService db, UserPreferences prefs) {
        return new NovelRepository(novelDao, db, prefs);
    }

    @Provides @Singleton
    public ChapterRepository provideChapterRepository(
            ChapterDao chapterDao, SupabaseDatabaseService db, UserPreferences prefs) {
        return new ChapterRepository(chapterDao, db, prefs);
    }

    @Provides @Singleton
    public LibraryRepository provideLibraryRepository(
            ReadingProgressDao readingProgressDao, BookmarkDao bookmarkDao,
            SupabaseDatabaseService databaseService, UserPreferences userPreferences) {
        return new LibraryRepository(readingProgressDao, bookmarkDao, databaseService, userPreferences);
    }

    @Provides @Singleton
    public CommentRepository provideCommentRepository(
            CommentDao commentDao, SupabaseDatabaseService databaseService) {
        return new CommentRepository(commentDao, databaseService);
    }

    @Provides @Singleton
    public SearchRepository provideSearchRepository(
            NovelDao novelDao, SupabaseDatabaseService databaseService,
            UserPreferences userPreferences) {
        return new SearchRepository(novelDao, databaseService, userPreferences);
    }

    @Provides @Singleton
    public PaymentRepository providePaymentRepository(
            UserDao userDao, PaymentApi paymentApi,
            SupabaseDatabaseService databaseService) {
        return new PaymentRepository(userDao, paymentApi, databaseService);
    }
   
    @Provides @Singleton
    public FollowsRepository provideFollowsRepository(SupabaseDatabaseService dbService) {
        return new FollowsRepository(dbService);
    }
}
