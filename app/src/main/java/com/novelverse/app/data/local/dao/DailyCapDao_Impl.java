package com.novelverse.app.data.local.dao;

import android.database.Cursor;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;

import com.novelverse.app.data.local.entities.DailyCapEntity;

@SuppressWarnings({"unchecked", "deprecation"})
public final class DailyCapDao_Impl implements DailyCapDao {

    private final RoomDatabase __db;
    private final EntityInsertionAdapter<DailyCapEntity> __insertionAdapter;

    public DailyCapDao_Impl(RoomDatabase db) {
        this.__db = db;
        this.__insertionAdapter = new EntityInsertionAdapter<DailyCapEntity>(db) {
            @Override public String createQuery() {
                return "INSERT OR IGNORE INTO `daily_caps` (`id`,`user_id`,`date_key`,"
                     + "`ink_from_reading`,`reading_cap`,`achievements_today`,"
                     + "`last_achievement_minute_ts`,`achievements_in_minute`) VALUES (?,?,?,?,?,?,?,?)";
            }
            @Override public void bind(SupportSQLiteStatement s, DailyCapEntity e) { bindDC(s, e); }
        };
    }

    private void bindDC(SupportSQLiteStatement s, DailyCapEntity e) {
        if (e.getId()      == null) s.bindNull(1); else s.bindString(1, e.getId());
        if (e.getUserId()  == null) s.bindNull(2); else s.bindString(2, e.getUserId());
        if (e.getDateKey() == null) s.bindNull(3); else s.bindString(3, e.getDateKey());
        s.bindLong(4, e.getInkFromReading());
        s.bindLong(5, e.getReadingCap());
        s.bindLong(6, e.getAchievementsToday());
        s.bindLong(7, e.getLastAchievementMinuteTs());
        s.bindLong(8, e.getAchievementsInMinute());
    }

    private DailyCapEntity cursorToDC(Cursor c) {
        DailyCapEntity e = new DailyCapEntity();
        e.setId(                      c.isNull(CursorUtil.getColumnIndexOrThrow(c,"id"))      ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c,"id")));
        e.setUserId(                  c.isNull(CursorUtil.getColumnIndexOrThrow(c,"user_id")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c,"user_id")));
        e.setDateKey(                 c.isNull(CursorUtil.getColumnIndexOrThrow(c,"date_key"))? null : c.getString(CursorUtil.getColumnIndexOrThrow(c,"date_key")));
        e.setInkFromReading(          c.getInt(CursorUtil.getColumnIndexOrThrow(c,"ink_from_reading")));
        e.setReadingCap(              c.getInt(CursorUtil.getColumnIndexOrThrow(c,"reading_cap")));
        e.setAchievementsToday(       c.getInt(CursorUtil.getColumnIndexOrThrow(c,"achievements_today")));
        e.setLastAchievementMinuteTs( c.getLong(CursorUtil.getColumnIndexOrThrow(c,"last_achievement_minute_ts")));
        e.setAchievementsInMinute(    c.getInt(CursorUtil.getColumnIndexOrThrow(c,"achievements_in_minute")));
        return e;
    }

    @Override public void insert(DailyCapEntity e) {
        __db.assertNotSuspendingTransaction(); __db.beginTransaction();
        try { __insertionAdapter.insert(e); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); }
    }

    @Override public DailyCapEntity get(String userId, String dateKey) {
        final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM daily_caps WHERE user_id = ? AND date_key = ?", 2);
        q.bindString(1, userId); q.bindString(2, dateKey);
        final Cursor c = DBUtil.query(__db, q, false, null);
        try { return c.moveToFirst() ? cursorToDC(c) : null; } finally { c.close(); q.release(); }
    }

    @Override public void addReadingInk(String userId, String dateKey, int delta) {
        __db.assertNotSuspendingTransaction();
        final SupportSQLiteStatement s = __db.compileStatement("UPDATE daily_caps SET ink_from_reading = ink_from_reading + ? WHERE user_id = ? AND date_key = ?");
        __db.beginTransaction();
        try { s.bindLong(1,delta); s.bindString(2,userId); s.bindString(3,dateKey); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); }
    }

    @Override public void incrementAchievements(String userId, String dateKey) {
        __db.assertNotSuspendingTransaction();
        final SupportSQLiteStatement s = __db.compileStatement("UPDATE daily_caps SET achievements_today = achievements_today + 1 WHERE user_id = ? AND date_key = ?");
        __db.beginTransaction();
        try { s.bindString(1,userId); s.bindString(2,dateKey); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); }
    }

    @Override public void updateMinuteWindow(String userId, String dateKey, long ts, int count) {
        __db.assertNotSuspendingTransaction();
        final SupportSQLiteStatement s = __db.compileStatement("UPDATE daily_caps SET last_achievement_minute_ts = ?, achievements_in_minute = ? WHERE user_id = ? AND date_key = ?");
        __db.beginTransaction();
        try { s.bindLong(1,ts); s.bindLong(2,count); s.bindString(3,userId); s.bindString(4,dateKey); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); }
    }

    @Override public void pruneOlderThan(String dateKey) {
        __db.assertNotSuspendingTransaction();
        final SupportSQLiteStatement s = __db.compileStatement("DELETE FROM daily_caps WHERE date_key < ?");
        __db.beginTransaction();
        try { s.bindString(1,dateKey); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); }
    }
}
