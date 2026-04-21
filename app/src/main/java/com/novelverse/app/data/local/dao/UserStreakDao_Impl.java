package com.novelverse.app.data.local.dao;

import android.database.Cursor;
import androidx.lifecycle.LiveData;
import androidx.room.EntityInsertionAdapter;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;

import com.novelverse.app.data.local.entities.UserStreakEntity;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unchecked", "deprecation"})
public final class UserStreakDao_Impl implements UserStreakDao {

    private final RoomDatabase __db;
    private final EntityInsertionAdapter<UserStreakEntity> __insertionAdapter;
    private final EntityDeletionOrUpdateAdapter<UserStreakEntity> __updateAdapter;

    public UserStreakDao_Impl(RoomDatabase db) {
        this.__db = db;
        this.__insertionAdapter = new EntityInsertionAdapter<UserStreakEntity>(db) {
            @Override public String createQuery() {
                return "INSERT OR IGNORE INTO `user_streaks` (`user_id`,`current_streak`,`longest_streak`,"
                     + "`last_activity_date`,`grace_window_start`,`grace_used_in_window`,"
                     + "`freeze_expires_at`,`free_freezes_used_this_month`,`freeze_month_reset_at`,`needs_sync`)"
                     + " VALUES (?,?,?,?,?,?,?,?,?,?)";
            }
            @Override public void bind(SupportSQLiteStatement s, UserStreakEntity e) { bindUS(s, e); }
        };
        this.__updateAdapter = new EntityDeletionOrUpdateAdapter<UserStreakEntity>(db) {
            @Override public String createQuery() {
                return "UPDATE OR ABORT `user_streaks` SET `user_id`=?,`current_streak`=?,`longest_streak`=?,"
                     + "`last_activity_date`=?,`grace_window_start`=?,`grace_used_in_window`=?,"
                     + "`freeze_expires_at`=?,`free_freezes_used_this_month`=?,`freeze_month_reset_at`=?,"
                     + "`needs_sync`=? WHERE `user_id`=?";
            }
            @Override public void bind(SupportSQLiteStatement s, UserStreakEntity e) {
                bindUS(s, e); s.bindString(11, e.getUserId());
            }
        };
    }

    private void bindUS(SupportSQLiteStatement s, UserStreakEntity e) {
        if (e.getUserId() == null) s.bindNull(1); else s.bindString(1, e.getUserId());
        s.bindLong(2, e.getCurrentStreak());
        s.bindLong(3, e.getLongestStreak());
        s.bindLong(4, e.getLastActivityDate());
        s.bindLong(5, e.getGraceWindowStart());
        s.bindLong(6, e.isGraceUsedInWindow() ? 1 : 0);
        s.bindLong(7, e.getFreezeExpiresAt());
        s.bindLong(8, e.getFreeFreezesUsedThisMonth());
        s.bindLong(9, e.getFreezeMonthResetAt());
        s.bindLong(10, e.isNeedsSync() ? 1 : 0);
    }

