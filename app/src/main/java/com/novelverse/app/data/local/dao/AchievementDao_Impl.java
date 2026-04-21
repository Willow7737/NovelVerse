package com.novelverse.app.data.local.dao;

import android.database.Cursor;
import androidx.lifecycle.LiveData;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;

import com.novelverse.app.data.local.entities.AchievementEntity;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unchecked", "deprecation"})
public final class AchievementDao_Impl implements AchievementDao {

    private final RoomDatabase __db;
    private final EntityInsertionAdapter<AchievementEntity> __insertionAdapter;

    public AchievementDao_Impl(RoomDatabase db) {
        this.__db = db;
        this.__insertionAdapter = new EntityInsertionAdapter<AchievementEntity>(db) {
            @Override public String createQuery() {
                return "INSERT OR IGNORE INTO `achievements` (`id`,`title`,`description`,"
                     + "`asset_name`,`rarity`,`category`,`xp_reward`,`ink_reward`,"
                     + "`quill_reward`,`is_visible`,`target_value`) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
            }
            @Override public void bind(SupportSQLiteStatement s, AchievementEntity e) {
                bindAch(s, e);
            }
        };
    }

    private void bindAch(SupportSQLiteStatement s, AchievementEntity e) {
        if (e.getId()          == null) s.bindNull(1); else s.bindString(1, e.getId());
        if (e.getTitle()       == null) s.bindNull(2); else s.bindString(2, e.getTitle());
        if (e.getDescription() == null) s.bindNull(3); else s.bindString(3, e.getDescription());
        if (e.getAssetName()   == null) s.bindNull(4); else s.bindString(4, e.getAssetName());
        if (e.getRarity()      == null) s.bindNull(5); else s.bindString(5, e.getRarity());
        if (e.getCategory()    == null) s.bindNull(6); else s.bindString(6, e.getCategory());
        s.bindLong(7,  e.getXpReward());
        s.bindLong(8,  e.getInkReward());
        s.bindLong(9,  e.getQuillReward());
        s.bindLong(10, e.isVisible() ? 1 : 0);
        s.bindLong(11, e.getTargetValue());
    }

    private AchievementEntity cursorToAch(Cursor c) {
        AchievementEntity e = new AchievementEntity();
        e.setId(          c.isNull(CursorUtil.getColumnIndexOrThrow(c,"id"))          ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c,"id")));
        e.setTitle(       c.isNull(CursorUtil.getColumnIndexOrThrow(c,"title"))       ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c,"title")));
        e.setDescription( c.isNull(CursorUtil.getColumnIndexOrThrow(c,"description")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c,"description")));
        e.setAssetName(   c.isNull(CursorUtil.getColumnIndexOrThrow(c,"asset_name"))  ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c,"asset_name")));
        e.setRarity(      c.isNull(CursorUtil.getColumnIndexOrThrow(c,"rarity"))      ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c,"rarity")));
        e.setCategory(    c.isNull(CursorUtil.getColumnIndexOrThrow(c,"category"))    ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c,"category")));
        e.setXpReward(    c.getInt(CursorUtil.getColumnIndexOrThrow(c,"xp_reward")));
        e.setInkReward(   c.getInt(CursorUtil.getColumnIndexOrThrow(c,"ink_reward")));
        e.setQuillReward( c.getInt(CursorUtil.getColumnIndexOrThrow(c,"quill_reward")));
        e.setIsVisible(   c.getInt(CursorUtil.getColumnIndexOrThrow(c,"is_visible")) != 0);
        e.setTargetValue( c.getInt(CursorUtil.getColumnIndexOrThrow(c,"target_value")));
        return e;
    }

    @Override
    public void insertAll(List<AchievementEntity> achievements) {
        __db.assertNotSuspendingTransaction();
        __db.beginTransaction();
        try {
            for (AchievementEntity a : achievements) __insertionAdapter.insert(a);
            __db.setTransactionSuccessful();
        } finally { __db.endTransaction(); }
    }

    @Override
    public List<AchievementEntity> getAll() {
        final RoomSQLiteQuery q = RoomSQLiteQuery.acquire(
            "SELECT * FROM achievements ORDER BY category, rarity DESC", 0);
        final Cursor c = DBUtil.query(__db, q, false, null);
        try {
            List<AchievementEntity> r = new ArrayList<>();
            while (c.moveToNext()) r.add(cursorToAch(c));
            return r;
        } finally { c.close(); q.release(); }
    }

    @Override
    public AchievementEntity getById(String id) {
        final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM achievements WHERE id = ?", 1);
        q.bindString(1, id);
        final Cursor c = DBUtil.query(__db, q, false, null);
        try { return c.moveToFirst() ? cursorToAch(c) : null; } finally { c.close(); q.release(); }
    }

    @Override
    public int count() {
        final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT COUNT(*) FROM achievements", 0);
        final Cursor c = DBUtil.query(__db, q, false, null);
        try { return c.moveToFirst() ? c.getInt(0) : 0; } finally { c.close(); q.release(); }
    }

    @Override
    public LiveData<List<AchievementEntity>> getAllLive() {
        final RoomSQLiteQuery q = RoomSQLiteQuery.acquire(
            "SELECT * FROM achievements ORDER BY category, rarity DESC", 0);
        return __db.getInvalidationTracker().createLiveData(new String[]{"achievements"}, false, () -> {
            final Cursor c = DBUtil.query(__db, q, false, null);
            try {
                List<AchievementEntity> r = new ArrayList<>();
                while (c.moveToNext()) r.add(cursorToAch(c));
                return r;
            } finally { c.close(); }
        });
    }
}
