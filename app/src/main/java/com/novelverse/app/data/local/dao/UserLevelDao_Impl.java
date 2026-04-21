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

import com.novelverse.app.data.local.entities.UserLevelEntity;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unchecked", "deprecation"})
public final class UserLevelDao_Impl implements UserLevelDao {

    private final RoomDatabase __db;
    private final EntityInsertionAdapter<UserLevelEntity> __insertionAdapter;
    private final EntityDeletionOrUpdateAdapter<UserLevelEntity> __updateAdapter;

    public UserLevelDao_Impl(RoomDatabase db) {
        this.__db = db;
        this.__insertionAdapter = new EntityInsertionAdapter<UserLevelEntity>(db) {
            @Override public String createQuery() {
                return "INSERT OR IGNORE INTO `user_levels` (`user_id`,`xp_total`,`current_level`,"
                     + "`xp_in_level`,`xp_level_target`,`badge_slots`,`equipped_badge_ids`,`needs_sync`)"
                     + " VALUES (?,?,?,?,?,?,?,?)";
            }
            @Override public void bind(SupportSQLiteStatement s, UserLevelEntity e) { bindUL(s, e); }
        };
        this.__updateAdapter = new EntityDeletionOrUpdateAdapter<UserLevelEntity>(db) {
            @Override public String createQuery() {
                return "UPDATE OR ABORT `user_levels` SET `user_id`=?,`xp_total`=?,`current_level`=?,"
                     + "`xp_in_level`=?,`xp_level_target`=?,`badge_slots`=?,`equipped_badge_ids`=?,"
                     + "`needs_sync`=? WHERE `user_id`=?";
            }
            @Override public void bind(SupportSQLiteStatement s, UserLevelEntity e) {
                bindUL(s, e); s.bindString(9, e.getUserId());
            }
        };
    }

    private void bindUL(SupportSQLiteStatement s, UserLevelEntity e) {
        if (e.getUserId()           == null) s.bindNull(1); else s.bindString(1, e.getUserId());
        s.bindLong(2, e.getXpTotal());
        s.bindLong(3, e.getCurrentLevel());
        s.bindLong(4, e.getXpInLevel());
        s.bindLong(5, e.getXpLevelTarget());
        s.bindLong(6, e.getBadgeSlots());
        if (e.getEquippedBadgeIds() == null) s.bindNull(7); else s.bindString(7, e.getEquippedBadgeIds());
        s.bindLong(8, e.isNeedsSync() ? 1 : 0);
    }

    private UserLevelEntity cursorToUL(Cursor c) {
        UserLevelEntity e = new UserLevelEntity();
        e.setUserId(          c.isNull(CursorUtil.getColumnIndexOrThrow(c,"user_id"))           ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c,"user_id")));
        e.setXpTotal(         c.getLong(CursorUtil.getColumnIndexOrThrow(c,"xp_total")));
        e.setCurrentLevel(    c.getInt(CursorUtil.getColumnIndexOrThrow(c,"current_level")));
        e.setXpInLevel(       c.getInt(CursorUtil.getColumnIndexOrThrow(c,"xp_in_level")));
        e.setXpLevelTarget(   c.getInt(CursorUtil.getColumnIndexOrThrow(c,"xp_level_target")));
        e.setBadgeSlots(      c.getInt(CursorUtil.getColumnIndexOrThrow(c,"badge_slots")));
        e.setEquippedBadgeIds(c.isNull(CursorUtil.getColumnIndexOrThrow(c,"equipped_badge_ids")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c,"equipped_badge_ids")));
        e.setNeedsSync(       c.getInt(CursorUtil.getColumnIndexOrThrow(c,"needs_sync")) != 0);
        return e;
    }

    @Override public void insert(UserLevelEntity e) {
        __db.assertNotSuspendingTransaction(); __db.beginTransaction();
        try { __insertionAdapter.insert(e); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); }
    }

    @Override public void update(UserLevelEntity e) {
        __db.assertNotSuspendingTransaction(); __db.beginTransaction();
        try { __updateAdapter.handle(e); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); }
    }

    @Override public UserLevelEntity get(String userId) {
        final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM user_levels WHERE user_id = ?", 1);
        q.bindString(1, userId);
        final Cursor c = DBUtil.query(__db, q, false, null);
        try { return c.moveToFirst() ? cursorToUL(c) : null; } finally { c.close(); q.release(); }
    }

    @Override public LiveData<UserLevelEntity> getLive(String userId) {
        final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM user_levels WHERE user_id = ?", 1);
        q.bindString(1, userId);
        return __db.getInvalidationTracker().createLiveData(new String[]{"user_levels"}, false, () -> {
            final Cursor c = DBUtil.query(__db, q, false, null);
            try { return c.moveToFirst() ? cursorToUL(c) : null; } finally { c.close(); }
        });
    }

    @Override public void updateProgress(String userId, long xpTotal, int level, int xpInLevel, int target, int slots) {
        __db.assertNotSuspendingTransaction();
        final SupportSQLiteStatement s = __db.compileStatement(
            "UPDATE user_levels SET xp_total=?,current_level=?,xp_in_level=?,xp_level_target=?,badge_slots=?,needs_sync=1 WHERE user_id=?");
        __db.beginTransaction();
        try { s.bindLong(1,xpTotal); s.bindLong(2,level); s.bindLong(3,xpInLevel); s.bindLong(4,target); s.bindLong(5,slots); s.bindString(6,userId); s.executeUpdateDelete(); __db.setTransactionSuccessful(); }
        finally { __db.endTransaction(); }
    }

    @Override public void updateEquippedBadges(String userId, String badgeIds) {
        __db.assertNotSuspendingTransaction();
        final SupportSQLiteStatement s = __db.compileStatement("UPDATE user_levels SET equipped_badge_ids=?,needs_sync=1 WHERE user_id=?");
        __db.beginTransaction();
        try { if (badgeIds==null) s.bindNull(1); else s.bindString(1,badgeIds); s.bindString(2,userId); s.executeUpdateDelete(); __db.setTransactionSuccessful(); }
        finally { __db.endTransaction(); }
    }

    @Override public List<UserLevelEntity> getPendingSync() {
        final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM user_levels WHERE needs_sync = 1", 0);
        final Cursor c = DBUtil.query(__db, q, false, null);
        try { List<UserLevelEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToUL(c)); return r; }
        finally { c.close(); q.release(); }
    }

    @Override public void markSynced(String userId) {
        __db.assertNotSuspendingTransaction();
        final SupportSQLiteStatement s = __db.compileStatement("UPDATE user_levels SET needs_sync=0 WHERE user_id=?");
        __db.beginTransaction();
        try { s.bindString(1,userId); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); }
    }
}
