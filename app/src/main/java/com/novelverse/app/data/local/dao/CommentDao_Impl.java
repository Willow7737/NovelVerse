package com.novelverse.app.data.local.dao;

import android.database.Cursor;
import androidx.lifecycle.LiveData;
import androidx.paging.PagingSource;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.paging.LimitOffsetPagingSource;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;

import com.novelverse.app.data.local.database.Converters;
import com.novelverse.app.data.local.entities.CommentEntity;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unchecked", "deprecation"})
public final class CommentDao_Impl implements CommentDao {

    private final RoomDatabase __db;
    private final EntityInsertionAdapter<CommentEntity> __insertionAdapter;
    private final EntityDeletionOrUpdateAdapter<CommentEntity> __deletionAdapter;
    private final EntityDeletionOrUpdateAdapter<CommentEntity> __updateAdapter;

    public CommentDao_Impl(RoomDatabase db) {
        this.__db = db;
        this.__insertionAdapter = new EntityInsertionAdapter<CommentEntity>(db) {
            @Override public String createQuery() {
                return "INSERT OR REPLACE INTO `comments` (`id`,`user_id`,`username`,`user_avatar_url`,"
                     + "`novel_id`,`chapter_id`,`parent_id`,`content`,`likes_count`,`dislikes_count`,"
                     + "`replies_count`,`is_edited`,`is_deleted`,`is_spoiler`,`created_at`,`updated_at`,"
                     + "`synced_at`,`is_dirty`,`is_liked_by_me`,`is_disliked_by_me`)"
                     + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            }
            @Override public void bind(SupportSQLiteStatement s, CommentEntity e) { bindComment(s, e); }
        };
        this.__deletionAdapter = new EntityDeletionOrUpdateAdapter<CommentEntity>(db) {
            @Override public String createQuery() { return "DELETE FROM `comments` WHERE `id` = ?"; }
            @Override public void bind(SupportSQLiteStatement s, CommentEntity e) { s.bindString(1, e.getId()); }
        };
        this.__updateAdapter = new EntityDeletionOrUpdateAdapter<CommentEntity>(db) {
            @Override public String createQuery() {
                return "UPDATE OR ABORT `comments` SET `id`=?,`user_id`=?,`username`=?,`user_avatar_url`=?,"
                     + "`novel_id`=?,`chapter_id`=?,`parent_id`=?,`content`=?,`likes_count`=?,`dislikes_count`=?,"
                     + "`replies_count`=?,`is_edited`=?,`is_deleted`=?,`is_spoiler`=?,`created_at`=?,`updated_at`=?,"
                     + "`synced_at`=?,`is_dirty`=?,`is_liked_by_me`=?,`is_disliked_by_me`=?"
                     + " WHERE `id`=?";
            }
            @Override public void bind(SupportSQLiteStatement s, CommentEntity e) { bindComment(s, e); s.bindString(21, e.getId()); }
        };
    }

