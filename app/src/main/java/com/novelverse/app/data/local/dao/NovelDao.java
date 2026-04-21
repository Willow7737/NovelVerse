package com.novelverse.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.paging.PagingSource;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.novelverse.app.data.local.entities.NovelEntity;

import java.util.List;

/**
 * DAO for Novel operations
 */
@Dao
public interface NovelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(NovelEntity novel);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<NovelEntity> novels);

    @Update
    void update(NovelEntity novel);

    @Delete
    void delete(NovelEntity novel);

    @Query("DELETE FROM novels WHERE id = :novelId")
    void deleteById(String novelId);

    @Query("SELECT * FROM novels WHERE id = :novelId")
    LiveData<NovelEntity> getNovelById(String novelId);

    @Query("SELECT * FROM novels WHERE id = :novelId")
    NovelEntity getNovelByIdSync(String novelId);

    @Query("SELECT * FROM novels WHERE is_published = 1 ORDER BY last_updated_at DESC")
    LiveData<List<NovelEntity>> getAllPublishedNovels();

    @Query("SELECT * FROM novels WHERE is_published = 1 ORDER BY last_updated_at DESC")
    PagingSource<Integer, NovelEntity> getAllPublishedNovelsPaged();

    @Query("SELECT * FROM novels WHERE author_id = :authorId ORDER BY created_at DESC")
    LiveData<List<NovelEntity>> getNovelsByAuthor(String authorId);

    @Query("SELECT * FROM novels WHERE is_featured = 1 AND is_published = 1 ORDER BY featured_at DESC")
    LiveData<List<NovelEntity>> getFeaturedNovels();

    @Query("SELECT * FROM novels WHERE is_published = 1 ORDER BY total_views DESC LIMIT :limit")
    LiveData<List<NovelEntity>> getTrendingNovels(int limit);

    @Query("SELECT * FROM novels WHERE is_published = 1 ORDER BY published_at DESC LIMIT :limit")
    LiveData<List<NovelEntity>> getNewReleases(int limit);

    @Query("SELECT * FROM novels WHERE status = 'completed' AND is_published = 1 ORDER BY completed_at DESC LIMIT :limit")
    LiveData<List<NovelEntity>> getCompletedNovels(int limit);

    @Query("SELECT * FROM novels WHERE is_published = 1 ORDER BY average_rating DESC LIMIT :limit")
    LiveData<List<NovelEntity>> getTopRatedNovels(int limit);

    @Query("SELECT * FROM novels WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    PagingSource<Integer, NovelEntity> searchNovels(String query);

    @Query("SELECT * FROM novels WHERE genres LIKE '%' || :genre || '%' AND is_published = 1")
    PagingSource<Integer, NovelEntity> getNovelsByGenre(String genre);

    @Query("UPDATE novels SET is_downloaded = :isDownloaded, download_completed_at = :timestamp WHERE id = :novelId")
    void updateDownloadStatus(String novelId, boolean isDownloaded, long timestamp);

    @Query("SELECT * FROM novels WHERE is_downloaded = 1")
    LiveData<List<NovelEntity>> getDownloadedNovels();

    @Query("SELECT * FROM novels WHERE is_dirty = 1")
    List<NovelEntity> getDirtyNovels();

    @Query("UPDATE novels SET is_dirty = 0, synced_at = :timestamp WHERE id = :novelId")
    void markAsSynced(String novelId, long timestamp);

    @Query("DELETE FROM novels WHERE synced_at < :timestamp AND is_downloaded = 0")
    void deleteOldCache(long timestamp);

    @Query("SELECT COUNT(*) FROM novels")
    int getNovelCount();

    @Query("DELETE FROM novels")
    void deleteAll();
}
