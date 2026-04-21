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
import com.novelverse.app.data.local.entities.ReadingProgressEntity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SuppressWarnings({"unchecked", "deprecation"})
public final class ReadingProgressDao_Impl implements ReadingProgressDao {

    private final RoomDatabase __db;
    private final EntityInsertionAdapter<ReadingProgressEntity> __insertionAdapter;
    private final EntityDeletionOrUpdateAdapter<ReadingProgressEntity> __deletionAdapter;
    private final EntityDeletionOrUpdateAdapter<ReadingProgressEntity> __updateAdapter;

    public ReadingProgressDao_Impl(RoomDatabase db) {
        this.__db = db;
        this.__insertionAdapter = new EntityInsertionAdapter<ReadingProgressEntity>(db) {
            @Override public String createQuery() {
                return "INSERT OR REPLACE INTO `reading_progress` (`id`,`user_id`,`novel_id`,`chapter_id`,"
                     + "`scroll_position`,`progress_percentage`,`is_completed`,`completed_at`,"
                     + "`last_read_at`,`total_reading_time`,`device_info`,`synced_at`,`is_dirty`,`needs_sync`)"
                     + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            }
            @Override public void bind(SupportSQLiteStatement s, ReadingProgressEntity e) { bindRP(s, e); }
        };
        this.__deletionAdapter = new EntityDeletionOrUpdateAdapter<ReadingProgressEntity>(db) {
            @Override public String createQuery() { return "DELETE FROM `reading_progress` WHERE `id` = ?"; }
            @Override public void bind(SupportSQLiteStatement s, ReadingProgressEntity e) { s.bindString(1, e.getId()); }
        };
        this.__updateAdapter = new EntityDeletionOrUpdateAdapter<ReadingProgressEntity>(db) {
            @Override public String createQuery() {
                return "UPDATE OR ABORT `reading_progress` SET `id`=?,`user_id`=?,`novel_id`=?,`chapter_id`=?,"
                     + "`scroll_position`=?,`progress_percentage`=?,`is_completed`=?,`completed_at`=?,"
                     + "`last_read_at`=?,`total_reading_time`=?,`device_info`=?,`synced_at`=?,`is_dirty`=?,`needs_sync`=?"
                     + " WHERE `id`=?";
            }
            @Override public void bind(SupportSQLiteStatement s, ReadingProgressEntity e) { bindRP(s, e); s.bindString(15, e.getId()); }
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Shared bind / cursor helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void bindRP(SupportSQLiteStatement s, ReadingProgressEntity e) {
        s.bindString(1, e.getId());
        if (e.getUserId() == null) s.bindNull(2); else s.bindString(2, e.getUserId());
        if (e.getNovelId() == null) s.bindNull(3); else s.bindString(3, e.getNovelId());
        if (e.getChapterId() == null) s.bindNull(4); else s.bindString(4, e.getChapterId());
        if (e.getScrollPosition() == null) s.bindNull(5); else s.bindLong(5, e.getScrollPosition());
        if (e.getProgressPercentage() == null) s.bindNull(6); else s.bindDouble(6, e.getProgressPercentage());
        final Integer comp = Converters.booleanToInteger(e.getIsCompleted()); if (comp == null) s.bindNull(7); else s.bindLong(7, comp);
        final Long compAt = Converters.dateToTimestamp(e.getCompletedAt()); if (compAt == null) s.bindNull(8); else s.bindLong(8, compAt);
        final Long lastRead = Converters.dateToTimestamp(e.getLastReadAt()); if (lastRead == null) s.bindNull(9); else s.bindLong(9, lastRead);
        if (e.getTotalReadingTime() == null) s.bindNull(10); else s.bindLong(10, e.getTotalReadingTime());
        if (e.getDeviceInfo() == null) s.bindNull(11); else s.bindString(11, e.getDeviceInfo());
        final Long sync = Converters.dateToTimestamp(e.getSyncedAt()); if (sync == null) s.bindNull(12); else s.bindLong(12, sync);
        final Integer dirty = Converters.booleanToInteger(e.getIsDirty()); if (dirty == null) s.bindNull(13); else s.bindLong(13, dirty);
        final Integer ns = Converters.booleanToInteger(e.getNeedsSync()); if (ns == null) s.bindNull(14); else s.bindLong(14, ns);
    }

    private ReadingProgressEntity cursorToRP(Cursor c) {
        ReadingProgressEntity e = new ReadingProgressEntity();
        e.setId(c.getString(CursorUtil.getColumnIndexOrThrow(c, "id")));
        e.setUserId(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "user_id")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "user_id")));
        e.setNovelId(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "novel_id")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "novel_id")));
        e.setChapterId(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "chapter_id")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "chapter_id")));
        e.setScrollPosition(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "scroll_position")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "scroll_position")));
        e.setProgressPercentage(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "progress_percentage")) ? null : c.getDouble(CursorUtil.getColumnIndexOrThrow(c, "progress_percentage")));
        e.setIsCompleted(Converters.fromInteger(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "is_completed")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "is_completed"))));
        e.setCompletedAt(Converters.fromTimestamp(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "completed_at")) ? null : c.getLong(CursorUtil.getColumnIndexOrThrow(c, "completed_at"))));
        e.setLastReadAt(Converters.fromTimestamp(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "last_read_at")) ? null : c.getLong(CursorUtil.getColumnIndexOrThrow(c, "last_read_at"))));
        e.setTotalReadingTime(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "total_reading_time")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "total_reading_time")));
        e.setDeviceInfo(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "device_info")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "device_info")));
        e.setSyncedAt(Converters.fromTimestamp(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "synced_at")) ? null : c.getLong(CursorUtil.getColumnIndexOrThrow(c, "synced_at"))));
        e.setIsDirty(Converters.fromInteger(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "is_dirty")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "is_dirty"))));
        e.setNeedsSync(Converters.fromInteger(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "needs_sync")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "needs_sync"))));
        return e;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mutations (write)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void insert(ReadingProgressEntity p) {
        __db.assertNotSuspendingTransaction();
        __db.beginTransaction();
        try { __insertionAdapter.insert(p); __db.setTransactionSuccessful(); }
        finally { __db.endTransaction(); }
    }

    /**
     * INSERT OR REPLACE keyed on (user_id, novel_id) natural key.
     * Delegates to the same INSERT OR REPLACE adapter so the behaviour is identical to insert().
     */
    @Override
    public void insertOrUpdate(ReadingProgressEntity entity) {
        __db.assertNotSuspendingTransaction();
        __db.beginTransaction();
        try { __insertionAdapter.insert(entity); __db.setTransactionSuccessful(); }
        finally { __db.endTransaction(); }
    }

    @Override
    public void update(ReadingProgressEntity p) {
        __db.assertNotSuspendingTransaction();
        __db.beginTransaction();
        try { __updateAdapter.handle(p); __db.setTransactionSuccessful(); }
        finally { __db.endTransaction(); }
    }

    @Override
    public void delete(ReadingProgressEntity p) {
        __db.assertNotSuspendingTransaction();
        __db.beginTransaction();
        try { __deletionAdapter.handle(p); __db.setTransactionSuccessful(); }
        finally { __db.endTransaction(); }
    }

    @Override
    public void deleteById(String id) {
        __db.assertNotSuspendingTransaction();
        final SupportSQLiteStatement s = __db.compileStatement("DELETE FROM reading_progress WHERE id = ?");
        __db.beginTransaction();
        try { s.bindString(1, id); s.executeUpdateDelete(); __db.setTransactionSuccessful(); }
        finally { __db.endTransaction(); }
    }

    @Override
    public void updateProgress(String userId, String novelId, int pos, double pct, long ts) {
        __db.assertNotSuspendingTransaction();
        final SupportSQLiteStatement s = __db.compileStatement(
            "UPDATE reading_progress SET scroll_position = ?, progress_percentage = ?, " +
            "last_read_at = ?, is_dirty = 1, needs_sync = 1 WHERE user_id = ? AND novel_id = ?");
        __db.beginTransaction();
        try {
            s.bindLong(1, pos); s.bindDouble(2, pct); s.bindLong(3, ts);
            s.bindString(4, userId); s.bindString(5, novelId);
            s.executeUpdateDelete(); __db.setTransactionSuccessful();
        } finally { __db.endTransaction(); }
    }

    @Override
    public void updateCurrentChapter(String userId, String novelId, String chapterId, long ts) {
        __db.assertNotSuspendingTransaction();
        final SupportSQLiteStatement s = __db.compileStatement(
            "UPDATE reading_progress SET chapter_id = ?, scroll_position = 0, " +
            "progress_percentage = 0, last_read_at = ?, is_dirty = 1, needs_sync = 1 " +
            "WHERE user_id = ? AND novel_id = ?");
        __db.beginTransaction();
        try {
            s.bindString(1, chapterId); s.bindLong(2, ts);
            s.bindString(3, userId); s.bindString(4, novelId);
            s.executeUpdateDelete(); __db.setTransactionSuccessful();
        } finally { __db.endTransaction(); }
    }

    @Override
    public void markAsCompleted(String userId, String novelId, long ts) {
        __db.assertNotSuspendingTransaction();
        final SupportSQLiteStatement s = __db.compileStatement(
            "UPDATE reading_progress SET is_completed = 1, completed_at = ?, " +
            "is_dirty = 1, needs_sync = 1 WHERE user_id = ? AND novel_id = ?");
        __db.beginTransaction();
        try { s.bindLong(1, ts); s.bindString(2, userId); s.bindString(3, novelId); s.executeUpdateDelete(); __db.setTransactionSuccessful(); }
        finally { __db.endTransaction(); }
    }

    @Override
    public void addReadingTime(String userId, String novelId, int seconds) {
        __db.assertNotSuspendingTransaction();
        final SupportSQLiteStatement s = __db.compileStatement(
            "UPDATE reading_progress SET total_reading_time = total_reading_time + ? " +
            "WHERE user_id = ? AND novel_id = ?");
        __db.beginTransaction();
        try { s.bindLong(1, seconds); s.bindString(2, userId); s.bindString(3, novelId); s.executeUpdateDelete(); __db.setTransactionSuccessful(); }
        finally { __db.endTransaction(); }
    }

    /**
     * Clears sync flags after a successful RPC replay.
     * Keyed on (user_id, novel_id) — matches the signature in ReadingProgressRepository.
     */
    @Override
    public void markSynced(String userId, String novelId, Date syncedAt) {
        __db.assertNotSuspendingTransaction();
        final SupportSQLiteStatement s = __db.compileStatement(
            "UPDATE reading_progress SET needs_sync = 0, is_dirty = 0, synced_at = ? " +
            "WHERE user_id = ? AND novel_id = ?");
        __db.beginTransaction();
        try {
            final Long ts = Converters.dateToTimestamp(syncedAt);
            if (ts == null) s.bindNull(1); else s.bindLong(1, ts);
            s.bindString(2, userId);
            s.bindString(3, novelId);
            s.executeUpdateDelete();
            __db.setTransactionSuccessful();
        } finally { __db.endTransaction(); }
    }

    /** Original markAsSynced by surrogate id — kept for backward compat with existing call sites. */
    @Override
    public void markAsSynced(String id, long ts) {
        __db.assertNotSuspendingTransaction();
        final SupportSQLiteStatement s = __db.compileStatement(
            "UPDATE reading_progress SET needs_sync = 0, synced_at = ?, is_dirty = 0 WHERE id = ?");
        __db.beginTransaction();
        try { s.bindLong(1, ts); s.bindString(2, id); s.executeUpdateDelete(); __db.setTransactionSuccessful(); }
        finally { __db.endTransaction(); }
    }

    @Override
    public void deleteProgress(String userId, String novelId) {
        __db.assertNotSuspendingTransaction();
        final SupportSQLiteStatement s = __db.compileStatement("DELETE FROM reading_progress WHERE user_id = ? AND novel_id = ?");
        __db.beginTransaction();
        try { s.bindString(1, userId); s.bindString(2, novelId); s.executeUpdateDelete(); __db.setTransactionSuccessful(); }
        finally { __db.endTransaction(); }
    }

    @Override
    public void deleteAllUserProgress(String userId) {
        __db.assertNotSuspendingTransaction();
        final SupportSQLiteStatement s = __db.compileStatement("DELETE FROM reading_progress WHERE user_id = ?");
        __db.beginTransaction();
        try { s.bindString(1, userId); s.executeUpdateDelete(); __db.setTransactionSuccessful(); }
        finally { __db.endTransaction(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Queries (synchronous)
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public ReadingProgressEntity getProgressById(String id) {
        final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM reading_progress WHERE id = ?", 1);
        q.bindString(1, id);
        final Cursor c = DBUtil.query(__db, q, false, null);
        try { return c.moveToFirst() ? cursorToRP(c) : null; } finally { c.close(); q.release(); }
    }

    @Override
    public ReadingProgressEntity getProgressSync(String userId, String novelId) {
        final RoomSQLiteQuery q = RoomSQLiteQuery.acquire(
            "SELECT * FROM reading_progress WHERE user_id = ? AND novel_id = ?", 2);
        q.bindString(1, userId); q.bindString(2, novelId);
        final Cursor c = DBUtil.query(__db, q, false, null);
        try { return c.moveToFirst() ? cursorToRP(c) : null; } finally { c.close(); q.release(); }
    }

    /**
     * Alias of getProgressSync — used by ReadingProgressRepository for named clarity.
     */
    @Override
    public ReadingProgressEntity getProgressForNovel(String userId, String novelId) {
        final RoomSQLiteQuery q = RoomSQLiteQuery.acquire(
            "SELECT * FROM reading_progress WHERE user_id = ? AND novel_id = ?", 2);
        q.bindString(1, userId); q.bindString(2, novelId);
        final Cursor c = DBUtil.query(__db, q, false, null);
        try { return c.moveToFirst() ? cursorToRP(c) : null; } finally { c.close(); q.release(); }
    }

    @Override
    public List<ReadingProgressEntity> getRecentProgress(String userId, int limit) {
        final RoomSQLiteQuery q = RoomSQLiteQuery.acquire(
            "SELECT * FROM reading_progress WHERE user_id = ? ORDER BY last_read_at DESC LIMIT ?", 2);
        q.bindString(1, userId); q.bindLong(2, limit);
        final Cursor c = DBUtil.query(__db, q, false, null);
        try { List<ReadingProgressEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToRP(c)); return r; }
        finally { c.close(); q.release(); }
    }

    @Override
    public List<ReadingProgressEntity> getCompletedNovels(String userId) {
        final RoomSQLiteQuery q = RoomSQLiteQuery.acquire(
            "SELECT * FROM reading_progress WHERE user_id = ? AND is_completed = 1", 1);
        q.bindString(1, userId);
        final Cursor c = DBUtil.query(__db, q, false, null);
        try { List<ReadingProgressEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToRP(c)); return r; }
        finally { c.close(); q.release(); }
    }

    @Override
    public List<ReadingProgressEntity> getPendingSync() {
        final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM reading_progress WHERE needs_sync = 1", 0);
        final Cursor c = DBUtil.query(__db, q, false, null);
        try { List<ReadingProgressEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToRP(c)); return r; }
        finally { c.close(); q.release(); }
    }

    @Override
    public int getReadingCount(String userId) {
        final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT COUNT(*) FROM reading_progress WHERE user_id = ?", 1);
        q.bindString(1, userId);
        final Cursor c = DBUtil.query(__db, q, false, null);
        try { return c.moveToFirst() ? c.getInt(0) : 0; } finally { c.close(); q.release(); }
    }

    @Override
    public Integer getTotalReadingTime(String userId) {
        final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT SUM(total_reading_time) FROM reading_progress WHERE user_id = ?", 1);
        q.bindString(1, userId);
        final Cursor c = DBUtil.query(__db, q, false, null);
        try { return (c.moveToFirst() && !c.isNull(0)) ? c.getInt(0) : null; } finally { c.close(); q.release(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LiveData queries
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public LiveData<ReadingProgressEntity> getProgress(String userId, String novelId) {
        final RoomSQLiteQuery q = RoomSQLiteQuery.acquire(
            "SELECT * FROM reading_progress WHERE user_id = ? AND novel_id = ?", 2);
        q.bindString(1, userId); q.bindString(2, novelId);
        return __db.getInvalidationTracker().createLiveData(new String[]{"reading_progress"}, false, () -> {
            final Cursor c = DBUtil.query(__db, q, false, null);
            try { return c.moveToFirst() ? cursorToRP(c) : null; } finally { c.close(); }
        });
    }

    @Override
    public LiveData<List<ReadingProgressEntity>> getAllProgressForUser(String userId) {
        final RoomSQLiteQuery q = RoomSQLiteQuery.acquire(
            "SELECT * FROM reading_progress WHERE user_id = ? ORDER BY last_read_at DESC", 1);
        q.bindString(1, userId);
        return __db.getInvalidationTracker().createLiveData(new String[]{"reading_progress"}, false, () -> {
            final Cursor c = DBUtil.query(__db, q, false, null);
            try { List<ReadingProgressEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToRP(c)); return r; }
            finally { c.close(); }
        });
    }
}