    private UserStreakEntity cursorToUS(Cursor c) {
        UserStreakEntity e = new UserStreakEntity();
        e.setUserId(                 c.isNull(CursorUtil.getColumnIndexOrThrow(c,"user_id")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c,"user_id")));
        e.setCurrentStreak(          c.getInt(CursorUtil.getColumnIndexOrThrow(c,"current_streak")));
        e.setLongestStreak(          c.getInt(CursorUtil.getColumnIndexOrThrow(c,"longest_streak")));
        e.setLastActivityDate(       c.getLong(CursorUtil.getColumnIndexOrThrow(c,"last_activity_date")));
        e.setGraceWindowStart(       c.getLong(CursorUtil.getColumnIndexOrThrow(c,"grace_window_start")));
        e.setGraceUsedInWindow(      c.getInt(CursorUtil.getColumnIndexOrThrow(c,"grace_used_in_window")) != 0);
        e.setFreezeExpiresAt(        c.getLong(CursorUtil.getColumnIndexOrThrow(c,"freeze_expires_at")));
        e.setFreeFreezesUsedThisMonth(c.getInt(CursorUtil.getColumnIndexOrThrow(c,"free_freezes_used_this_month")));
        e.setFreezeMonthResetAt(     c.getLong(CursorUtil.getColumnIndexOrThrow(c,"freeze_month_reset_at")));
        e.setNeedsSync(              c.getInt(CursorUtil.getColumnIndexOrThrow(c,"needs_sync")) != 0);
        return e;
    }

    @Override public void insert(UserStreakEntity e) {
        __db.assertNotSuspendingTransaction(); __db.beginTransaction();
        try { __insertionAdapter.insert(e); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); }
    }
    @Override public void update(UserStreakEntity e) {
        __db.assertNotSuspendingTransaction(); __db.beginTransaction();
        try { __updateAdapter.handle(e); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); }
    }
    @Override public UserStreakEntity get(String userId) {
        final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM user_streaks WHERE user_id = ?", 1);
        q.bindString(1, userId);
        final Cursor c = DBUtil.query(__db, q, false, null);
        try { return c.moveToFirst() ? cursorToUS(c) : null; } finally { c.close(); q.release(); }
    }
    @Override public LiveData<UserStreakEntity> getLive(String userId) {
        final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM user_streaks WHERE user_id = ?", 1);
        q.bindString(1, userId);
        return __db.getInvalidationTracker().createLiveData(new String[]{"user_streaks"}, false, () -> {
            final Cursor c = DBUtil.query(__db, q, false, null);
            try { return c.moveToFirst() ? cursorToUS(c) : null; } finally { c.close(); }
        });
    }
    @Override public void updateStreak(String userId, int streak, int longest, long lastActivity) {
        __db.assertNotSuspendingTransaction();
        final SupportSQLiteStatement s = __db.compileStatement("UPDATE user_streaks SET current_streak=?,longest_streak=?,last_activity_date=?,needs_sync=1 WHERE user_id=?");
        __db.beginTransaction();
        try { s.bindLong(1,streak); s.bindLong(2,longest); s.bindLong(3,lastActivity); s.bindString(4,userId); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); }
    }
    @Override public void applyFreeze(String userId, long expiresAt, int freeUsed) {
        __db.assertNotSuspendingTransaction();
        final SupportSQLiteStatement s = __db.compileStatement("UPDATE user_streaks SET freeze_expires_at=?,free_freezes_used_this_month=?,needs_sync=1 WHERE user_id=?");
        __db.beginTransaction();
        try { s.bindLong(1,expiresAt); s.bindLong(2,freeUsed); s.bindString(3,userId); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); }
    }
    @Override public void updateGrace(String userId, boolean used, long windowStart) {
        __db.assertNotSuspendingTransaction();
        final SupportSQLiteStatement s = __db.compileStatement("UPDATE user_streaks SET grace_used_in_window=?,grace_window_start=? WHERE user_id=?");
        __db.beginTransaction();
        try { s.bindLong(1,used?1:0); s.bindLong(2,windowStart); s.bindString(3,userId); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); }
    }
    @Override public void resetMonthlyFreezes(String userId, long resetAt) {
        __db.assertNotSuspendingTransaction();
        final SupportSQLiteStatement s = __db.compileStatement("UPDATE user_streaks SET free_freezes_used_this_month=0,freeze_month_reset_at=? WHERE user_id=?");
        __db.beginTransaction();
        try { s.bindLong(1,resetAt); s.bindString(2,userId); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); }
    }
    @Override public List<UserStreakEntity> getPendingSync() {
        final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM user_streaks WHERE needs_sync = 1", 0);
        final Cursor c = DBUtil.query(__db, q, false, null);
        try { List<UserStreakEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToUS(c)); return r; }
        finally { c.close(); q.release(); }
    }
    @Override public void markSynced(String userId) {
        __db.assertNotSuspendingTransaction();
        final SupportSQLiteStatement s = __db.compileStatement("UPDATE user_streaks SET needs_sync=0 WHERE user_id=?");
        __db.beginTransaction();
        try { s.bindString(1,userId); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); }
    }
}
