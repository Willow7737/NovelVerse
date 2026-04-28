package com.novelverse.app.data.local.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.novelverse.app.data.local.database.Converters;

import java.util.Date;

/**
 * Room entity for User profile data (offline cache)
 */
@Entity(
    tableName = "users",
    indices = {
        @Index(value = "username", unique = true),
        @Index(value = "email", unique = true),
        @Index(value = "role"),
        @Index(value = "synced_at")
    }
)
@TypeConverters(Converters.class)
public class UserEntity {

    @PrimaryKey
    @ColumnInfo(name = "id")
    private String id;

    @ColumnInfo(name = "username")
    private String username;

    @ColumnInfo(name = "display_name")
    private String displayName;

    @ColumnInfo(name = "email")
    private String email;

    @ColumnInfo(name = "avatar_url")
    private String avatarUrl;

    @ColumnInfo(name = "bio")
    private String bio;

    @ColumnInfo(name = "role")
    private String role; // guest, reader, author, admin, moderator

    @ColumnInfo(name = "points_balance")
    private Integer pointsBalance;

    @ColumnInfo(name = "total_spent")
    private Double totalSpent;

    @ColumnInfo(name = "is_verified")
    private Boolean isVerified;

    @ColumnInfo(name = "is_email_verified")
    private Boolean isEmailVerified;

    @ColumnInfo(name = "phone_number")
    private String phoneNumber;

    @ColumnInfo(name = "language_preference")
    private String languagePreference;

    @ColumnInfo(name = "theme_preference")
    private String themePreference;

    @ColumnInfo(name = "font_size")
    private Integer fontSize;

    @ColumnInfo(name = "line_spacing")
    private Float lineSpacing;

    @ColumnInfo(name = "auto_scroll_speed")
    private Integer autoScrollSpeed;

    @ColumnInfo(name = "tts_speed")
    private Float ttsSpeed;

    @ColumnInfo(name = "notifications_enabled")
    private Boolean notificationsEnabled;

    @ColumnInfo(name = "email_notifications")
    private Boolean emailNotifications;

    @ColumnInfo(name = "push_notifications")
    private Boolean pushNotifications;

    @ColumnInfo(name = "marketing_emails")
    private Boolean marketingEmails;

    @ColumnInfo(name = "privacy_setting")
    private String privacySetting; // public, friends, private

    @ColumnInfo(name = "last_active_at")
    private Date lastActiveAt;

    @ColumnInfo(name = "created_at")
    private Date createdAt;

    @ColumnInfo(name = "subscription_tier")
    private String subscriptionTier; // free, premium, vip

    @ColumnInfo(name = "subscription_expires_at")
    private Date subscriptionExpiresAt;

    @ColumnInfo(name = "total_earnings")
    private Double totalEarnings;

    @ColumnInfo(name = "available_for_payout")
    private Double availableForPayout;

    @ColumnInfo(name = "is_banned")
    private Boolean isBanned;

    @ColumnInfo(name = "followers_count")
    private Integer followersCount;

    @ColumnInfo(name = "following_count")
    private Integer followingCount;

    // ── Profile redesign ────────────────────────────────────────────────────

    /** Full-width cover photo / banner URL. Nullable — falls back to genre gradient. */
    @ColumnInfo(name = "cover_url")
    private String coverUrl;

    /** Cached status string: "online", "away", "offline". Refreshed from last_active_at. */
    @ColumnInfo(name = "user_status")
    private String userStatus;

    // Local fields
    @ColumnInfo(name = "is_current_user")
    private Boolean isCurrentUser;

    @ColumnInfo(name = "auth_token")
    private String authToken;

    @ColumnInfo(name = "refresh_token")
    private String refreshToken;

    @ColumnInfo(name = "token_expires_at")
    private Date tokenExpiresAt;

    @ColumnInfo(name = "synced_at")
    private Date syncedAt;

    @ColumnInfo(name = "is_dirty")
    private Boolean isDirty;

    public UserEntity() {
        this.pointsBalance = 0;
        this.totalSpent = 0.0;
        this.isVerified = false;
        this.isEmailVerified = false;
        this.languagePreference = "en";
        this.themePreference = "light";
        this.fontSize = 16;
        this.lineSpacing = 1.5f;
        this.autoScrollSpeed = 50;
        this.ttsSpeed = 1.0f;
        this.notificationsEnabled = true;
        this.emailNotifications = true;
        this.pushNotifications = true;
        this.marketingEmails = false;
        this.privacySetting = "public";
        this.subscriptionTier = "free";
        this.totalEarnings = 0.0;
        this.availableForPayout = 0.0;
        this.isBanned = false;
        this.followersCount = 0;
        this.followingCount = 0;
        this.isCurrentUser = false;
        this.isDirty = false;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Integer getPointsBalance() { return pointsBalance; }
    public void setPointsBalance(Integer pointsBalance) { this.pointsBalance = pointsBalance; }

    public Double getTotalSpent() { return totalSpent; }
    public void setTotalSpent(Double totalSpent) { this.totalSpent = totalSpent; }

    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }

    public Boolean getIsEmailVerified() { return isEmailVerified; }
    public void setIsEmailVerified(Boolean isEmailVerified) { this.isEmailVerified = isEmailVerified; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getLanguagePreference() { return languagePreference; }
    public void setLanguagePreference(String languagePreference) { this.languagePreference = languagePreference; }

    public String getThemePreference() { return themePreference; }
    public void setThemePreference(String themePreference) { this.themePreference = themePreference; }

    public Integer getFontSize() { return fontSize; }
    public void setFontSize(Integer fontSize) { this.fontSize = fontSize; }

    public Float getLineSpacing() { return lineSpacing; }
    public void setLineSpacing(Float lineSpacing) { this.lineSpacing = lineSpacing; }

    public Integer getAutoScrollSpeed() { return autoScrollSpeed; }
    public void setAutoScrollSpeed(Integer autoScrollSpeed) { this.autoScrollSpeed = autoScrollSpeed; }

    public Float getTtsSpeed() { return ttsSpeed; }
    public void setTtsSpeed(Float ttsSpeed) { this.ttsSpeed = ttsSpeed; }

    public Boolean getNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(Boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }

    public Boolean getEmailNotifications() { return emailNotifications; }
    public void setEmailNotifications(Boolean emailNotifications) { this.emailNotifications = emailNotifications; }

    public Boolean getPushNotifications() { return pushNotifications; }
    public void setPushNotifications(Boolean pushNotifications) { this.pushNotifications = pushNotifications; }

    public Boolean getMarketingEmails() { return marketingEmails; }
    public void setMarketingEmails(Boolean marketingEmails) { this.marketingEmails = marketingEmails; }

    public String getPrivacySetting() { return privacySetting; }
    public void setPrivacySetting(String privacySetting) { this.privacySetting = privacySetting; }

    public Date getLastActiveAt() { return lastActiveAt; }
    public void setLastActiveAt(Date lastActiveAt) { this.lastActiveAt = lastActiveAt; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public String getSubscriptionTier() { return subscriptionTier; }
    public void setSubscriptionTier(String subscriptionTier) { this.subscriptionTier = subscriptionTier; }

    public Date getSubscriptionExpiresAt() { return subscriptionExpiresAt; }
    public void setSubscriptionExpiresAt(Date subscriptionExpiresAt) { this.subscriptionExpiresAt = subscriptionExpiresAt; }

    public Double getTotalEarnings() { return totalEarnings; }
    public void setTotalEarnings(Double totalEarnings) { this.totalEarnings = totalEarnings; }

    public Double getAvailableForPayout() { return availableForPayout; }
    public void setAvailableForPayout(Double availableForPayout) { this.availableForPayout = availableForPayout; }

    public Boolean getIsBanned() { return isBanned; }
    public void setIsBanned(Boolean isBanned) { this.isBanned = isBanned; }

    public Integer getFollowersCount() { return followersCount; }
    public void setFollowersCount(Integer followersCount) { this.followersCount = followersCount; }

    public Integer getFollowingCount() { return followingCount; }
    public void setFollowingCount(Integer followingCount) { this.followingCount = followingCount; }

    // ── Profile redesign ────────────────────────────────────────────────────

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getUserStatus() { return userStatus; }
    public void setUserStatus(String userStatus) { this.userStatus = userStatus; }

    public Boolean getIsCurrentUser() { return isCurrentUser; }
    public void setIsCurrentUser(Boolean isCurrentUser) { this.isCurrentUser = isCurrentUser; }

    public String getAuthToken() { return authToken; }
    public void setAuthToken(String authToken) { this.authToken = authToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public Date getTokenExpiresAt() { return tokenExpiresAt; }
    public void setTokenExpiresAt(Date tokenExpiresAt) { this.tokenExpiresAt = tokenExpiresAt; }

    public Date getSyncedAt() { return syncedAt; }
    public void setSyncedAt(Date syncedAt) { this.syncedAt = syncedAt; }

    public Boolean getIsDirty() { return isDirty; }
    public void setIsDirty(Boolean isDirty) { this.isDirty = isDirty; }
    // Alias setters (UserRepository uses non-Is-prefixed names)
    public void setDirty(boolean v) { this.isDirty = v; }
    public void setCurrentUser(boolean v) { this.isCurrentUser = v; }
    public void setVerified(boolean v) { this.isVerified = v; }
    public void setEmailVerified(boolean v) { this.isEmailVerified = v; }
    public void setBanned(boolean v) { this.isBanned = v; }
}