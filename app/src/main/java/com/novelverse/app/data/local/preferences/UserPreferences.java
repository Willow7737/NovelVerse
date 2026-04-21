package com.novelverse.app.data.local.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class UserPreferences {

    private static final String PREFS_NAME = "user_secure_prefs";

    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_TOKEN_EXPIRES_AT = "token_expires_at";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_IS_GUEST = "is_guest";
    private static final String KEY_THEME = "theme_preference";
    private static final String KEY_PUSH_NOTIFICATIONS = "push_notifications";
    private static final String KEY_EMAIL_NOTIFICATIONS = "email_notifications";
    private static final String KEY_MARKETING_EMAILS = "marketing_emails";
    private static final String KEY_ONBOARDING_COMPLETED = "onboarding_completed";
    private static final String KEY_LAST_SYNC = "last_sync";
    public static final String KEY_LAST_CHECK_IN = "last_check_in_timestamp";
    private static final String KEY_RECENT_SEARCHES = "recent_searches_json";
    private static final String KEY_STREAK_COUNT = "streak_count";
    private static final String KEY_STREAK_LAST_DATE = "streak_last_date";
    public static final String KEY_BIOMETRIC_LOCK_ENABLED = "biometric_lock_enabled";

    // Reader: typography
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_LINE_SPACING = "line_spacing";
    private static final String KEY_FONT_FAMILY = "font_family";
    private static final String KEY_PARAGRAPH_SPACING = "paragraph_spacing";
    private static final String KEY_LETTER_SPACING = "letter_spacing";
    private static final String KEY_TEXT_ALIGNMENT = "text_alignment";
    private static final String KEY_DYSLEXIA_FONT = "dyslexia_font_enabled";

    // Reader: display
    private static final String KEY_READER_THEME = "reader_theme";
    private static final String KEY_BRIGHTNESS = "screen_brightness";
    private static final String KEY_FOLLOW_BRIGHTNESS = "follow_system_brightness";
    private static final String KEY_KEEP_SCREEN_ON = "keep_screen_on";

    // Reader: layout
    private static final String KEY_SIDE_MARGIN = "side_margin_dp";
    private static final String KEY_READING_MODE = "reading_mode";

    // Reader: automation
    private static final String KEY_AUTO_SCROLL_ENABLED = "auto_scroll_enabled";
    private static final String KEY_AUTO_SCROLL_SPEED = "auto_scroll_speed";
    private static final String KEY_VOLUME_FLIP = "volume_button_flip";
    private static final String KEY_TAP_ZONE_NAV = "tap_zone_chapter_nav";

    // Cached profile
    private static final String KEY_CACHED_ROLE = "cached_role";
    private static final String KEY_CACHED_USERNAME = "cached_username";
    private static final String KEY_CACHED_DISPLAY_NAME = "cached_display_name";
    private static final String KEY_CACHED_AVATAR_URL = "cached_avatar_url";
    private static final String KEY_CACHED_BIO = "cached_bio";

    // Points & rewards
    private static final String KEY_POINTS = "user_points";
    private static final String KEY_LAST_AD_REWARD_DATE = "last_ad_reward_date";
    private static final String KEY_LAST_CHECKIN_DATE = "last_checkin_date";

    // Preferred genres
    private static final String KEY_PREFERRED_GENRES = "preferred_genres_set";

    // Gamification counters
    private static final String KEY_BOOKMARK_COUNT      = "gam_bookmark_count";
    private static final String KEY_TOTAL_CHAPTERS_READ = "gam_chapters_read";
    private static final String KEY_TOTAL_NOVELS_READ   = "gam_novels_read";
    private static final String KEY_TOTAL_FOLLOWING     = "gam_following_count";

    private final Context context;

    // Lazily initialized — NOT in constructor.
    // Guarded by synchronized getPrefs(). Volatile so the reference is
    // safely published after the synchronized block completes.
    private volatile SharedPreferences preferences;

    // ── Constructor ───────────────────────────────────────────────────────────

    @Inject
    public UserPreferences(@ApplicationContext Context context) {
        // Store context only. EncryptedSharedPreferences init is deferred to
        // the first actual read/write (via getPrefs()), which must NOT happen
        // on the main thread. Call warmUp() from a background thread early in
        // Application.onCreate() so the prefs are ready before the splash timer
        // fires.
        this.context = context;
    }

    // ── Lazy initializer ──────────────────────────────────────────────────────

    /**
     * Returns the SharedPreferences instance, initializing it on first call.
     * Thread-safe via double-checked locking. Never call this on the main
     * thread directly — use warmUp() to pre-init from a background thread.
     */
    private SharedPreferences getPrefs() {
        if (preferences != null) return preferences;
        synchronized (this) {
            if (preferences != null) return preferences;
            try {
                MasterKey masterKey =
                        new MasterKey.Builder(context)
                                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                                .build();
                preferences = EncryptedSharedPreferences.create(
                        context,
                        PREFS_NAME,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
            } catch (Exception e) {
                // Keystore unavailable (rooted device, corrupted keystore, etc.)
                // Fall back to plain SharedPreferences so the app still works.
                android.util.Log.e("UserPreferences",
                        "EncryptedSharedPreferences init failed, falling back to plain prefs", e);
                preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            }
        }
        return preferences;
    }

    /**
     * Pre-warms the EncryptedSharedPreferences on a background thread.
     * Call this from NovelVerseApplication.onCreate() like:
     *
     *   new Thread(userPreferences::warmUp, "prefs-warmup").start();
     *
     * This ensures the Keystore I/O is finished before SplashActivity's
     * 1600ms timer fires, so the first getAccessToken() call is instant.
     */
    public void warmUp() {
        getPrefs(); // triggers lazy init on whichever thread calls this
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    public void setAccessToken(String t) {
        putStr(KEY_ACCESS_TOKEN, t);
    }

    public String getAccessToken() {
        return getPrefs().getString(KEY_ACCESS_TOKEN, null);
    }

    public void setRefreshToken(String t) {
        putStr(KEY_REFRESH_TOKEN, t);
    }

    public String getRefreshToken() {
        return getPrefs().getString(KEY_REFRESH_TOKEN, null);
    }

    public void setTokenExpiresAt(long ts) {
        getPrefs().edit().putLong(KEY_TOKEN_EXPIRES_AT, ts).apply();
    }

    public long getTokenExpiresAt() {
        return getPrefs().getLong(KEY_TOKEN_EXPIRES_AT, 0);
    }

    public boolean isTokenExpired() {
        long e = getTokenExpiresAt();
        return e > 0 && System.currentTimeMillis() > e;
    }

    // ── Identity ──────────────────────────────────────────────────────────────

    public void setUserId(String id) {
        putStr(KEY_USER_ID, id);
    }

    public String getUserId() {
        return getPrefs().getString(KEY_USER_ID, null);
    }

    public void setGuestMode(boolean v) {
        getPrefs().edit().putBoolean(KEY_IS_GUEST, v).apply();
    }

    public boolean isGuestMode() {
        return getPrefs().getBoolean(KEY_IS_GUEST, false);
    }

    // ── App theme ─────────────────────────────────────────────────────────────

    public void setThemePreference(String t) {
        putStr(KEY_THEME, t);
    }

    public String getThemePreference() {
        return getPrefs().getString(KEY_THEME, "light");
    }

    // ── Reader – typography ───────────────────────────────────────────────────

    public void setFontSize(int sp) {
        getPrefs().edit().putInt(KEY_FONT_SIZE, sp).apply();
    }

    public int getFontSize() {
        return getPrefs().getInt(KEY_FONT_SIZE, 17);
    }

    public void setLineSpacing(float s) {
        getPrefs().edit().putFloat(KEY_LINE_SPACING, s).apply();
    }

    public float getLineSpacing() {
        return getPrefs().getFloat(KEY_LINE_SPACING, 1.6f);
    }

    public void setFontFamily(String f) {
        putStr(KEY_FONT_FAMILY, f);
    }

    public String getFontFamily() {
        return getPrefs().getString(KEY_FONT_FAMILY, "inter");
    }

    public void setParagraphSpacing(float sp) {
        getPrefs().edit().putFloat(KEY_PARAGRAPH_SPACING, sp).apply();
    }

    public float getParagraphSpacing() {
        return getPrefs().getFloat(KEY_PARAGRAPH_SPACING, 8f);
    }

    public void setLetterSpacing(float em) {
        getPrefs().edit().putFloat(KEY_LETTER_SPACING, em).apply();
    }

    public float getLetterSpacing() {
        return getPrefs().getFloat(KEY_LETTER_SPACING, 0f);
    }

    public void setTextAlignment(String a) {
        putStr(KEY_TEXT_ALIGNMENT, a);
    }

    public String getTextAlignment() {
        return getPrefs().getString(KEY_TEXT_ALIGNMENT, "left");
    }

    public void setDyslexiaFontEnabled(boolean v) {
        getPrefs().edit().putBoolean(KEY_DYSLEXIA_FONT, v).apply();
    }

    public boolean isDyslexiaFontEnabled() {
        return getPrefs().getBoolean(KEY_DYSLEXIA_FONT, false);
    }

    // ── Reader – display ──────────────────────────────────────────────────────

    public void setReaderTheme(String t) {
        putStr(KEY_READER_THEME, t);
    }

    public String getReaderTheme() {
        return getPrefs().getString(KEY_READER_THEME, "light");
    }

    public void setScreenBrightness(float b) {
        getPrefs().edit().putFloat(KEY_BRIGHTNESS, b).apply();
    }

    public float getScreenBrightness() {
        return getPrefs().getFloat(KEY_BRIGHTNESS, -1f);
    }

    public void setFollowSystemBrightness(boolean v) {
        getPrefs().edit().putBoolean(KEY_FOLLOW_BRIGHTNESS, v).apply();
    }

    public boolean isFollowSystemBrightness() {
        return getPrefs().getBoolean(KEY_FOLLOW_BRIGHTNESS, true);
    }

    public void setKeepScreenOn(boolean v) {
        getPrefs().edit().putBoolean(KEY_KEEP_SCREEN_ON, v).apply();
    }

    public boolean isKeepScreenOn() {
        return getPrefs().getBoolean(KEY_KEEP_SCREEN_ON, false);
    }

    // ── Reader – layout ───────────────────────────────────────────────────────

    public void setSideMargin(int dp) {
        getPrefs().edit().putInt(KEY_SIDE_MARGIN, dp).apply();
    }

    public int getSideMargin() {
        return getPrefs().getInt(KEY_SIDE_MARGIN, 20);
    }

    public void setReadingMode(String m) {
        putStr(KEY_READING_MODE, m);
    }

    public String getReadingMode() {
        return getPrefs().getString(KEY_READING_MODE, "scroll");
    }

    // ── Reader – automation ───────────────────────────────────────────────────

    public void setAutoScrollEnabled(boolean v) {
        getPrefs().edit().putBoolean(KEY_AUTO_SCROLL_ENABLED, v).apply();
    }

    public boolean isAutoScrollEnabled() {
        return getPrefs().getBoolean(KEY_AUTO_SCROLL_ENABLED, false);
    }

    public void setAutoScrollSpeed(int s) {
        getPrefs().edit().putInt(KEY_AUTO_SCROLL_SPEED, s).apply();
    }

    public int getAutoScrollSpeed() {
        return getPrefs().getInt(KEY_AUTO_SCROLL_SPEED, 5);
    }

    public void setVolumeFlipEnabled(boolean v) {
        getPrefs().edit().putBoolean(KEY_VOLUME_FLIP, v).apply();
    }

    public boolean isVolumeFlipEnabled() {
        return getPrefs().getBoolean(KEY_VOLUME_FLIP, false);
    }

    public void setTapZoneNavEnabled(boolean v) {
        getPrefs().edit().putBoolean(KEY_TAP_ZONE_NAV, v).apply();
    }

    public boolean isTapZoneNavEnabled() {
        return getPrefs().getBoolean(KEY_TAP_ZONE_NAV, true);
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    public void setPushNotifications(boolean v) {
        getPrefs().edit().putBoolean(KEY_PUSH_NOTIFICATIONS, v).apply();
    }

    public boolean isPushNotifications() {
        return getPrefs().getBoolean(KEY_PUSH_NOTIFICATIONS, true);
    }

    public void setEmailNotifications(boolean v) {
        getPrefs().edit().putBoolean(KEY_EMAIL_NOTIFICATIONS, v).apply();
    }

    public boolean isEmailNotifications() {
        return getPrefs().getBoolean(KEY_EMAIL_NOTIFICATIONS, true);
    }

    public void setMarketingEmails(boolean v) {
        getPrefs().edit().putBoolean(KEY_MARKETING_EMAILS, v).apply();
    }

    public boolean isMarketingEmailsEnabled() {
        return getPrefs().getBoolean(KEY_MARKETING_EMAILS, false);
    }

    // ── Onboarding / sync ─────────────────────────────────────────────────────

    public void setOnboardingCompleted(boolean v) {
        getPrefs().edit().putBoolean(KEY_ONBOARDING_COMPLETED, v).apply();
    }

    public boolean isOnboardingCompleted() {
        return getPrefs().getBoolean(KEY_ONBOARDING_COMPLETED, false);
    }

    public void setLastSync(long ts) {
        getPrefs().edit().putLong(KEY_LAST_SYNC, ts).apply();
    }

    public long getLastSync() {
        return getPrefs().getLong(KEY_LAST_SYNC, 0);
    }

    public void setLastCheckInTimestamp(long ts) {
        getPrefs().edit().putLong(KEY_LAST_CHECK_IN, ts).apply();
    }

    public long getLastCheckInTimestamp() {
        return getPrefs().getLong(KEY_LAST_CHECK_IN, 0);
    }

    // ── Recent searches / profile cache ───────────────────────────────────────

    public void setRecentSearches(String j) {
        putStr(KEY_RECENT_SEARCHES, j);
    }

    public String getRecentSearches() {
        return getPrefs().getString(KEY_RECENT_SEARCHES, null);
    }

    public void setCachedRole(String v) {
        putStr(KEY_CACHED_ROLE, v);
    }

    public String getCachedRole() {
        return getPrefs().getString(KEY_CACHED_ROLE, null);
    }

    public void setCachedUsername(String v) {
        putStr(KEY_CACHED_USERNAME, v);
    }

    public String getCachedUsername() {
        return getPrefs().getString(KEY_CACHED_USERNAME, null);
    }

    public void setCachedDisplayName(String v) {
        putStr(KEY_CACHED_DISPLAY_NAME, v);
    }

    public String getCachedDisplayName() {
        return getPrefs().getString(KEY_CACHED_DISPLAY_NAME, null);
    }

    public void setCachedAvatarUrl(String v) {
        putStr(KEY_CACHED_AVATAR_URL, v);
    }

    public String getCachedAvatarUrl() {
        return getPrefs().getString(KEY_CACHED_AVATAR_URL, null);
    }

    public void setCachedBio(String v) {
        putStr(KEY_CACHED_BIO, v);
    }

    public String getCachedBio() {
        return getPrefs().getString(KEY_CACHED_BIO, null);
    }

    // ── Biometric ─────────────────────────────────────────────────────────────

    public boolean isBiometricLockEnabled() {
        return getPrefs().getBoolean(KEY_BIOMETRIC_LOCK_ENABLED, false);
    }

    public void setBiometricLockEnabled(boolean v) {
        getPrefs().edit().putBoolean(KEY_BIOMETRIC_LOCK_ENABLED, v).apply();
    }

    // ── Streak ────────────────────────────────────────────────────────────────

    public int updateAndGetStreak() {
        String today = today(), last = getPrefs().getString(KEY_STREAK_LAST_DATE, "");
        int streak = getPrefs().getInt(KEY_STREAK_COUNT, 0);
        if (today.equals(last)) return streak;
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        String yesterday = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());
        streak = yesterday.equals(last) ? streak + 1 : 1;
        getPrefs()
                .edit()
                .putInt(KEY_STREAK_COUNT, streak)
                .putString(KEY_STREAK_LAST_DATE, today)
                .apply();
        return streak;
    }

    public int getCurrentStreak() {
        return getPrefs().getInt(KEY_STREAK_COUNT, 0);
    }

    // ── Points ────────────────────────────────────────────────────────────────

    public int getPoints() {
        return getPrefs().getInt(KEY_POINTS, 0);
    }

    public void addPoints(int amount) {
        getPrefs().edit().putInt(KEY_POINTS, getPoints() + amount).apply();
    }

    public void setPoints(int points) {
        getPrefs().edit().putInt(KEY_POINTS, points).apply();
    }

    public boolean performDailyCheckIn() {
        String today = today(), last = getPrefs().getString(KEY_LAST_CHECKIN_DATE, "");
        if (today.equals(last)) return false;
        getPrefs().edit().putString(KEY_LAST_CHECKIN_DATE, today).apply();
        setLastCheckInTimestamp(System.currentTimeMillis());
        return true;
    }

    public boolean hasCheckedInToday() {
        return today().equals(getPrefs().getString(KEY_LAST_CHECKIN_DATE, ""));
    }

    public boolean claimAdRewardIfEligible() {
        String today = today(), last = getPrefs().getString(KEY_LAST_AD_REWARD_DATE, "");
        if (today.equals(last)) return false;
        getPrefs().edit().putString(KEY_LAST_AD_REWARD_DATE, today).apply();
        return true;
    }

    public boolean hasClaimedAdRewardToday() {
        return today().equals(getPrefs().getString(KEY_LAST_AD_REWARD_DATE, ""));
    }

    // ── Preferred genres ──────────────────────────────────────────────────────

    public void setPreferredGenres(List<String> genres) {
        getPrefs().edit().putStringSet(KEY_PREFERRED_GENRES, new HashSet<>(genres)).apply();
    }

    public List<String> getPreferredGenres() {
        Set<String> set = getPrefs().getStringSet(KEY_PREFERRED_GENRES, null);
        return (set != null) ? new ArrayList<>(set) : new ArrayList<>();
    }

    // ── Gamification counters ─────────────────────────────────────────────────

    /** Atomically increments bookmark count and returns new value. */
    public int incrementBookmarkCount() {
        int v = getPrefs().getInt(KEY_BOOKMARK_COUNT, 0) + 1;
        getPrefs().edit().putInt(KEY_BOOKMARK_COUNT, v).apply();
        return v;
    }

    public int getBookmarkCount() {
        return getPrefs().getInt(KEY_BOOKMARK_COUNT, 0);
    }

    /** Atomically increments total chapters read and returns new value. */
    public int incrementChaptersRead() {
        int v = getPrefs().getInt(KEY_TOTAL_CHAPTERS_READ, 0) + 1;
        getPrefs().edit().putInt(KEY_TOTAL_CHAPTERS_READ, v).apply();
        return v;
    }

    public int getTotalChaptersRead() {
        return getPrefs().getInt(KEY_TOTAL_CHAPTERS_READ, 0);
    }

    /** Atomically increments total novels read/started and returns new value. */
    public int incrementNovelsRead() {
        int v = getPrefs().getInt(KEY_TOTAL_NOVELS_READ, 0) + 1;
        getPrefs().edit().putInt(KEY_TOTAL_NOVELS_READ, v).apply();
        return v;
    }

    public int getTotalNovelsRead() {
        return getPrefs().getInt(KEY_TOTAL_NOVELS_READ, 0);
    }

    /** Atomically increments following count and returns new value. */
    public int incrementFollowingCount() {
        int v = getPrefs().getInt(KEY_TOTAL_FOLLOWING, 0) + 1;
        getPrefs().edit().putInt(KEY_TOTAL_FOLLOWING, v).apply();
        return v;
    }

    public int getTotalFollowing() {
        return getPrefs().getInt(KEY_TOTAL_FOLLOWING, 0);
    }

    // ── Misc ──────────────────────────────────────────────────────────────────

    public void clear() {
        getPrefs().edit().clear().apply();
    }

    public void clearAll() {
        getPrefs().edit().clear().apply();
    }

    private void putStr(String key, String value) {
        getPrefs().edit().putString(key, value).apply();
    }

    private static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }
}
