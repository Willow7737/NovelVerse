package com.novelverse.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.paging.PagingSource;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.novelverse.app.data.local.entities.CommentEntity;

import java.util.List;

/**
 * DAO for Comment operations
 */
@Dao
public interface CommentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CommentEntity comment);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CommentEntity> comments);

    @Update
    void update(CommentEntity comment);

    @Delete
    void delete(CommentEntity comment);

    @Query("DELETE FROM comments WHERE id = :commentId")
    void deleteById(String commentId);

    @Query("SELECT * FROM comments WHERE id = :commentId")
    CommentEntity getCommentById(String commentId);

    @Query("SELECT * FROM comments WHERE novel_id = :novelId AND parent_id IS NULL AND is_deleted = 0 " +
           "ORDER BY created_at DESC")
    PagingSource<Integer, CommentEntity> getCommentsForNovel(String novelId);

    @Query("SELECT * FROM comments WHERE chapter_id = :chapterId AND parent_id IS NULL AND is_deleted = 0 " +
           "ORDER BY created_at DESC")
    PagingSource<Integer, CommentEntity> getCommentsForChapter(String chapterId);

    @Query("SELECT * FROM comments WHERE parent_id = :parentId AND is_deleted = 0 ORDER BY created_at ASC")
    List<CommentEntity> getReplies(String parentId);

    @Query("SELECT * FROM comments WHERE user_id = :userId ORDER BY created_at DESC")
    LiveData<List<CommentEntity>> getCommentsByUser(String userId);

    @Query("UPDATE comments SET likes_count = likes_count + 1, is_liked_by_me = 1 WHERE id = :commentId")
    void incrementLikes(String commentId);

    @Query("UPDATE comments SET likes_count = likes_count - 1, is_liked_by_me = 0 WHERE id = :commentId")
    void decrementLikes(String commentId);

    @Query("UPDATE comments SET dislikes_count = dislikes_count + 1, is_disliked_by_me = 1 WHERE id = :commentId")
    void incrementDislikes(String commentId);

    @Query("UPDATE comments SET dislikes_count = dislikes_count - 1, is_disliked_by_me = 0 WHERE id = :commentId")
    void decrementDislikes(String commentId);

    @Query("UPDATE comments SET content = :content, is_edited = 1, updated_at = :timestamp, is_dirty = 1 " +
           "WHERE id = :commentId")
    void editComment(String commentId, String content, long timestamp);

    @Query("UPDATE comments SET is_deleted = 1, content = '', is_dirty = 1 WHERE id = :commentId")
    void softDelete(String commentId);

    @Query("SELECT * FROM comments WHERE is_dirty = 1")
    List<CommentEntity> getDirtyComments();

    @Query("UPDATE comments SET is_dirty = 0, synced_at = :timestamp WHERE id = :commentId")
    void markAsSynced(String commentId, long timestamp);

    @Query("DELETE FROM comments WHERE novel_id = :novelId")
    void deleteCommentsForNovel(String novelId);

    @Query("DELETE FROM comments")
    void deleteAll();
}
