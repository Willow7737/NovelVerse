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

import com.novelverse.app.data.local.entities.UserCurrencyEntity;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unchecked", "deprecation"})
public final class UserCurrencyDao_Impl implements UserCurrencyDao {

    private final RoomDatabase __db;
    private final EntityInsertionAdapter<UserCurrencyEntity> __insertionAdapter;
    private final EntityDeletionOrUpdateAdapter<UserCurrencyEntity> __updateAdapter;

    public UserCurrencyDao_Impl(RoomDatabase db) {
        this.__db = db;
        this.__insertionAdapter = new EntityInsertionAdapter<UserCurrencyEntity>(db) {
            @Override public String createQuery() {
                return "INSERT OR IGNORE INTO `user_currency` (`user_id`,`ink_balance`,`quill_balance`,`version`,`last_synced_at`,`needs_sync`) VALUES (?,?,?,?,?,?)";
            }
            @Override public void bind(SupportSQLiteStatement s, UserCurrencyEntity e) { bindUC(s, e); }
        };
        this.__updateAdapter = new EntityDeletionOrUpdateAdapter<UserCurrencyEntity>(db) {
            @Override public String createQuery() {
                return "UPDATE OR ABORT `user_currency` SET `user_id`=?,`ink_balance`=?,`quill_balance`=?,`version`=?,`last_synced_at`=?,`needs_sync`=? WHERE `user_id`=?";
            }
            @Override public void bind(SupportSQLiteStatement s, UserCurrencyEntity e) {
                bindUC(s, e); s.bindString(7, e.getUserId());
            }
        };
    }

    private void bindUC(SupportSQLiteStatement s, UserCurrencyEntity e) {
        if (e.getUserId() == null) s.bindNull(1); else s.bindString(1, e.getUserId());
        s.bindLong(2, e.getInkBalance());
        s.bindLong(3, e.getQuillBalance());
        s.bindLong(4, e.getVersion());
        s.bindLong(5, e.getLastSyncedAt());
        s.bindLong(6, e.isNeedsSync() ? 1 : 0);
    }

    private UserCurrencyEntity cursorToUC(Cursor c) {
        UserCurrencyEntity e = new UserCurrencyEntity();
        e.setUserId(       c.isNull(CursorUtil.getColumnIndexOrThrow(c,"user_id")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c,"user_id")));
        e.setInkBalance(   c.getInt(CursorUtil.getColumnIndexOrThrow(c,"ink_balance")));
        e.setQuillBalance( c.getInt(CursorUtil.getColumnIndexOrThrow(c,"quill_balance")));
        e.setVersion(      c.getLong(CursorUtil.getColumnIndexOrThrow(c,"version")));
        e.setLastSyncedAt( c.getLong(CursorUtil.getColumnIndexOrThrow(c,"last_synced_at")));
        e.setNeedsSync(    c.getInt(CursorUtil.getColumnIndexOrThrow(c,"needs_sync")) != 0);
        return e;
    }

    @Override public void insert(UserCurrencyEntity e) {
        __db.assertNotSuspendingTransaction(); __db.beginTransaction();
        try { __insertionAdapter.insert(e); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); }
    }

    @Override public void update(UserCurrencyEntity e) {
        __db.assertNotSuspendingTransaction(); __db.beginTransaction();
        try { __updateAdapter.handle(e); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); }
    }

    @Override public UserCurrencyEntity get(String userId) {
        final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM user_currency WHERE user_id = ?", 1);
        q.bindString(1, userId);
        final Cursor c = DBUtil.query(__db, q, false, null);
        try { return c.moveToFirst() ? cursorToUC(c) : null; } finally { c.close(); q.release(); }
    }

    @Override public LiveData<UserCurrencyEntity> getLive(String userId) {
        final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM user_currency WHERE user_id = ?", 1);
        q.bindString(1, userId);
        return __db.getInvalidationTracker().createLiveData(new String[]{"user_currency"}, false, () -> {
            final Cursor c = DBUtil.query(__db, q, false, null);
            try { return c.moveToFirst() ? cursorToUC(c) : null; } finally { c.close(); }
        });
    }

    @Override public void addInk(String userId, int delta) {
        __db.assertNotSuspendingTransaction();
        final SupportSQLiteStatement s = __db.compileStatement(
            "UPDATE user_currency SET ink_balance = ink_balance + ?, version = version + 1, needs_sync = 1 WHERE user_id = ?");
        __db.beginTransaction();
        try { s.bindLong(1, delta); s.bindString(2, userId); s.executeUpdateDelete(); __db.setTransactionSuccessful(); }
        finally { __db.endTransaction(); }
    }

    @Override public void addQuill(String userId, int delta) {
        __db.assertNotSuspendingTransaction();
        final SupportSQLiteStatement s = __db.compileStatement(
            "UPDATE user_currency SET quill_balance = quill_balance + ?, version = version + 1, needs_sync = 1 WHERE user_id = ?");
        __db.beginTransaction();
        try { s.bindLong(1, delta); s.bindString(2, userId); s.executeUpdateDelete(); __db.setTransactionSuccessful(); }
        finally { __db.endTransaction(); }
    }

    @Override public void applyServerState(String userId, int ink, int quill, long version, long ts) {
        __db.assertNotSuspendingTransaction();
        final SupportSQLiteStatement s = __db.compileStatement(
            "UPDATE user_currency SET ink_balance = ?, quill_balance = ?, version = ?, last_synced_at = ?, needs_sync = 0 WHERE user_id = ?");
        __db.beginTransaction();
        try { s.bindLong(1, ink); s.bindLong(2, quill); s.bindLong(3, version); s.bindLong(4, ts); s.bindString(5, userId); s.executeUpdateDelete(); __db.setTransactionSuccessful(); }
        finally { __db.endTransaction(); }
    }

    @Override public List<UserCurrencyEntity> getPendingSync() {
        final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM user_currency WHERE needs_sync = 1", 0);
        final Cursor c = DBUtil.query(__db, q, false, null);
        try { List<UserCurrencyEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToUC(c)); return r; }
        finally { c.close(); q.release(); }
    }
   
    @Override public void markSynced(String userId) {
        __db.assertNotSuspendingTransaction();
        final SupportSQLiteStatement s = __db.compileStatement(
            "UPDATE user_currency SET needs_sync = 0 WHERE user_id = ?");
        __db.beginTransaction();
        try { s.bindString(1, userId); s.executeUpdateDelete(); __db.setTransactionSuccessful(); }
        finally { __db.endTransaction(); }
    }
}
