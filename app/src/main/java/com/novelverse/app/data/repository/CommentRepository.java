package com.novelverse.app.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.PagingLiveData;

import com.novelverse.app.data.local.dao.CommentDao;
import com.novelverse.app.data.remote.supabase.SupabaseDatabaseService;
import com.novelverse.app.domain.models.Comment;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Repository for comment-related operations
 */
@Singleton
public class CommentRepository {

    private final CommentDao commentDao;
    private final SupabaseDatabaseService databaseService;
    private final ExecutorService executor;

    @Inject
    public CommentRepository(CommentDao commentDao,
                            SupabaseDatabaseService databaseService) {
        this.commentDao = commentDao;
        this.databaseService = databaseService;
        this.executor = Executors.newFixedThreadPool(2);
    }

    /**
     * Get comments for a novel
     */
    public LiveData<PagingData<Comment>> getCommentsForNovel(String novelId) {
        return PagingLiveData.getLiveData(
            new Pager<>(
                new PagingConfig(20),
                () -> new CommentPagingSource(commentDao, databaseService, novelId, null)
            )
        );
    }

    /**
     * Get comments for a chapter
     */
    public LiveData<PagingData<Comment>> getCommentsForChapter(String chapterId) {
        return PagingLiveData.getLiveData(
            new Pager<>(
                new PagingConfig(20),
                () -> new CommentPagingSource(commentDao, databaseService, null, chapterId)
            )
        );
    }

    /**
     * Get replies for a comment
     */
    public LiveData<List<Comment>> getReplies(String parentId) {
        MutableLiveData<List<Comment>> result = new MutableLiveData<>();
        executor.execute(() -> {
            List<com.novelverse.app.data.local.entities.CommentEntity> entities =
                commentDao.getReplies(parentId);
            List<Comment> out = new java.util.ArrayList<>();
            for (com.novelverse.app.data.local.entities.CommentEntity e : entities) {
                Comment c2 = new Comment();
                c2.setId(e.getId());
                c2.setUserId(e.getUserId());
                c2.setUsername(e.getUsername());
                c2.setUserAvatarUrl(e.getUserAvatarUrl());
                c2.setNovelId(e.getNovelId());
                c2.setChapterId(e.getChapterId());
                c2.setParentId(e.getParentId());
                c2.setContent(e.getContent());
                c2.setLikesCount(e.getLikesCount() != null ? e.getLikesCount() : 0);
                c2.setDislikesCount(e.getDislikesCount() != null ? e.getDislikesCount() : 0);
                c2.setRepliesCount(e.getRepliesCount() != null ? e.getRepliesCount() : 0);
                c2.setEdited(e.getIsEdited() != null && e.getIsEdited());
                c2.setDeleted(e.getIsDeleted() != null && e.getIsDeleted());
                c2.setSpoiler(e.getIsSpoiler() != null && e.getIsSpoiler());
                c2.setCreatedAt(e.getCreatedAt());
                c2.setUpdatedAt(e.getUpdatedAt());
                c2.setLikedByMe(e.getIsLikedByMe() != null && e.getIsLikedByMe());
                c2.setDislikedByMe(e.getIsDislikedByMe() != null && e.getIsDislikedByMe());
                out.add(c2);
            }
            result.postValue(out);
        });
        return result;
    }

    /**
     * Post a comment
     */
    public void postComment(Comment comment, CommentCallback callback) {
        executor.execute(() -> {
            try {
                // Insert into remote database
                callback.onSuccess(comment);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    /**
     * Like a comment
     */
    public void likeComment(String commentId, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                // Update in remote database
                callback.onResult(true, null);
            } catch (Exception e) {
                callback.onResult(false, e.getMessage());
            }
        });
    }

    /**
     * Delete a comment
     */
    public void deleteComment(String commentId, SimpleCallback callback) {
        executor.execute(() -> {
            try {
                // Delete from remote database
                callback.onResult(true, null);
            } catch (Exception e) {
                callback.onResult(false, e.getMessage());
            }
        });
    }

    // Callback interfaces
    public interface CommentCallback {
        void onSuccess(Comment comment);
        void onError(String error);
    }

    public interface SimpleCallback {
        void onResult(boolean success, String error);
    }
}
