package com.novelverse.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.novelverse.app.data.local.entities.UserCurrencyEntity;

@Dao
public interface UserCurrencyDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(UserCurrencyEntity entity);

    @Update
    void update(UserCurrencyEntity entity);

    @Query("SELECT * FROM user_currency WHERE user_id = :userId")
    UserCurrencyEntity get(String userId);

    @Query("SELECT * FROM user_currency WHERE user_id = :userId")
    LiveData<UserCurrencyEntity> getLive(String userId);

    @Query("UPDATE user_currency SET ink_balance = ink_balance + :delta, version = version + 1, needs_sync = 1 WHERE user_id = :userId")
    void addInk(String userId, int delta);

    @Query("UPDATE user_currency SET quill_balance = quill_balance + :delta, version = version + 1, needs_sync = 1 WHERE user_id = :userId")
    void addQuill(String userId, int delta);

    @Query("UPDATE user_currency SET ink_balance = :ink, quill_balance = :quill, version = :version, last_synced_at = :ts, needs_sync = 0 WHERE user_id = :userId")
    void applyServerState(String userId, int ink, int quill, long version, long ts);

    @Query("SELECT * FROM user_currency WHERE needs_sync = 1")
    java.util.List<UserCurrencyEntity> getPendingSync();
}
