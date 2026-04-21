package com.novelverse.app.data.local.dao;

import android.database.Cursor;
import androidx.lifecycle.LiveData;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;

import com.novelverse.app.data.local.database.Converters;
import com.novelverse.app.data.local.entities.BookmarkEntity;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unchecked", "deprecation"})
public final class BookmarkDao_Impl implements BookmarkDao {

    private final RoomDatabase __db;
    private final EntityInsertionAdapter<BookmarkEntity> __insertionAdapter;
    private final EntityDeletionOrUpdateAdapter<BookmarkEntity> __deletionAdapter;
    private final EntityDeletionOrUpdateAdapter<BookmarkEntity> __updateAdapter;

    public BookmarkDao_Impl(RoomDatabase db) {
        this.__db = db;
        this.__insertionAdapter = new EntityInsertionAdapter<BookmarkEntity>(db) {
            @Override public String createQuery() {
                return "INSERT OR REPLACE INTO `bookmarks` (`id`,`user_id`,`novel_id`,`chapter_id`,"
                     + "`position`,`note`,`is_favorite`,`created_at`,`updated_at`,`synced_at`,`is_dirty`)"
                     + " VALUES (?,?,?,?,?,?,?,?,?,?,?)";
            }
            @Override public void bind(SupportSQLiteStatement s, BookmarkEntity e) { bindBM(s, e); }
        };
        this.__deletionAdapter = new EntityDeletionOrUpdateAdapter<BookmarkEntity>(db) {
            @Override public String createQuery() { return "DELETE FROM `bookmarks` WHERE `id` = ?"; }
            @Override public void bind(SupportSQLiteStatement s, BookmarkEntity e) { s.bindString(1, e.getId()); }
        };
        this.__updateAdapter = new EntityDeletionOrUpdateAdapter<BookmarkEntity>(db) {
            @Override public String createQuery() {
                return "UPDATE OR ABORT `bookmarks` SET `id`=?,`user_id`=?,`novel_id`=?,`chapter_id`=?,"
                     + "`position`=?,`note`=?,`is_favorite`=?,`created_at`=?,`updated_at`=?,`synced_at`=?,`is_dirty`=?"
                     + " WHERE `id`=?";
            }
            @Override public void bind(SupportSQLiteStatement s, BookmarkEntity e) { bindBM(s, e); s.bindString(12, e.getId()); }
        };
    }

    private void bindBM(SupportSQLiteStatement s, BookmarkEntity e) {
        s.bindString(1, e.getId());
        if (e.getUserId() == null) s.bindNull(2); else s.bindString(2, e.getUserId());
        if (e.getNovelId() == null) s.bindNull(3); else s.bindString(3, e.getNovelId());
        if (e.getChapterId() == null) s.bindNull(4); else s.bindString(4, e.getChapterId());
        if (e.getPosition() == null) s.bindNull(5); else s.bindLong(5, e.getPosition());
        if (e.getNote() == null) s.bindNull(6); else s.bindString(6, e.getNote());
        final Integer fav = Converters.booleanToInteger(e.getIsFavorite()); if (fav == null) s.bindNull(7); else s.bindLong(7, fav);
        final Long cAt = Converters.dateToTimestamp(e.getCreatedAt()); if (cAt == null) s.bindNull(8); else s.bindLong(8, cAt);
        final Long uAt = Converters.dateToTimestamp(e.getUpdatedAt()); if (uAt == null) s.bindNull(9); else s.bindLong(9, uAt);
        final Long sync = Converters.dateToTimestamp(e.getSyncedAt()); if (sync == null) s.bindNull(10); else s.bindLong(10, sync);
        final Integer dirty = Converters.booleanToInteger(e.getIsDirty()); if (dirty == null) s.bindNull(11); else s.bindLong(11, dirty);
    }

