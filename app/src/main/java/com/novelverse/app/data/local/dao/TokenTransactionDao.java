package com.novelverse.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.novelverse.app.data.local.entities.TokenTransactionEntity;

import java.util.List;

@Dao
public interface TokenTransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TokenTransactionEntity entity);

    @Query("SELECT * FROM token_transactions WHERE user_id = :userId ORDER BY created_at DESC LIMIT 50")
    LiveData<List<TokenTransactionEntity>> getRecentLive(String userId);

    @Query("SELECT * FROM token_transactions WHERE user_id = :userId ORDER BY created_at DESC LIMIT :limit")
    List<TokenTransactionEntity> getRecent(String userId, int limit);

    @Query("SELECT * FROM token_transactions WHERE needs_sync = 1")
    List<TokenTransactionEntity> getPendingSync();

    @Query("UPDATE token_transactions SET needs_sync = 0 WHERE id = :id")
    void markSynced(String id);

    @Query("DELETE FROM token_transactions WHERE user_id = :userId AND created_at < :beforeTs")
    void pruneOlderThan(String userId, long beforeTs);
}
