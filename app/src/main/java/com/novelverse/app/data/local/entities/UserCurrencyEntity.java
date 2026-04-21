package com.novelverse.app.data.local.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

// ─────────────────────────────────────────────────────────────────────────────
// UserCurrencyEntity — one row per user, tracks Ink + Quill balances
// ─────────────────────────────────────────────────────────────────────────────
@Entity(
    tableName = "user_currency",
    indices = { @Index(value = "user_id", unique = true) }
)
public class UserCurrencyEntity {

    @PrimaryKey
    @ColumnInfo(name = "user_id")
    private String userId;

    /** Soft currency (reading rewards, achievements) */
    @ColumnInfo(name = "ink_balance")
    private int inkBalance;

    /** Hard currency (purchased) */
    @ColumnInfo(name = "quill_balance")
    private int quillBalance;

    /** Optimistic version counter for conflict resolution */
    @ColumnInfo(name = "version")
    private long version;

    @ColumnInfo(name = "last_synced_at")
    private long lastSyncedAt;

    @ColumnInfo(name = "needs_sync")
    private boolean needsSync;

    public UserCurrencyEntity() {}

    public String getUserId()        { return userId; }
    public int    getInkBalance()    { return inkBalance; }
    public int    getQuillBalance()  { return quillBalance; }
    public long   getVersion()       { return version; }
    public long   getLastSyncedAt()  { return lastSyncedAt; }
    public boolean isNeedsSync()     { return needsSync; }

    public void setUserId(String userId)         { this.userId = userId; }
    public void setInkBalance(int inkBalance)     { this.inkBalance = inkBalance; }
    public void setQuillBalance(int quillBalance) { this.quillBalance = quillBalance; }
    public void setVersion(long version)          { this.version = version; }
    public void setLastSyncedAt(long ts)          { this.lastSyncedAt = ts; }
    public void setNeedsSync(boolean needsSync)   { this.needsSync = needsSync; }
}
