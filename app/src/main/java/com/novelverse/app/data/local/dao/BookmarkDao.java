package com.novelverse.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.novelverse.app.data.local.entities.BookmarkEntity;

import java.util.List;

/**
 * DAO for Bookmark operations
 */
@Dao
public interface BookmarkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(BookmarkEntity bookmark);

    @Update
    void update(BookmarkEntity bookmark);

    @Delete
    void delete(BookmarkEntity bookmark);

    @Query("DELETE FROM bookmarks WHERE id = :bookmarkId")
    void deleteById(String bookmarkId);

    @Query("SELECT * FROM bookmarks WHERE id = :bookmarkId")
    BookmarkEntity getBookmarkById(String bookmarkId);

    @Query("SELECT * FROM bookmarks WHERE user_id = :userId ORDER BY created_at DESC")
    LiveData<List<BookmarkEntity>> getBookmarksForUser(String userId);

    @Query("SELECT * FROM bookmarks WHERE user_id = :userId AND novel_id = :novelId ORDER BY position ASC")
    List<BookmarkEntity> getBookmarksForNovel(String userId, String novelId);

    @Query("SELECT * FROM bookmarks WHERE user_id = :userId AND chapter_id = :chapterId ORDER BY position ASC")
    List<BookmarkEntity> getBookmarksForChapter(String userId, String chapterId);

    @Query("SELECT * FROM bookmarks WHERE user_id = :userId AND is_favorite = 1 ORDER BY created_at DESC")
    LiveData<List<BookmarkEntity>> getFavoriteBookmarks(String userId);

    @Query("SELECT COUNT(*) FROM bookmarks WHERE user_id = :userId AND novel_id = :novelId")
    int getBookmarkCountForNovel(String userId, String novelId);

    @Query("UPDATE bookmarks SET note = :note, is_dirty = 1 WHERE id = :bookmarkId")
    void updateNote(String bookmarkId, String note);

    @Query("UPDATE bookmarks SET is_favorite = :isFavorite, is_dirty = 1 WHERE id = :bookmarkId")
    void updateFavoriteStatus(String bookmarkId, boolean isFavorite);

    @Query("SELECT * FROM bookmarks WHERE is_dirty = 1")
    List<BookmarkEntity> getDirtyBookmarks();

    @Query("UPDATE bookmarks SET is_dirty = 0, synced_at = :timestamp WHERE id = :bookmarkId")
    void markAsSynced(String bookmarkId, long timestamp);

    @Query("DELETE FROM bookmarks WHERE user_id = :userId")
    void deleteAllUserBookmarks(String userId);
}
