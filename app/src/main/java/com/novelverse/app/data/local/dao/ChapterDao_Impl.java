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
import com.novelverse.app.data.local.entities.ChapterEntity;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unchecked", "deprecation"})
public final class ChapterDao_Impl implements ChapterDao {

    private final RoomDatabase __db;
    private final EntityInsertionAdapter<ChapterEntity> __insertionAdapter;
    private final EntityDeletionOrUpdateAdapter<ChapterEntity> __deletionAdapter;
    private final EntityDeletionOrUpdateAdapter<ChapterEntity> __updateAdapter;

    public ChapterDao_Impl(RoomDatabase db) {
        this.__db = db;
        this.__insertionAdapter = new EntityInsertionAdapter<ChapterEntity>(db) {
            @Override public String createQuery() {
                return "INSERT OR REPLACE INTO `chapters` (`id`,`novel_id`,`chapter_number`,`title`,`slug`,"
                     + "`content`,`word_count`,`is_published`,`is_free`,`price`,`points_cost`,`published_at`,"
                     + "`created_at`,`updated_at`,`total_views`,`total_comments`,`average_read_time`,"
                     + "`is_downloaded`,`is_locked`,`synced_at`,`is_dirty`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            }
            @Override public void bind(SupportSQLiteStatement s, ChapterEntity e) { bindChapter(s, e); }
        };
        this.__deletionAdapter = new EntityDeletionOrUpdateAdapter<ChapterEntity>(db) {
            @Override public String createQuery() { return "DELETE FROM `chapters` WHERE `id` = ?"; }
            @Override public void bind(SupportSQLiteStatement s, ChapterEntity e) { s.bindString(1, e.getId()); }
        };
        this.__updateAdapter = new EntityDeletionOrUpdateAdapter<ChapterEntity>(db) {
            @Override public String createQuery() {
                return "UPDATE OR ABORT `chapters` SET `id`=?,`novel_id`=?,`chapter_number`=?,`title`=?,`slug`=?,"
                     + "`content`=?,`word_count`=?,`is_published`=?,`is_free`=?,`price`=?,`points_cost`=?,`published_at`=?,"
                     + "`created_at`=?,`updated_at`=?,`total_views`=?,`total_comments`=?,`average_read_time`=?,"
                     + "`is_downloaded`=?,`is_locked`=?,`synced_at`=?,`is_dirty`=? WHERE `id`=?";
            }
            @Override public void bind(SupportSQLiteStatement s, ChapterEntity e) { bindChapter(s, e); s.bindString(22, e.getId()); }
        };
    }

    private void bindChapter(SupportSQLiteStatement s, ChapterEntity e) {
        s.bindString(1, e.getId());
        if (e.getNovelId() == null) s.bindNull(2); else s.bindString(2, e.getNovelId());
        if (e.getChapterNumber() == null) s.bindNull(3); else s.bindLong(3, e.getChapterNumber());
        if (e.getTitle() == null) s.bindNull(4); else s.bindString(4, e.getTitle());
        if (e.getSlug() == null) s.bindNull(5); else s.bindString(5, e.getSlug());
        if (e.getContent() == null) s.bindNull(6); else s.bindString(6, e.getContent());
        if (e.getWordCount() == null) s.bindNull(7); else s.bindLong(7, e.getWordCount());
        final Integer pub = Converters.booleanToInteger(e.getIsPublished()); if (pub == null) s.bindNull(8); else s.bindLong(8, pub);
        final Integer free = Converters.booleanToInteger(e.getIsFree()); if (free == null) s.bindNull(9); else s.bindLong(9, free);
        if (e.getPrice() == null) s.bindNull(10); else s.bindDouble(10, e.getPrice());
        if (e.getPointsCost() == null) s.bindNull(11); else s.bindLong(11, e.getPointsCost());
        final Long pubAt = Converters.dateToTimestamp(e.getPublishedAt()); if (pubAt == null) s.bindNull(12); else s.bindLong(12, pubAt);
        final Long cAt = Converters.dateToTimestamp(e.getCreatedAt()); if (cAt == null) s.bindNull(13); else s.bindLong(13, cAt);
        final Long uAt = Converters.dateToTimestamp(e.getUpdatedAt()); if (uAt == null) s.bindNull(14); else s.bindLong(14, uAt);
        if (e.getTotalViews() == null) s.bindNull(15); else s.bindLong(15, e.getTotalViews());
        if (e.getTotalComments() == null) s.bindNull(16); else s.bindLong(16, e.getTotalComments());
        if (e.getAverageReadTime() == null) s.bindNull(17); else s.bindLong(17, e.getAverageReadTime());
        final Integer dl = Converters.booleanToInteger(e.getIsDownloaded()); if (dl == null) s.bindNull(18); else s.bindLong(18, dl);
        final Integer lk = Converters.booleanToInteger(e.getIsLocked()); if (lk == null) s.bindNull(19); else s.bindLong(19, lk);
        final Long sync = Converters.dateToTimestamp(e.getSyncedAt()); if (sync == null) s.bindNull(20); else s.bindLong(20, sync);
        final Integer dirty = Converters.booleanToInteger(e.getIsDirty()); if (dirty == null) s.bindNull(21); else s.bindLong(21, dirty);
    }

    private ChapterEntity cursorToChapter(Cursor c) {
        ChapterEntity e = new ChapterEntity();
        e.setId(c.getString(CursorUtil.getColumnIndexOrThrow(c, "id")));
        e.setNovelId(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "novel_id")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "novel_id")));
        e.setChapterNumber(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "chapter_number")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "chapter_number")));
        e.setTitle(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "title")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "title")));
        e.setSlug(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "slug")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "slug")));
        e.setContent(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "content")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "content")));
        e.setWordCount(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "word_count")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "word_count")));
        e.setIsPublished(Converters.fromInteger(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "is_published")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "is_published"))));
        e.setIsFree(Converters.fromInteger(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "is_free")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "is_free"))));
        e.setPrice(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "price")) ? null : c.getDouble(CursorUtil.getColumnIndexOrThrow(c, "price")));
        e.setPointsCost(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "points_cost")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "points_cost")));
        e.setPublishedAt(Converters.fromTimestamp(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "published_at")) ? null : c.getLong(CursorUtil.getColumnIndexOrThrow(c, "published_at"))));
        e.setCreatedAt(Converters.fromTimestamp(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "created_at")) ? null : c.getLong(CursorUtil.getColumnIndexOrThrow(c, "created_at"))));
        e.setUpdatedAt(Converters.fromTimestamp(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "updated_at")) ? null : c.getLong(CursorUtil.getColumnIndexOrThrow(c, "updated_at"))));
        e.setTotalViews(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "total_views")) ? null : c.getLong(CursorUtil.getColumnIndexOrThrow(c, "total_views")));
        e.setTotalComments(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "total_comments")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "total_comments")));
        e.setAverageReadTime(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "average_read_time")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "average_read_time")));
        e.setIsDownloaded(Converters.fromInteger(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "is_downloaded")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "is_downloaded"))));
        e.setIsLocked(Converters.fromInteger(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "is_locked")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "is_locked"))));
        e.setSyncedAt(Converters.fromTimestamp(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "synced_at")) ? null : c.getLong(CursorUtil.getColumnIndexOrThrow(c, "synced_at"))));
        e.setIsDirty(Converters.fromInteger(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "is_dirty")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "is_dirty"))));
        return e;
    }

    @Override public void insert(ChapterEntity chapter) { __db.assertNotSuspendingTransaction(); __db.beginTransaction(); try { __insertionAdapter.insert(chapter); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void insertAll(List<ChapterEntity> chapters) { __db.assertNotSuspendingTransaction(); __db.beginTransaction(); try { __insertionAdapter.insert(chapters); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void update(ChapterEntity chapter) { __db.assertNotSuspendingTransaction(); __db.beginTransaction(); try { __updateAdapter.handle(chapter); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void delete(ChapterEntity chapter) { __db.assertNotSuspendingTransaction(); __db.beginTransaction(); try { __deletionAdapter.handle(chapter); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void deleteById(String id) { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("DELETE FROM chapters WHERE id = ?"); __db.beginTransaction(); try { s.bindString(1, id); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void updateDownloadStatus(String id, boolean v) { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("UPDATE chapters SET is_downloaded = ? WHERE id = ?"); __db.beginTransaction(); try { s.bindLong(1, v?1:0); s.bindString(2, id); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void updateLockStatus(String id, boolean v) { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("UPDATE chapters SET is_locked = ? WHERE id = ?"); __db.beginTransaction(); try { s.bindLong(1, v?1:0); s.bindString(2, id); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void markAsSynced(String id, long ts) { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("UPDATE chapters SET is_dirty = 0, synced_at = ? WHERE id = ?"); __db.beginTransaction(); try { s.bindLong(1, ts); s.bindString(2, id); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void deleteNonDownloadedChapters(String novelId) { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("DELETE FROM chapters WHERE novel_id = ? AND is_downloaded = 0"); __db.beginTransaction(); try { s.bindString(1, novelId); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void deleteAll() { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("DELETE FROM chapters"); __db.beginTransaction(); try { s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }

    @Override public ChapterEntity getChapterByIdSync(String id) { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM chapters WHERE id = ?", 1); q.bindString(1, id); final Cursor c = DBUtil.query(__db, q, false, null); try { return c.moveToFirst() ? cursorToChapter(c) : null; } finally { c.close(); q.release(); } }
    @Override public List<ChapterEntity> getChaptersByNovelSync(String novelId) { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM chapters WHERE novel_id = ? AND is_published = 1 ORDER BY chapter_number ASC", 1); q.bindString(1, novelId); final Cursor c = DBUtil.query(__db, q, false, null); try { List<ChapterEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToChapter(c)); return r; } finally { c.close(); q.release(); } }
    @Override public ChapterEntity getChapterByNumber(String novelId, int num) { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM chapters WHERE novel_id = ? AND chapter_number = ?", 2); q.bindString(1, novelId); q.bindLong(2, num); final Cursor c = DBUtil.query(__db, q, false, null); try { return c.moveToFirst() ? cursorToChapter(c) : null; } finally { c.close(); q.release(); } }
    @Override public List<ChapterEntity> getFreeChapters(String novelId) { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM chapters WHERE novel_id = ? AND is_free = 1 AND is_published = 1 ORDER BY chapter_number ASC", 1); q.bindString(1, novelId); final Cursor c = DBUtil.query(__db, q, false, null); try { List<ChapterEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToChapter(c)); return r; } finally { c.close(); q.release(); } }
    @Override public List<ChapterEntity> getDownloadedChapters(String novelId) { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM chapters WHERE novel_id = ? AND is_downloaded = 1", 1); q.bindString(1, novelId); final Cursor c = DBUtil.query(__db, q, false, null); try { List<ChapterEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToChapter(c)); return r; } finally { c.close(); q.release(); } }
    @Override public List<ChapterEntity> getDirtyChapters() { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM chapters WHERE is_dirty = 1", 0); final Cursor c = DBUtil.query(__db, q, false, null); try { List<ChapterEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToChapter(c)); return r; } finally { c.close(); q.release(); } }
    @Override public int getPublishedChapterCount(String novelId) { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT COUNT(*) FROM chapters WHERE novel_id = ? AND is_published = 1", 1); q.bindString(1, novelId); final Cursor c = DBUtil.query(__db, q, false, null); try { return c.moveToFirst() ? c.getInt(0) : 0; } finally { c.close(); q.release(); } }
    @Override public Integer getMaxChapterNumber(String novelId) { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT MAX(chapter_number) FROM chapters WHERE novel_id = ?", 1); q.bindString(1, novelId); final Cursor c = DBUtil.query(__db, q, false, null); try { return (c.moveToFirst() && !c.isNull(0)) ? c.getInt(0) : null; } finally { c.close(); q.release(); } }

    @Override public LiveData<ChapterEntity> getChapterById(String id) { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM chapters WHERE id = ?", 1); q.bindString(1, id); return __db.getInvalidationTracker().createLiveData(new String[]{"chapters"}, false, () -> { final Cursor c = DBUtil.query(__db, q, false, null); try { return c.moveToFirst() ? cursorToChapter(c) : null; } finally { c.close(); } }); }
    @Override public LiveData<List<ChapterEntity>> getChaptersByNovel(String novelId) { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM chapters WHERE novel_id = ? AND is_published = 1 ORDER BY chapter_number ASC", 1); q.bindString(1, novelId); return __db.getInvalidationTracker().createLiveData(new String[]{"chapters"}, false, () -> { final Cursor c = DBUtil.query(__db, q, false, null); try { List<ChapterEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToChapter(c)); return r; } finally { c.close(); } }); }
}