    private void bindComment(SupportSQLiteStatement s, CommentEntity e) {
        s.bindString(1, e.getId());
        if (e.getUserId() == null) s.bindNull(2); else s.bindString(2, e.getUserId());
        if (e.getUsername() == null) s.bindNull(3); else s.bindString(3, e.getUsername());
        if (e.getUserAvatarUrl() == null) s.bindNull(4); else s.bindString(4, e.getUserAvatarUrl());
        if (e.getNovelId() == null) s.bindNull(5); else s.bindString(5, e.getNovelId());
        if (e.getChapterId() == null) s.bindNull(6); else s.bindString(6, e.getChapterId());
        if (e.getParentId() == null) s.bindNull(7); else s.bindString(7, e.getParentId());
        if (e.getContent() == null) s.bindNull(8); else s.bindString(8, e.getContent());
        if (e.getLikesCount() == null) s.bindNull(9); else s.bindLong(9, e.getLikesCount());
        if (e.getDislikesCount() == null) s.bindNull(10); else s.bindLong(10, e.getDislikesCount());
        if (e.getRepliesCount() == null) s.bindNull(11); else s.bindLong(11, e.getRepliesCount());
        final Integer edited = Converters.booleanToInteger(e.getIsEdited()); if (edited == null) s.bindNull(12); else s.bindLong(12, edited);
        final Integer deleted = Converters.booleanToInteger(e.getIsDeleted()); if (deleted == null) s.bindNull(13); else s.bindLong(13, deleted);
        final Integer spoiler = Converters.booleanToInteger(e.getIsSpoiler()); if (spoiler == null) s.bindNull(14); else s.bindLong(14, spoiler);
        final Long cAt = Converters.dateToTimestamp(e.getCreatedAt()); if (cAt == null) s.bindNull(15); else s.bindLong(15, cAt);
        final Long uAt = Converters.dateToTimestamp(e.getUpdatedAt()); if (uAt == null) s.bindNull(16); else s.bindLong(16, uAt);
        final Long sync = Converters.dateToTimestamp(e.getSyncedAt()); if (sync == null) s.bindNull(17); else s.bindLong(17, sync);
        final Integer dirty = Converters.booleanToInteger(e.getIsDirty()); if (dirty == null) s.bindNull(18); else s.bindLong(18, dirty);
        final Integer liked = Converters.booleanToInteger(e.getIsLikedByMe()); if (liked == null) s.bindNull(19); else s.bindLong(19, liked);
        final Integer disliked = Converters.booleanToInteger(e.getIsDislikedByMe()); if (disliked == null) s.bindNull(20); else s.bindLong(20, disliked);
    }

