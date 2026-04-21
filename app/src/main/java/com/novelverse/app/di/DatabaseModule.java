package com.novelverse.app.di;

import android.content.Context;

import androidx.room.Room;

import com.novelverse.app.data.local.dao.BookmarkDao;
import com.novelverse.app.data.local.dao.ChapterDao;
import com.novelverse.app.data.local.dao.CommentDao;
import com.novelverse.app.data.local.dao.NovelDao;
import com.novelverse.app.data.local.dao.ReadingProgressDao;
import com.novelverse.app.data.local.dao.UserDao;
import com.novelverse.app.data.local.database.NovelVerseDatabase;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * Database Dagger Hilt Module
 */
@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    @Provides
    @Singleton
    public NovelVerseDatabase provideDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(
                context,
                NovelVerseDatabase.class,
                NovelVerseDatabase.DATABASE_NAME
            )
            .fallbackToDestructiveMigration()
            .build();
    }

    @Provides
    @Singleton
    public NovelDao provideNovelDao(NovelVerseDatabase database) {
        return database.novelDao();
    }

    @Provides
    @Singleton
    public ChapterDao provideChapterDao(NovelVerseDatabase database) {
        return database.chapterDao();
    }

    @Provides
    @Singleton
    public UserDao provideUserDao(NovelVerseDatabase database) {
        return database.userDao();
    }

    @Provides
    @Singleton
    public ReadingProgressDao provideReadingProgressDao(NovelVerseDatabase database) {
        return database.readingProgressDao();
    }

    @Provides
    @Singleton
    public BookmarkDao provideBookmarkDao(NovelVerseDatabase database) {
        return database.bookmarkDao();
    }

    @Provides
    @Singleton
    public CommentDao provideCommentDao(NovelVerseDatabase database) {
        return database.commentDao();
    }
}
