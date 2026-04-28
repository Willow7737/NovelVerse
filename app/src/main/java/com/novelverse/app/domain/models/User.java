package com.novelverse.app.domain.models;

import java.util.Date;

/**
 * Domain model for User
 */
public class User {

    private String id;
    private String username;
    private String displayName;
    private String email;
    private String avatarUrl;
    private String bio;
    private String role; // guest, reader, author, admin, moderator
    private int pointsBalance;
    private double totalSpent;
    private boolean isVerified;
    private boolean isEmailVerified;
    private String phoneNumber;
    private String languagePreference;
    private String themePreference;
    private int fontSize;
    private float lineSpacing;
    private int autoScrollSpeed;
    private float ttsSpeed;
    private boolean notificationsEnabled;
    private boolean emailNotifications;
    private boolean pushNotifications;
    private boolean marketingEmails;
    private String privacySetting;
    private Date lastActiveAt;
    private Date createdAt;
    private String subscriptionTier;
    private Date subscriptionExpiresAt;
    private double totalEarnings;
    private double availableForPayout;
    private boolean isBanned;
    private int followersCount;
    private int followingCount;

    // Profile redesign fields
    private String coverUrl;      // Full-width banner/cover photo URL (nullable)
    private String userStatus;    // "online" | "away" | "offline" — stored for quick read; derived from last_active_at

    // Constructor
    public User() {
        this.pointsBalance = 0;
        this.totalSpent = 0.0;
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
        this.followersCount = 0;
        this.followingCount = 0;
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

    public int getPointsBalance() { return pointsBalance; }
    public void setPointsBalance(int pointsBalance) { this.pointsBalance = pointsBalance; }

    public double getTotalSpent() { return totalSpent; }
    public void setTotalSpent(double totalSpent) { this.totalSpent = totalSpent; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public boolean isEmailVerified() { return isEmailVerified; }
    public void setEmailVerified(boolean emailVerified) { isEmailVerified = emailVerified; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getLanguagePreference() { return languagePreference; }
    public void setLanguagePreference(String languagePreference) { this.languagePreference = languagePreference; }

    public String getThemePreference() { return themePreference; }
    public void setThemePreference(String themePreference) { this.themePreference = themePreference; }

    public int getFontSize() { return fontSize; }
    public void setFontSize(int fontSize) { this.fontSize = fontSize; }

    public float getLineSpacing() { return lineSpacing; }
    public void setLineSpacing(float lineSpacing) { this.lineSpacing = lineSpacing; }

    public int getAutoScrollSpeed() { return autoScrollSpeed; }
    public void setAutoScrollSpeed(int autoScrollSpeed) { this.autoScrollSpeed = autoScrollSpeed; }

    public float getTtsSpeed() { return ttsSpeed; }
    public void setTtsSpeed(float ttsSpeed) { this.ttsSpeed = ttsSpeed; }

    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }

    public boolean isEmailNotifications() { return emailNotifications; }
    public void setEmailNotifications(boolean emailNotifications) { this.emailNotifications = emailNotifications; }

    public boolean isPushNotifications() { return pushNotifications; }
    public void setPushNotifications(boolean pushNotifications) { this.pushNotifications = pushNotifications; }

    public boolean isMarketingEmails() { return marketingEmails; }
    public void setMarketingEmails(boolean marketingEmails) { this.marketingEmails = marketingEmails; }

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

    public double getTotalEarnings() { return totalEarnings; }
    public void setTotalEarnings(double totalEarnings) { this.totalEarnings = totalEarnings; }

    public double getAvailableForPayout() { return availableForPayout; }
    public void setAvailableForPayout(double availableForPayout) { this.availableForPayout = availableForPayout; }

    public boolean isBanned() { return isBanned; }
    public void setBanned(boolean banned) { isBanned = banned; }

    public int getFollowersCount() { return followersCount; }
    public void setFollowersCount(int followersCount) { this.followersCount = followersCount; }

    public int getFollowingCount() { return followingCount; }
    public void setFollowingCount(int followingCount) { this.followingCount = followingCount; }

    // ── Profile redesign: cover photo + status ──────────────────────────────

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    /** Persisted status string: "online", "away", or "offline". */
    public String getUserStatus() { return userStatus; }
    public void setUserStatus(String userStatus) { this.userStatus = userStatus; }

    /**
     * Check if user is a guest
     */
    public boolean isGuest() {
        return "guest".equals(role);
    }

    /**
     * Check if user is an author
     */
    public boolean isAuthor() {
        return "author".equals(role) || "admin".equals(role);
    }

    /**
     * Check if user is an admin
     */
    public boolean isAdmin() {
        return "admin".equals(role);
    }

    /**
     * Check if user has premium subscription
     */
    public boolean isPremium() {
        return "premium".equals(subscriptionTier) || "vip".equals(subscriptionTier);
    }

    /**
     * Get display name or username fallback
     */
    public String getDisplayNameOrUsername() {
        return displayName != null && !displayName.isEmpty() ? displayName : username;
    }
}
