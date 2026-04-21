package com.novelverse.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.novelverse.app.data.local.entities.ReadingProgressEntity;

import java.util.Date;
import java.util.List;

/**
 * DAO for Reading Progress (offline cache).
 *
 * Three methods added for ReadingProgressRepository:
 *   getProgressForNovel  – synchronous lookup used by the repo's offline fallback
 *   insertOrUpdate       – REPLACE-semantics upsert by (user_id, novel_id)
 *   markSynced           – clears needs_sync flag after a successful RPC replay
 */
@Dao
public interface ReadingProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ReadingProgressEntity progress);

    @Update
    void update(ReadingProgressEntity progress);

    @Delete
    void delete(ReadingProgressEntity progress);

    @Query("DELETE FROM reading_progress WHERE id = :progressId")
    void deleteById(String progressId);

    @Query("SELECT * FROM reading_progress WHERE id = :progressId")
    ReadingProgressEntity getProgressById(String progressId);

    @Query("SELECT * FROM reading_progress WHERE user_id = :userId AND novel_id = :novelId")
    LiveData<ReadingProgressEntity> getProgress(String userId, String novelId);

    @Query("SELECT * FROM reading_progress WHERE user_id = :userId AND novel_id = :novelId")
    ReadingProgressEntity getProgressSync(String userId, String novelId);

    /**
     * Used by ReadingProgressRepository for its offline-cache fallback.
     * Semantically identical to getProgressSync; named separately for clarity at call sites.
     */
    @Query("SELECT * FROM reading_progress WHERE user_id = :userId AND novel_id = :novelId")
    ReadingProgressEntity getProgressForNovel(String userId, String novelId);

    /**
     * INSERT OR REPLACE on the (user_id, novel_id) natural key.
     * Because the entity's unique index is on (user_id, novel_id), this upserts cleanly
     * without generating a new id for an existing row — the entity passed in must already
     * have the same id as any row it replaces (callers use getProgressForNovel first).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(ReadingProgressEntity entity);

    /**
     * Clears the needs_sync / is_dirty flags after a successful RPC replay.
     * Takes user_id + novel_id so the caller doesn't need to hold the row's surrogate id.
     */
    @Query("UPDATE reading_progress " +
           "SET needs_sync = 0, is_dirty = 0, synced_at = :syncedAt " +
           "WHERE user_id = :userId AND novel_id = :novelId")
    void markSynced(String userId, String novelId, Date syncedAt);

    @Query("SELECT * FROM reading_progress WHERE user_id = :userId ORDER BY last_read_at DESC")
    LiveData<List<ReadingProgressEntity>> getAllProgressForUser(String userId);

    @Query("SELECT * FROM reading_progress WHERE user_id = :userId ORDER BY last_read_at DESC LIMIT :limit")
    List<ReadingProgressEntity> getRecentProgress(String userId, int limit);

    @Query("SELECT * FROM reading_progress WHERE user_id = :userId AND is_completed = 1")
    List<ReadingProgressEntity> getCompletedNovels(String userId);

    @Query("UPDATE reading_progress SET scroll_position = :position, progress_percentage = :percentage, " +
           "last_read_at = :timestamp, is_dirty = 1, needs_sync = 1 " +
           "WHERE user_id = :userId AND novel_id = :novelId")
    void updateProgress(String userId, String novelId, int position, double percentage, long timestamp);

    @Query("UPDATE reading_progress SET chapter_id = :chapterId, scroll_position = 0, " +
           "progress_percentage = 0, last_read_at = :timestamp, is_dirty = 1, needs_sync = 1 " +
           "WHERE user_id = :userId AND novel_id = :novelId")
    void updateCurrentChapter(String userId, String novelId, String chapterId, long timestamp);

    @Query("UPDATE reading_progress SET is_completed = 1, completed_at = :timestamp, " +
           "is_dirty = 1, needs_sync = 1 " +
           "WHERE user_id = :userId AND novel_id = :novelId")
    void markAsCompleted(String userId, String novelId, long timestamp);

    @Query("UPDATE reading_progress SET total_reading_time = total_reading_time + :seconds " +
           "WHERE user_id = :userId AND novel_id = :novelId")
    void addReadingTime(String userId, String novelId, int seconds);

    @Query("SELECT * FROM reading_progress WHERE needs_sync = 1")
    List<ReadingProgressEntity> getPendingSync();

    @Query("UPDATE reading_progress SET needs_sync = 0, synced_at = :timestamp, is_dirty = 0 " +
           "WHERE id = :progressId")
    void markAsSynced(String progressId, long timestamp);

    @Query("SELECT COUNT(*) FROM reading_progress WHERE user_id = :userId")
    int getReadingCount(String userId);

    @Query("SELECT SUM(total_reading_time) FROM reading_progress WHERE user_id = :userId")
    Integer getTotalReadingTime(String userId);

    @Query("DELETE FROM reading_progress WHERE user_id = :userId AND novel_id = :novelId")
    void deleteProgress(String userId, String novelId);

    @Query("DELETE FROM reading_progress WHERE user_id = :userId")
    void deleteAllUserProgress(String userId);
}
