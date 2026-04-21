package com.novelverse.app.data.local.dao;

import android.database.Cursor;
import androidx.lifecycle.LiveData;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;

import com.novelverse.app.data.local.database.Converters;
import com.novelverse.app.data.local.entities.UserEntity;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unchecked", "deprecation"})
public final class UserDao_Impl implements UserDao {

    private final RoomDatabase __db;
    private final EntityInsertionAdapter<UserEntity> __insertionAdapter;
    private final EntityDeletionOrUpdateAdapter<UserEntity> __deletionAdapter;
    private final EntityDeletionOrUpdateAdapter<UserEntity> __updateAdapter;

    public UserDao_Impl(RoomDatabase db) {
        this.__db = db;
        this.__insertionAdapter = new EntityInsertionAdapter<UserEntity>(db) {
            @Override public String createQuery() {
                return "INSERT OR REPLACE INTO `users` (`id`,`username`,`display_name`,`email`,`avatar_url`,"
                     + "`bio`,`role`,`points_balance`,`total_spent`,`is_verified`,`is_email_verified`,"
                     + "`phone_number`,`language_preference`,`theme_preference`,`font_size`,`line_spacing`,"
                     + "`auto_scroll_speed`,`tts_speed`,`notifications_enabled`,`email_notifications`,"
                     + "`push_notifications`,`marketing_emails`,`privacy_setting`,`last_active_at`,"
                     + "`created_at`,`subscription_tier`,`subscription_expires_at`,`total_earnings`,"
                     + "`available_for_payout`,`is_banned`,`followers_count`,`following_count`,"
                     + "`is_current_user`,`auth_token`,`refresh_token`,`token_expires_at`,`synced_at`,`is_dirty`)"
                     + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            }
            @Override public void bind(SupportSQLiteStatement s, UserEntity e) { bindUser(s, e); }
        };
        this.__deletionAdapter = new EntityDeletionOrUpdateAdapter<UserEntity>(db) {
            @Override public String createQuery() { return "DELETE FROM `users` WHERE `id` = ?"; }
            @Override public void bind(SupportSQLiteStatement s, UserEntity e) { s.bindString(1, e.getId()); }
        };
        this.__updateAdapter = new EntityDeletionOrUpdateAdapter<UserEntity>(db) {
            @Override public String createQuery() {
                return "UPDATE OR ABORT `users` SET `id`=?,`username`=?,`display_name`=?,`email`=?,`avatar_url`=?,"
                     + "`bio`=?,`role`=?,`points_balance`=?,`total_spent`=?,`is_verified`=?,`is_email_verified`=?,"
                     + "`phone_number`=?,`language_preference`=?,`theme_preference`=?,`font_size`=?,`line_spacing`=?,"
                     + "`auto_scroll_speed`=?,`tts_speed`=?,`notifications_enabled`=?,`email_notifications`=?,"
                     + "`push_notifications`=?,`marketing_emails`=?,`privacy_setting`=?,`last_active_at`=?,"
                     + "`created_at`=?,`subscription_tier`=?,`subscription_expires_at`=?,`total_earnings`=?,"
                     + "`available_for_payout`=?,`is_banned`=?,`followers_count`=?,`following_count`=?,"
                     + "`is_current_user`=?,`auth_token`=?,`refresh_token`=?,`token_expires_at`=?,`synced_at`=?,`is_dirty`=?"
                     + " WHERE `id`=?";
            }
            @Override public void bind(SupportSQLiteStatement s, UserEntity e) { bindUser(s, e); s.bindString(39, e.getId()); }
        };
    }

