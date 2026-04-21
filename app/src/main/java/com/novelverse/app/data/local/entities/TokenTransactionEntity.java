package com.novelverse.app.data.local.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Immutable ledger row for every Ink/Quill movement.
 * type: EARN_READING, EARN_ACHIEVEMENT, EARN_STREAK,
 *       SPEND_FREEZE, SPEND_SHIELD, SPEND_STORE,
 *       PURCHASE_QUILL, BONUS_DAILY
 */
@Entity(
    tableName = "token_transactions",
    indices = {
        @Index(value = "user_id"),
        @Index(value = "created_at")
    }
)
public class TokenTransactionEntity {

    @PrimaryKey
    @ColumnInfo(name = "id")
    private String id; // UUID

    @ColumnInfo(name = "user_id")
    private String userId;

    @ColumnInfo(name = "type")
    private String type;

    /** Positive = credit, negative = debit */
    @ColumnInfo(name = "ink_delta")
    private int inkDelta;

    @ColumnInfo(name = "quill_delta")
    private int quillDelta;

    /** Ink balance AFTER this transaction (snapshot) */
    @ColumnInfo(name = "ink_after")
    private int inkAfter;

    @ColumnInfo(name = "quill_after")
    private int quillAfter;

    /** Human-readable reason (e.g., "Unlocked Bookworm") */
    @ColumnInfo(name = "reason")
    private String reason;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "needs_sync")
    private boolean needsSync;

    public TokenTransactionEntity() {}

    public String  getId()          { return id; }
    public String  getUserId()      { return userId; }
    public String  getType()        { return type; }
    public int     getInkDelta()    { return inkDelta; }
    public int     getQuillDelta()  { return quillDelta; }
    public int     getInkAfter()    { return inkAfter; }
    public int     getQuillAfter()  { return quillAfter; }
    public String  getReason()      { return reason; }
    public long    getCreatedAt()   { return createdAt; }
    public boolean isNeedsSync()    { return needsSync; }

    public void setId(String id)             { this.id = id; }
    public void setUserId(String userId)     { this.userId = userId; }
    public void setType(String type)         { this.type = type; }
    public void setInkDelta(int inkDelta)    { this.inkDelta = inkDelta; }
    public void setQuillDelta(int quillDelta){ this.quillDelta = quillDelta; }
    public void setInkAfter(int inkAfter)    { this.inkAfter = inkAfter; }
    public void setQuillAfter(int quillAfter){ this.quillAfter = quillAfter; }
    public void setReason(String reason)     { this.reason = reason; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setNeedsSync(boolean v)      { this.needsSync = v; }
}
