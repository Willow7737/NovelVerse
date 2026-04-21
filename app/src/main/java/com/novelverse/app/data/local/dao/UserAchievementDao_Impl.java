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

import com.novelverse.app.data.local.entities.UserAchievementEntity;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unchecked", "deprecation"})
public final class UserAchievementDao_Impl implements UserAchievementDao {

    private final RoomDatabase __db;
    private final EntityInsertionAdapter<UserAchievementEntity> __insertionAdapter;
    private final EntityDeletionOrUpdateAdapter<UserAchievementEntity> __updateAdapter;

    public UserAchievementDao_Impl(RoomDatabase db) {
        this.__db = db;
        this.__insertionAdapter = new EntityInsertionAdapter<UserAchievementEntity>(db) {
            @Override public String createQuery() {
                return "INSERT OR IGNORE INTO `user_achievements` (`id`,`user_id`,`achievement_id`,"
                     + "`is_unlocked`,`unlocked_at`,`current_progress`,`reward_claimed`,`needs_sync`)"
                     + " VALUES (?,?,?,?,?,?,?,?)";
            }
            @Override public void bind(SupportSQLiteStatement s, UserAchievementEntity e) { bindUA(s, e); }
        };
        this.__updateAdapter = new EntityDeletionOrUpdateAdapter<UserAchievementEntity>(db) {
            @Override public String createQuery() {
                return "UPDATE OR ABORT `user_achievements` SET `id`=?,`user_id`=?,`achievement_id`=?,"
                     + "`is_unlocked`=?,`unlocked_at`=?,`current_progress`=?,`reward_claimed`=?,"
                     + "`needs_sync`=? WHERE `id`=?";
            }
            @Override public void bind(SupportSQLiteStatement s, UserAchievementEntity e) {
                bindUA(s, e); s.bindString(9, e.getId());
            }
        };
    }

    private void bindUA(SupportSQLiteStatement s, UserAchievementEntity e) {
        if (e.getId()            == null) s.bindNull(1); else s.bindString(1, e.getId());
        if (e.getUserId()        == null) s.bindNull(2); else s.bindString(2, e.getUserId());
        if (e.getAchievementId() == null) s.bindNull(3); else s.bindString(3, e.getAchievementId());
        s.bindLong(4, e.isUnlocked() ? 1 : 0);
        s.bindLong(5, e.getUnlockedAt());
        s.bindLong(6, e.getCurrentProgress());
        s.bindLong(7, e.isRewardClaimed() ? 1 : 0);
        s.bindLong(8, e.isNeedsSync() ? 1 : 0);
    }