    private void bindUser(SupportSQLiteStatement s, UserEntity e) {
        s.bindString(1, e.getId());
        if (e.getUsername() == null) s.bindNull(2); else s.bindString(2, e.getUsername());
        if (e.getDisplayName() == null) s.bindNull(3); else s.bindString(3, e.getDisplayName());
        if (e.getEmail() == null) s.bindNull(4); else s.bindString(4, e.getEmail());
        if (e.getAvatarUrl() == null) s.bindNull(5); else s.bindString(5, e.getAvatarUrl());
        if (e.getBio() == null) s.bindNull(6); else s.bindString(6, e.getBio());
        if (e.getRole() == null) s.bindNull(7); else s.bindString(7, e.getRole());
        if (e.getPointsBalance() == null) s.bindNull(8); else s.bindLong(8, e.getPointsBalance());
        if (e.getTotalSpent() == null) s.bindNull(9); else s.bindDouble(9, e.getTotalSpent());
        final Integer ver = Converters.booleanToInteger(e.getIsVerified()); if (ver == null) s.bindNull(10); else s.bindLong(10, ver);
        final Integer eVer = Converters.booleanToInteger(e.getIsEmailVerified()); if (eVer == null) s.bindNull(11); else s.bindLong(11, eVer);
        if (e.getPhoneNumber() == null) s.bindNull(12); else s.bindString(12, e.getPhoneNumber());
        if (e.getLanguagePreference() == null) s.bindNull(13); else s.bindString(13, e.getLanguagePreference());
        if (e.getThemePreference() == null) s.bindNull(14); else s.bindString(14, e.getThemePreference());
        if (e.getFontSize() == null) s.bindNull(15); else s.bindLong(15, e.getFontSize());
        if (e.getLineSpacing() == null) s.bindNull(16); else s.bindDouble(16, e.getLineSpacing());
        if (e.getAutoScrollSpeed() == null) s.bindNull(17); else s.bindLong(17, e.getAutoScrollSpeed());
        if (e.getTtsSpeed() == null) s.bindNull(18); else s.bindDouble(18, e.getTtsSpeed());
        final Integer notif = Converters.booleanToInteger(e.getNotificationsEnabled()); if (notif == null) s.bindNull(19); else s.bindLong(19, notif);
        final Integer eNotif = Converters.booleanToInteger(e.getEmailNotifications()); if (eNotif == null) s.bindNull(20); else s.bindLong(20, eNotif);
        final Integer pNotif = Converters.booleanToInteger(e.getPushNotifications()); if (pNotif == null) s.bindNull(21); else s.bindLong(21, pNotif);
        final Integer mkt = Converters.booleanToInteger(e.getMarketingEmails()); if (mkt == null) s.bindNull(22); else s.bindLong(22, mkt);
        if (e.getPrivacySetting() == null) s.bindNull(23); else s.bindString(23, e.getPrivacySetting());
        final Long lastActive = Converters.dateToTimestamp(e.getLastActiveAt()); if (lastActive == null) s.bindNull(24); else s.bindLong(24, lastActive);
        final Long createdAt = Converters.dateToTimestamp(e.getCreatedAt()); if (createdAt == null) s.bindNull(25); else s.bindLong(25, createdAt);
        if (e.getSubscriptionTier() == null) s.bindNull(26); else s.bindString(26, e.getSubscriptionTier());
        final Long subExp = Converters.dateToTimestamp(e.getSubscriptionExpiresAt()); if (subExp == null) s.bindNull(27); else s.bindLong(27, subExp);
        if (e.getTotalEarnings() == null) s.bindNull(28); else s.bindDouble(28, e.getTotalEarnings());
        if (e.getAvailableForPayout() == null) s.bindNull(29); else s.bindDouble(29, e.getAvailableForPayout());
        final Integer banned = Converters.booleanToInteger(e.getIsBanned()); if (banned == null) s.bindNull(30); else s.bindLong(30, banned);
        if (e.getFollowersCount() == null) s.bindNull(31); else s.bindLong(31, e.getFollowersCount());
        if (e.getFollowingCount() == null) s.bindNull(32); else s.bindLong(32, e.getFollowingCount());
        final Integer curr = Converters.booleanToInteger(e.getIsCurrentUser()); if (curr == null) s.bindNull(33); else s.bindLong(33, curr);
        if (e.getAuthToken() == null) s.bindNull(34); else s.bindString(34, e.getAuthToken());
        if (e.getRefreshToken() == null) s.bindNull(35); else s.bindString(35, e.getRefreshToken());
        final Long tokExp = Converters.dateToTimestamp(e.getTokenExpiresAt()); if (tokExp == null) s.bindNull(36); else s.bindLong(36, tokExp);
        final Long sync = Converters.dateToTimestamp(e.getSyncedAt()); if (sync == null) s.bindNull(37); else s.bindLong(37, sync);
        final Integer dirty = Converters.booleanToInteger(e.getIsDirty()); if (dirty == null) s.bindNull(38); else s.bindLong(38, dirty);
    }

