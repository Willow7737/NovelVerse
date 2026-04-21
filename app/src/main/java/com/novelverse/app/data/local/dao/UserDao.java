package com.novelverse.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.novelverse.app.data.local.entities.UserEntity;

import java.util.List;

/**
 * DAO for User operations
 */
@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UserEntity user);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<UserEntity> users);

    @Update
    void update(UserEntity user);

    @Delete
    void delete(UserEntity user);

    @Query("DELETE FROM users WHERE id = :userId")
    void deleteById(String userId);

    @Query("SELECT * FROM users WHERE id = :userId")
    LiveData<UserEntity> getUserById(String userId);

    @Query("SELECT * FROM users WHERE id = :userId")
    UserEntity getUserByIdSync(String userId);

    @Query("SELECT * FROM users WHERE is_current_user = 1 LIMIT 1")
    LiveData<UserEntity> getCurrentUser();

    @Query("SELECT * FROM users WHERE is_current_user = 1 LIMIT 1")
    UserEntity getCurrentUserSync();

    @Query("SELECT * FROM users WHERE username = :username")
    UserEntity getUserByUsername(String username);

    @Query("SELECT * FROM users WHERE email = :email")
    UserEntity getUserByEmail(String email);

    @Query("UPDATE users SET is_current_user = 0")
    void clearCurrentUser();

    @Query("UPDATE users SET is_current_user = 1 WHERE id = :userId")
    void setCurrentUser(String userId);

    @Query("UPDATE users SET auth_token = :token, token_expires_at = :expiresAt WHERE id = :userId")
    void updateAuthToken(String userId, String token, long expiresAt);

    @Query("UPDATE users SET refresh_token = :token WHERE id = :userId")
    void updateRefreshToken(String userId, String token);

    @Query("UPDATE users SET points_balance = :points WHERE id = :userId")
    void updatePointsBalance(String userId, int points);

    @Query("UPDATE users SET theme_preference = :theme WHERE id = :userId")
    void updateThemePreference(String userId, String theme);

    @Query("UPDATE users SET font_size = :fontSize, line_spacing = :lineSpacing WHERE id = :userId")
    void updateReadingPreferences(String userId, int fontSize, float lineSpacing);

    @Query("SELECT * FROM users WHERE is_dirty = 1")
    List<UserEntity> getDirtyUsers();

    @Query("UPDATE users SET is_dirty = 0, synced_at = :timestamp WHERE id = :userId")
    void markAsSynced(String userId, long timestamp);

    @Query("DELETE FROM users WHERE is_current_user = 0 AND synced_at < :timestamp")
    void deleteOldUsers(long timestamp);

    @Query("DELETE FROM users")
    void deleteAll();
}
