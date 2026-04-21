package com.novelverse.app.data.local.dao;

import android.database.Cursor;
import androidx.lifecycle.LiveData;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;

import com.novelverse.app.data.local.entities.TokenTransactionEntity;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unchecked", "deprecation"})
public final class TokenTransactionDao_Impl implements TokenTransactionDao {

    private final RoomDatabase __db;
    private final EntityInsertionAdapter<TokenTransactionEntity> __insertionAdapter;

    public TokenTransactionDao_Impl(RoomDatabase db) {
        this.__db = db;
        this.__insertionAdapter = new EntityInsertionAdapter<TokenTransactionEntity>(db) {
            @Override public String createQuery() {
                return "INSERT OR REPLACE INTO `token_transactions` (`id`,`user_id`,`type`,"
                     + "`ink_delta`,`quill_delta`,`ink_after`,`quill_after`,`reason`,"
                     + "`created_at`,`needs_sync`) VALUES (?,?,?,?,?,?,?,?,?,?)";
            }
            @Override public void bind(SupportSQLiteStatement s, TokenTransactionEntity e) { bindTT(s, e); }
        };
    }

    private void bindTT(SupportSQLiteStatement s, TokenTransactionEntity e) {
        if (e.getId()     == null) s.bindNull(1); else s.bindString(1, e.getId());
        if (e.getUserId() == null) s.bindNull(2); else s.bindString(2, e.getUserId());
        if (e.getType()   == null) s.bindNull(3); else s.bindString(3, e.getType());
        s.bindLong(4, e.getInkDelta());
        s.bindLong(5, e.getQuillDelta());
        s.bindLong(6, e.getInkAfter());
        s.bindLong(7, e.getQuillAfter());
        if (e.getReason() == null) s.bindNull(8); else s.bindString(8, e.getReason());
        s.bindLong(9,  e.getCreatedAt());
        s.bindLong(10, e.isNeedsSync() ? 1 : 0);
    }

    private TokenTransactionEntity cursorToTT(Cursor c) {
        TokenTransactionEntity e = new TokenTransactionEntity();
        e.setId(         c.isNull(CursorUtil.getColumnIndexOrThrow(c,"id"))         ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c,"id")));
        e.setUserId(     c.isNull(CursorUtil.getColumnIndexOrThrow(c,"user_id"))    ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c,"user_id")));
        e.setType(       c.isNull(CursorUtil.getColumnIndexOrThrow(c,"type"))       ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c,"type")));
        e.setInkDelta(   c.getInt(CursorUtil.getColumnIndexOrThrow(c,"ink_delta")));
        e.setQuillDelta( c.getInt(CursorUtil.getColumnIndexOrThrow(c,"quill_delta")));
        e.setInkAfter(   c.getInt(CursorUtil.getColumnIndexOrThrow(c,"ink_after")));
        e.setQuillAfter( c.getInt(CursorUtil.getColumnIndexOrThrow(c,"quill_after")));
        e.setReason(     c.isNull(CursorUtil.getColumnIndexOrThrow(c,"reason"))     ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c,"reason")));
        e.setCreatedAt(  c.getLong(CursorUtil.getColumnIndexOrThrow(c,"created_at")));
        e.setNeedsSync(  c.getInt(CursorUtil.getColumnIndexOrThrow(c,"needs_sync")) != 0);
        return e;
    }

    @Override public void insert(TokenTransactionEntity e) {
        __db.assertNotSuspendingTransaction(); __db.beginTransaction();
        try { __insertionAdapter.insert(e); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); }
    }

    @Override public List<TokenTransactionEntity> getRecent(String userId, int limit) {
        final RoomSQLiteQuery q = RoomSQLiteQuery.acquire(
            "SELECT * FROM token_transactions WHERE user_id = ? ORDER BY created_at DESC LIMIT ?", 2);
        q.bindString(1, userId); q.bindLong(2, limit);
        final Cursor c = DBUtil.query(__db, q, false, null);
        try { List<TokenTransactionEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToTT(c)); return r; }
        finally { c.close(); q.release(); }
    }

    @Override public List<TokenTransactionEntity> getPendingSync() {
        final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM token_transactions WHERE needs_sync = 1", 0);
        final Cursor c = DBUtil.query(__db, q, false, null);
        try { List<TokenTransactionEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToTT(c)); return r; }
        finally { c.close(); q.release(); }
    }

    @Override public void markSynced(String id) {
        __db.assertNotSuspendingTransaction();
        final SupportSQLiteStatement s = __db.compileStatement("UPDATE token_transactions SET needs_sync = 0 WHERE id = ?");
        __db.beginTransaction();
        try { s.bindString(1, id); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); }
    }

    @Override public void pruneOlderThan(String userId, long beforeTs) {
        __db.assertNotSuspendingTransaction();
        final SupportSQLiteStatement s = __db.compileStatement("DELETE FROM token_transactions WHERE user_id = ? AND created_at < ?");
        __db.beginTransaction();
        try { s.bindString(1, userId); s.bindLong(2, beforeTs); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); }
    }

    @Override public LiveData<List<TokenTransactionEntity>> getRecentLive(String userId) {
        final RoomSQLiteQuery q = RoomSQLiteQuery.acquire(
            "SELECT * FROM token_transactions WHERE user_id = ? ORDER BY created_at DESC LIMIT 50", 1);
        q.bindString(1, userId);
        return __db.getInvalidationTracker().createLiveData(new String[]{"token_transactions"}, false, () -> {
            final Cursor c = DBUtil.query(__db, q, false, null);
            try { List<TokenTransactionEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToTT(c)); return r; }
            finally { c.close(); }
        });
    }
}