    private CommentEntity cursorToComment(Cursor c) {
        CommentEntity e = new CommentEntity();
        e.setId(c.getString(CursorUtil.getColumnIndexOrThrow(c, "id")));
        e.setUserId(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "user_id")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "user_id")));
        e.setUsername(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "username")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "username")));
        e.setUserAvatarUrl(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "user_avatar_url")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "user_avatar_url")));
        e.setNovelId(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "novel_id")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "novel_id")));
        e.setChapterId(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "chapter_id")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "chapter_id")));
        e.setParentId(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "parent_id")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "parent_id")));
        e.setContent(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "content")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "content")));
        e.setLikesCount(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "likes_count")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "likes_count")));
        e.setDislikesCount(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "dislikes_count")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "dislikes_count")));
        e.setRepliesCount(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "replies_count")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "replies_count")));
        e.setIsEdited(Converters.fromInteger(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "is_edited")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "is_edited"))));
        e.setIsDeleted(Converters.fromInteger(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "is_deleted")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "is_deleted"))));
        e.setIsSpoiler(Converters.fromInteger(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "is_spoiler")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "is_spoiler"))));
        e.setCreatedAt(Converters.fromTimestamp(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "created_at")) ? null : c.getLong(CursorUtil.getColumnIndexOrThrow(c, "created_at"))));
        e.setUpdatedAt(Converters.fromTimestamp(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "updated_at")) ? null : c.getLong(CursorUtil.getColumnIndexOrThrow(c, "updated_at"))));
        e.setSyncedAt(Converters.fromTimestamp(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "synced_at")) ? null : c.getLong(CursorUtil.getColumnIndexOrThrow(c, "synced_at"))));
        e.setIsDirty(Converters.fromInteger(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "is_dirty")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "is_dirty"))));
        e.setIsLikedByMe(Converters.fromInteger(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "is_liked_by_me")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "is_liked_by_me"))));
        e.setIsDislikedByMe(Converters.fromInteger(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "is_disliked_by_me")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "is_disliked_by_me"))));
        return e;
    }

    @Override public void insert(CommentEntity c) { __db.assertNotSuspendingTransaction(); __db.beginTransaction(); try { __insertionAdapter.insert(c); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void insertAll(List<CommentEntity> cs) { __db.assertNotSuspendingTransaction(); __db.beginTransaction(); try { __insertionAdapter.insert(cs); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void update(CommentEntity c) { __db.assertNotSuspendingTransaction(); __db.beginTransaction(); try { __updateAdapter.handle(c); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void delete(CommentEntity c) { __db.assertNotSuspendingTransaction(); __db.beginTransaction(); try { __deletionAdapter.handle(c); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void deleteById(String id) { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("DELETE FROM comments WHERE id = ?"); __db.beginTransaction(); try { s.bindString(1, id); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void incrementLikes(String id) { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("UPDATE comments SET likes_count = likes_count + 1, is_liked_by_me = 1 WHERE id = ?"); __db.beginTransaction(); try { s.bindString(1, id); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void decrementLikes(String id) { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("UPDATE comments SET likes_count = likes_count - 1, is_liked_by_me = 0 WHERE id = ?"); __db.beginTransaction(); try { s.bindString(1, id); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void incrementDislikes(String id) { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("UPDATE comments SET dislikes_count = dislikes_count + 1, is_disliked_by_me = 1 WHERE id = ?"); __db.beginTransaction(); try { s.bindString(1, id); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void decrementDislikes(String id) { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("UPDATE comments SET dislikes_count = dislikes_count - 1, is_disliked_by_me = 0 WHERE id = ?"); __db.beginTransaction(); try { s.bindString(1, id); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void editComment(String id, String content, long ts) { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("UPDATE comments SET content = ?, is_edited = 1, updated_at = ?, is_dirty = 1 WHERE id = ?"); __db.beginTransaction(); try { if (content == null) s.bindNull(1); else s.bindString(1, content); s.bindLong(2, ts); s.bindString(3, id); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void softDelete(String id) { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("UPDATE comments SET is_deleted = 1, content = '', is_dirty = 1 WHERE id = ?"); __db.beginTransaction(); try { s.bindString(1, id); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void markAsSynced(String id, long ts) { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("UPDATE comments SET is_dirty = 0, synced_at = ? WHERE id = ?"); __db.beginTransaction(); try { s.bindLong(1, ts); s.bindString(2, id); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void deleteCommentsForNovel(String novelId) { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("DELETE FROM comments WHERE novel_id = ?"); __db.beginTransaction(); try { s.bindString(1, novelId); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void deleteAll() { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("DELETE FROM comments"); __db.beginTransaction(); try { s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }

    @Override public CommentEntity getCommentById(String id) { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM comments WHERE id = ?", 1); q.bindString(1, id); final Cursor c = DBUtil.query(__db, q, false, null); try { return c.moveToFirst() ? cursorToComment(c) : null; } finally { c.close(); q.release(); } }
    @Override public List<CommentEntity> getReplies(String parentId) { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM comments WHERE parent_id = ? AND is_deleted = 0 ORDER BY created_at ASC", 1); q.bindString(1, parentId); final Cursor c = DBUtil.query(__db, q, false, null); try { List<CommentEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToComment(c)); return r; } finally { c.close(); q.release(); } }
    @Override public List<CommentEntity> getDirtyComments() { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM comments WHERE is_dirty = 1", 0); final Cursor c = DBUtil.query(__db, q, false, null); try { List<CommentEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToComment(c)); return r; } finally { c.close(); q.release(); } }

    @Override public LiveData<List<CommentEntity>> getCommentsByUser(String userId) { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM comments WHERE user_id = ? ORDER BY created_at DESC", 1); q.bindString(1, userId); return __db.getInvalidationTracker().createLiveData(new String[]{"comments"}, false, () -> { final Cursor c = DBUtil.query(__db, q, false, null); try { List<CommentEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToComment(c)); return r; } finally { c.close(); } }); }

    @Override public PagingSource<Integer, CommentEntity> getCommentsForNovel(String novelId) { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM comments WHERE novel_id = ? AND parent_id IS NULL AND is_deleted = 0 ORDER BY created_at DESC", 1); q.bindString(1, novelId); return new LimitOffsetPagingSource<CommentEntity>(q, __db, "comments") { @Override protected List<CommentEntity> convertRows(Cursor c) { List<CommentEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToComment(c)); return r; } }; }
    @Override public PagingSource<Integer, CommentEntity> getCommentsForChapter(String chapterId) { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM comments WHERE chapter_id = ? AND parent_id IS NULL AND is_deleted = 0 ORDER BY created_at DESC", 1); q.bindString(1, chapterId); return new LimitOffsetPagingSource<CommentEntity>(q, __db, "comments") { @Override protected List<CommentEntity> convertRows(Cursor c) { List<CommentEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToComment(c)); return r; } }; }
}