    private UserEntity cursorToUser(Cursor c) {
        UserEntity e = new UserEntity();
        e.setId(c.getString(CursorUtil.getColumnIndexOrThrow(c, "id")));
        e.setUsername(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "username")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "username")));
        e.setDisplayName(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "display_name")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "display_name")));
        e.setEmail(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "email")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "email")));
        e.setAvatarUrl(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "avatar_url")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "avatar_url")));
        e.setBio(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "bio")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "bio")));
        e.setRole(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "role")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "role")));
        e.setPointsBalance(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "points_balance")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "points_balance")));
        e.setTotalSpent(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "total_spent")) ? null : c.getDouble(CursorUtil.getColumnIndexOrThrow(c, "total_spent")));
        e.setIsVerified(Converters.fromInteger(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "is_verified")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "is_verified"))));
        e.setIsEmailVerified(Converters.fromInteger(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "is_email_verified")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "is_email_verified"))));
        e.setPhoneNumber(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "phone_number")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "phone_number")));
        e.setLanguagePreference(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "language_preference")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "language_preference")));
        e.setThemePreference(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "theme_preference")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "theme_preference")));
        e.setFontSize(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "font_size")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "font_size")));
        e.setLineSpacing(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "line_spacing")) ? null : c.getFloat(CursorUtil.getColumnIndexOrThrow(c, "line_spacing")));
        e.setAutoScrollSpeed(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "auto_scroll_speed")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "auto_scroll_speed")));
        e.setTtsSpeed(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "tts_speed")) ? null : c.getFloat(CursorUtil.getColumnIndexOrThrow(c, "tts_speed")));
        e.setNotificationsEnabled(Converters.fromInteger(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "notifications_enabled")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "notifications_enabled"))));
        e.setEmailNotifications(Converters.fromInteger(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "email_notifications")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "email_notifications"))));
        e.setPushNotifications(Converters.fromInteger(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "push_notifications")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "push_notifications"))));
        e.setMarketingEmails(Converters.fromInteger(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "marketing_emails")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "marketing_emails"))));
        e.setPrivacySetting(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "privacy_setting")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "privacy_setting")));
        e.setLastActiveAt(Converters.fromTimestamp(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "last_active_at")) ? null : c.getLong(CursorUtil.getColumnIndexOrThrow(c, "last_active_at"))));
        e.setCreatedAt(Converters.fromTimestamp(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "created_at")) ? null : c.getLong(CursorUtil.getColumnIndexOrThrow(c, "created_at"))));
        e.setSubscriptionTier(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "subscription_tier")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "subscription_tier")));
        e.setSubscriptionExpiresAt(Converters.fromTimestamp(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "subscription_expires_at")) ? null : c.getLong(CursorUtil.getColumnIndexOrThrow(c, "subscription_expires_at"))));
        e.setTotalEarnings(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "total_earnings")) ? null : c.getDouble(CursorUtil.getColumnIndexOrThrow(c, "total_earnings")));
        e.setAvailableForPayout(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "available_for_payout")) ? null : c.getDouble(CursorUtil.getColumnIndexOrThrow(c, "available_for_payout")));
        e.setIsBanned(Converters.fromInteger(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "is_banned")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "is_banned"))));
        e.setFollowersCount(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "followers_count")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "followers_count")));
        e.setFollowingCount(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "following_count")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "following_count")));
        e.setIsCurrentUser(Converters.fromInteger(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "is_current_user")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "is_current_user"))));
        e.setAuthToken(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "auth_token")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "auth_token")));
        e.setRefreshToken(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "refresh_token")) ? null : c.getString(CursorUtil.getColumnIndexOrThrow(c, "refresh_token")));
        e.setTokenExpiresAt(Converters.fromTimestamp(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "token_expires_at")) ? null : c.getLong(CursorUtil.getColumnIndexOrThrow(c, "token_expires_at"))));
        e.setSyncedAt(Converters.fromTimestamp(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "synced_at")) ? null : c.getLong(CursorUtil.getColumnIndexOrThrow(c, "synced_at"))));
        e.setIsDirty(Converters.fromInteger(c.isNull(CursorUtil.getColumnIndexOrThrow(c, "is_dirty")) ? null : c.getInt(CursorUtil.getColumnIndexOrThrow(c, "is_dirty"))));
        return e;
    }

    @Override public void insert(UserEntity user) { __db.assertNotSuspendingTransaction(); __db.beginTransaction(); try { __insertionAdapter.insert(user); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void insertAll(List<UserEntity> users) { __db.assertNotSuspendingTransaction(); __db.beginTransaction(); try { __insertionAdapter.insert(users); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void update(UserEntity user) { __db.assertNotSuspendingTransaction(); __db.beginTransaction(); try { __updateAdapter.handle(user); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void delete(UserEntity user) { __db.assertNotSuspendingTransaction(); __db.beginTransaction(); try { __deletionAdapter.handle(user); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void deleteById(String id) { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("DELETE FROM users WHERE id = ?"); __db.beginTransaction(); try { s.bindString(1, id); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void clearCurrentUser() { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("UPDATE users SET is_current_user = 0"); __db.beginTransaction(); try { s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void setCurrentUser(String userId) { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("UPDATE users SET is_current_user = 1 WHERE id = ?"); __db.beginTransaction(); try { s.bindString(1, userId); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void updateAuthToken(String userId, String token, long expiresAt) { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("UPDATE users SET auth_token = ?, token_expires_at = ? WHERE id = ?"); __db.beginTransaction(); try { s.bindString(1, token); s.bindLong(2, expiresAt); s.bindString(3, userId); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void updateRefreshToken(String userId, String token) { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("UPDATE users SET refresh_token = ? WHERE id = ?"); __db.beginTransaction(); try { s.bindString(1, token); s.bindString(2, userId); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void updatePointsBalance(String userId, int points) { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("UPDATE users SET points_balance = ? WHERE id = ?"); __db.beginTransaction(); try { s.bindLong(1, points); s.bindString(2, userId); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void updateThemePreference(String userId, String theme) { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("UPDATE users SET theme_preference = ? WHERE id = ?"); __db.beginTransaction(); try { s.bindString(1, theme); s.bindString(2, userId); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void updateReadingPreferences(String userId, int fontSize, float lineSpacing) { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("UPDATE users SET font_size = ?, line_spacing = ? WHERE id = ?"); __db.beginTransaction(); try { s.bindLong(1, fontSize); s.bindDouble(2, lineSpacing); s.bindString(3, userId); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void markAsSynced(String userId, long ts) { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("UPDATE users SET is_dirty = 0, synced_at = ? WHERE id = ?"); __db.beginTransaction(); try { s.bindLong(1, ts); s.bindString(2, userId); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void deleteOldUsers(long ts) { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("DELETE FROM users WHERE is_current_user = 0 AND synced_at < ?"); __db.beginTransaction(); try { s.bindLong(1, ts); s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }
    @Override public void deleteAll() { __db.assertNotSuspendingTransaction(); final SupportSQLiteStatement s = __db.compileStatement("DELETE FROM users"); __db.beginTransaction(); try { s.executeUpdateDelete(); __db.setTransactionSuccessful(); } finally { __db.endTransaction(); } }

    @Override public UserEntity getUserByIdSync(String id) { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM users WHERE id = ?", 1); q.bindString(1, id); final Cursor c = DBUtil.query(__db, q, false, null); try { return c.moveToFirst() ? cursorToUser(c) : null; } finally { c.close(); q.release(); } }
    @Override public UserEntity getCurrentUserSync() { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM users WHERE is_current_user = 1 LIMIT 1", 0); final Cursor c = DBUtil.query(__db, q, false, null); try { return c.moveToFirst() ? cursorToUser(c) : null; } finally { c.close(); q.release(); } }
    @Override public UserEntity getUserByUsername(String username) { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM users WHERE username = ?", 1); q.bindString(1, username); final Cursor c = DBUtil.query(__db, q, false, null); try { return c.moveToFirst() ? cursorToUser(c) : null; } finally { c.close(); q.release(); } }
    @Override public UserEntity getUserByEmail(String email) { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM users WHERE email = ?", 1); q.bindString(1, email); final Cursor c = DBUtil.query(__db, q, false, null); try { return c.moveToFirst() ? cursorToUser(c) : null; } finally { c.close(); q.release(); } }
    @Override public List<UserEntity> getDirtyUsers() { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM users WHERE is_dirty = 1", 0); final Cursor c = DBUtil.query(__db, q, false, null); try { List<UserEntity> r = new ArrayList<>(); while (c.moveToNext()) r.add(cursorToUser(c)); return r; } finally { c.close(); q.release(); } }

    @Override public LiveData<UserEntity> getUserById(String id) { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM users WHERE id = ?", 1); q.bindString(1, id); return __db.getInvalidationTracker().createLiveData(new String[]{"users"}, false, () -> { final Cursor c = DBUtil.query(__db, q, false, null); try { return c.moveToFirst() ? cursorToUser(c) : null; } finally { c.close(); } }); }
    @Override public LiveData<UserEntity> getCurrentUser() { final RoomSQLiteQuery q = RoomSQLiteQuery.acquire("SELECT * FROM users WHERE is_current_user = 1 LIMIT 1", 0); return __db.getInvalidationTracker().createLiveData(new String[]{"users"}, false, () -> { final Cursor c = DBUtil.query(__db, q, false, null); try { return c.moveToFirst() ? cursorToUser(c) : null; } finally { c.close(); } }); }
}
