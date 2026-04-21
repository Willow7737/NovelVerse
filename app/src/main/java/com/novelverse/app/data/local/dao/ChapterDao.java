package com.novelverse.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.novelverse.app.data.local.entities.ChapterEntity;

import java.util.List;

/**
 * DAO for Chapter operations
 */
@Dao
public interface ChapterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ChapterEntity chapter);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ChapterEntity> chapters);

    @Update
    void update(ChapterEntity chapter);

    @Delete
    void delete(ChapterEntity chapter);

    @Query("DELETE FROM chapters WHERE id = :chapterId")
    void deleteById(String chapterId);

    @Query("SELECT * FROM chapters WHERE id = :chapterId")
    LiveData<ChapterEntity> getChapterById(String chapterId);

    @Query("SELECT * FROM chapters WHERE id = :chapterId")
    ChapterEntity getChapterByIdSync(String chapterId);

    @Query("SELECT * FROM chapters WHERE novel_id = :novelId AND is_published = 1 ORDER BY chapter_number ASC")
    LiveData<List<ChapterEntity>> getChaptersByNovel(String novelId);

    @Query("SELECT * FROM chapters WHERE novel_id = :novelId AND is_published = 1 ORDER BY chapter_number ASC")
    List<ChapterEntity> getChaptersByNovelSync(String novelId);

    @Query("SELECT * FROM chapters WHERE novel_id = :novelId AND chapter_number = :chapterNumber")
    ChapterEntity getChapterByNumber(String novelId, int chapterNumber);

    @Query("SELECT * FROM chapters WHERE novel_id = :novelId AND is_free = 1 AND is_published = 1 ORDER BY chapter_number ASC")
    List<ChapterEntity> getFreeChapters(String novelId);

    @Query("SELECT COUNT(*) FROM chapters WHERE novel_id = :novelId AND is_published = 1")
    int getPublishedChapterCount(String novelId);

    @Query("SELECT MAX(chapter_number) FROM chapters WHERE novel_id = :novelId")
    Integer getMaxChapterNumber(String novelId);

    @Query("UPDATE chapters SET is_downloaded = :isDownloaded WHERE id = :chapterId")
    void updateDownloadStatus(String chapterId, boolean isDownloaded);

    @Query("UPDATE chapters SET is_locked = :isLocked WHERE id = :chapterId")
    void updateLockStatus(String chapterId, boolean isLocked);

    @Query("SELECT * FROM chapters WHERE novel_id = :novelId AND is_downloaded = 1")
    List<ChapterEntity> getDownloadedChapters(String novelId);

    @Query("SELECT * FROM chapters WHERE is_dirty = 1")
    List<ChapterEntity> getDirtyChapters();

    @Query("UPDATE chapters SET is_dirty = 0, synced_at = :timestamp WHERE id = :chapterId")
    void markAsSynced(String chapterId, long timestamp);

    @Query("DELETE FROM chapters WHERE novel_id = :novelId AND is_downloaded = 0")
    void deleteNonDownloadedChapters(String novelId);

    @Query("DELETE FROM chapters")
    void deleteAll();
}