    private BookmarkEntity cursorToBM(Cursor c) {
        BookmarkEntity e = new BookmarkEntity();
        e.setId(c.getString(CursorUtil.getColumnIndexOrThrow(c, "id")));
        e.setUserId(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "user_id")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "user_id")));
        e.setNovelId(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "novel_id")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "novel_id")));
        e.setChapterId(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "chapter_id")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "chapter_id")));
        e.setPosition(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "position")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "position")));
        e.setNote(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "note")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "note")));
        e.setIsFavorite(Converters.fromInteger(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "is_favorite")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "is_favorite"))));
        e.setCreatedAt(Converters.fromTimestamp(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "created_at")) ? null : c.getLong(CursorUtil.getColumnIndexOrThrow(c, "created_at"))));
        e.setUpdatedAt(Converters.fromTimestamp(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "updated_at")) ? null : c.getLong(CursorUtil.getColumnIndexOrThrow(c, "updated_at"))));
        e.setSyncedAt(Converters.fromTimestamp(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "synced_at")) ? null : c.getLong(CursorUtil.getColumnIndexOrThrow(c, "synced_at"))));
        e.setIsDirty(Converters.fromInteger(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "is_dirty")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "is_dirty"))));
        return e;
    }

    @Override public void insert(BookmarkEntity b) { __db.assertNotSuspendingTransaction(); __db.beginTransaction(); try { __insertionAdapter.insert(b); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void update(BookmarkEntity b) { __db.assertNotSuspendingTransaction(); __db.beginTransaction(); try { __updateAdapter.handle(b); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void delete(BookmarkEntity b) { __db.assertNotSuspendingTransaction(); __db.beginTransaction(); try { __deletionAdapter.handle(b); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void deleteById(String id) { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("DELETE FROM bookmarks WHERE id = ?"); __db.beginTransaction(); try { s.bindString(1, id); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void updateNote(String id, String note) { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("UPDATE bookmarks SET note = ?, is_dirty = 1 WHERE id = ?"); __db.beginTransaction(); try { if (note == null) s.bindNull(1); else s.bindString(1, note); s.bindString(2, id); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void updateFavoriteStatus(String id, boolean isFav) { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("UPDATE bookmarks SET is_favorite = ?, is_dirty = 1 WHERE id = ?"); __db.beginTransaction(); try { s.bindLong(1, isFav ? 1 : 0); s.bindString(2, id); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void markAsSynced(String id, long ts) { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("UPDATE bookmarks SET is_dirty = 0, synced_at = ? WHERE id = ?"); __db.beginTransaction(); try { s.bindLong(1, ts); s.bindString(2, id); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void deleteAllUserBookmarks(String userId) { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("DELETE FROM bookmarks WHERE user_id = ?"); __db.beginTransaction(); try { s.bindString(1, userId); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }

    @Override public BookmarkEntity getBookmarkById(String id) { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM bookmarks WHERE id = ?", 1); q.bindString(1, id); final Cursor c = DBUtil.query(__db, q, false, null); try { return c.moveToFirst() ? cursorToBM(c) : null; } finally { c.close(); q.release(); } }
    @Override public List<BookmarkEntity> getBookmarksForNovel(String userId, String novelId) { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM bookmarks WHERE user_id = ? AND novel_id = ? ORDER BY position ASC", 2); q.bindString(1, userId); q.bindString(2, novelId); final Cursor c = DBUtil.query(__db, q, false, null); try { List<BookmarkEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToBM(c)); return r; } finally { c.close(); q.release(); } }
    @Override public List<BookmarkEntity> getBookmarksForChapter(String userId, String chapterId) { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM bookmarks WHERE user_id = ? AND chapter_id = ? ORDER BY position ASC", 2); q.bindString(1, userId); q.bindString(2, chapterId); final Cursor c = DBUtil.query(__db, q, false, null); try { List<BookmarkEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToBM(c)); return r; } finally { c.close(); q.release(); } }
    @Override public List<BookmarkEntity> getDirtyBookmarks() { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM bookmarks WHERE is_dirty = 1", 0); final Cursor c = DBUtil.query(__db, q, false, null); try { List<BookmarkEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToBM(c)); return r; } finally { c.close(); q.release(); } }
    @Override public int getBookmarkCountForNovel(String userId, String novelId) { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT COUNT(*) FROM bookmarks WHERE user_id = ? AND novel_id = ?", 2); q.bindString(1, userId); q.bindString(2, novelId); final Cursor c = DBUtil.query(__db, q, false, null); try { return c.moveToFirst() ? c.getInt(0) : 0; } finally { c.close(); q.release(); } }

    @Override public LiveData<List<BookmarkEntity>> getBookmarksForUser(String userId) { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM bookmarks WHERE user_id = ? ORDER BY created_at DESC", 1); q.bindString(1, userId); return __db.getInvalidationTracker().createLiveData(new String[]{"bookmarks"}, false, () -> { final Cursor c = DBUtil.query(__db, q, false, null); try { List<BookmarkEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToBM(c)); return r; } finally { c.close(); } }); }
    @Override public LiveData<List<BookmarkEntity>> getFavoriteBookmarks(String userId) { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM bookmarks WHERE user_id = ? AND is_favorite = 1 ORDER BY created_at DESC", 1); q.bindString(1, userId); return __db.getInvalidationTracker().createLiveData(new String[]{"bookmarks"}, false, () -> { final Cursor c = DBUtil.query(__db, q, false, null); try { List<BookmarkEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToBM(c)); return r; } finally { c.close(); } }); }
}