    private UserAchievementEntity cursorToUA(Cursor c) {
        UserAchievementEntity e = new UserAchievementEntity();
        e.setId(            c.isNull(CursorUtil.getColumnIndexOrThrow(c,"id"))             ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c,"id")));
        e.setUserId(        c.isNull(CursorUtil.getColumnIndexOrThrow(c,"user_id"))        ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c,"user_id")));
        e.setAchievementId( c.isNull(CursorUtil.getColumnIndexOrThrow(c,"achievement_id")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c,"achievement_id")));
        e.setIsUnlocked(    c.getInt(CursorUtil.getColumnIndexOrThrow(c,"is_unlocked")) != 0);
        e.setUnlockedAt(    c.getLong(CursorUtil.getColumnIndexOrThrow(c,"unlocked_at")));
        e.setCurrentProgress(c.getInt(CursorUtil.getColumnIndexOrThrow(c,"current_progress")));
        e.setRewardClaimed( c.getInt(CursorUtil.getColumnIndexOrThrow(c,"reward_claimed")) != 0);
        e.setNeedsSync(     c.getInt(CursorUtil.getColumnIndexOrThrow(c,"needs_sync")) != 0);
        return e;
    }

    @Override public long insert(UserAchievementEntity e) {
        __db.assertNotSuspendingTransaction();
        __db.beginTransaction();
        try { long id = __insertionAdapter.insertAndReturnId(e); __db.setTransactionSuccessful(); return id; }
        finally { __db.endTransaction(); }
    }

    @Override public void update(UserAchievementEntity e) {
        __db.assertNotSuspendingTransaction();
        __db.beginTransaction();
        try { __updateAdapter.handle(e); __db.setTransactionSuccessful(); }
        finally { __db.endTransaction(); }
    }

    @Override public List<UserAchievementEntity> getAllForUser(String userId) {
        final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM user_achievements WHERE user_id = ?", 1);
        q.bindString(1, userId);
        final Cursor c = DBUtil.query(__db, q, false, null);
        try { List<UserAchievementEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToUA(c)); return r; }
        finally { c.close(); q.release(); }
    }

    @Override public UserAchievementEntity get(String userId, String achievementId) {
        final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM user_achievements WHERE user_id = ? AND achievement_id = ?", 2);
        q.bindString(1, userId); q.bindString(2, achievementId);
        final Cursor c = DBUtil.query(__db, q, false, null);
        try { return c.moveToFirst() ? cursorToUA(c) : null; } finally { c.close(); q.release(); }
    }

    @Override public int countUnlocked(String userId) {
        final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT COUNT(*) FROM user_achievements WHERE user_id = ? AND is_unlocked = 1", 1);
        q.bindString(1, userId);
        final Cursor c = DBUtil.query(__db, q, false, null);
        try { return c.moveToFirst() ? c.getInt(0) : 0; } finally { c.close(); q.release(); }
    }

    @Override public void updateProgress(String userId, String achievementId, int progress) {
        __db.assertNotSuspendingTransaction();
        final SupportSQLiteStatement s = __db.compileStatement("UPDATE user_achievements SET current_progress = ? WHERE user_id = ? AND achievement_id = ?");
        __db.beginTransaction();
        try { s.bindLong(1, progress); s.bindString(2, userId); s.bindString(3, achievementId); s.executeUpdateDelete(); __db.setTransactionSuccessful(); }
        finally { __db.endTransaction(); }
    }

    @Override public void markUnlocked(String userId, String achievementId, long ts) {
        __db.assertNotSuspendingTransaction();
        final SupportSQLiteStatement s = __db.compileStatement("UPDATE user_achievements SET is_unlocked = 1, unlocked_at = ? WHERE user_id = ? AND achievement_id = ?");
        __db.beginTransaction();
        try { s.bindLong(1, ts); s.bindString(2, userId); s.bindString(3, achievementId); s.executeUpdateDelete(); __db.setTransactionSuccessful(); }
        finally { __db.endTransaction(); }
    }

    @Override public void markRewardClaimed(String userId, String achievementId) {
        __db.assertNotSuspendingTransaction();
        final SupportSQLiteStatement s = __db.compileStatement("UPDATE user_achievements SET reward_claimed = 1 WHERE user_id = ? AND achievement_id = ?");
        __db.beginTransaction();
        try { s.bindString(1, userId); s.bindString(2, achievementId); s.executeUpdateDelete(); __db.setTransactionSuccessful(); }
        finally { __db.endTransaction(); }
    }

    @Override public void markSynced(String id) {
        __db.assertNotSuspendingTransaction();
        final SupportSQLiteStatement s = __db.compileStatement("UPDATE user_achievements SET needs_sync = 0 WHERE id = ?");
        __db.beginTransaction();
        try { s.bindString(1, id); s.executeUpdateDelete(); __db.setTransactionSuccessful(); }
        finally { __db.endTransaction(); }
    }

    @Override public List<UserAchievementEntity> getPendingSync() {
        final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM user_achievements WHERE needs_sync = 1", 0);
        final Cursor c = DBUtil.query(__db, q, false, null);
        try { List<UserAchievementEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToUA(c)); return r; }
        finally { c.close(); q.release(); }
    }

    @Override public LiveData<List<UserAchievementEntity>> getAllForUserLive(String userId) {
        final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM user_achievements WHERE user_id = ?", 1);
        q.bindString(1, userId);
        return __db.getInvalidationTracker().createLiveData(new String[]{"user_achievements"}, false, () -> {
            final Cursor c = DBUtil.query(__db, q, false, null);
            try { List<UserAchievementEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToUA(c)); return r; }
            finally { c.close(); }
        });
    }

    @Override public LiveData<List<UserAchievementEntity>> getUnlockedLive(String userId) {
        final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM user_achievements WHERE user_id = ? AND is_unlocked = 1 ORDER BY unlocked_at DESC", 1);
        q.bindString(1, userId);
        return __db.getInvalidationTracker().createLiveData(new String[]{"user_achievements"}, false, () -> {
            final Cursor c = DBUtil.query(__db, q, false, null);
            try { List<UserAchievementEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToUA(c)); return r; }
            finally { c.close(); }
        });
    }
}
