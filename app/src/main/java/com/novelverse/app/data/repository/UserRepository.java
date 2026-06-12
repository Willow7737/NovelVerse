package com.novelverse.app.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.novelverse.app.data.local.dao.UserDao;
import com.novelverse.app.data.local.entities.UserEntity;
import com.novelverse.app.data.local.preferences.UserPreferences;
import com.novelverse.app.data.remote.api.UserApi;
import com.novelverse.app.data.remote.supabase.SupabaseAuthService;
import com.novelverse.app.data.remote.supabase.SupabaseDatabaseService;
import com.novelverse.app.domain.models.AuthResult;
import com.novelverse.app.domain.models.User;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Repository for user-related operations.
 *
 * <p>KEY FIXES vs. original: 1. checkExistingSession() now fetches the FULL profile from the
 * Supabase `profiles` table after the auth token check, so role/bio/displayName/avatarUrl survive
 * restarts. 2. updateProfile() now waits for Supabase confirmation before calling the success
 * callback — "Profile saved!" only shows when the DB actually accepted the write. 3.
 * updateReadingPreferences() persists font/spacing/theme to UserPreferences so they survive
 * restarts without a network round-trip. 4. signInWithGoogle() now accepts the idToken and forwards
 * it to SupabaseAuthService so it can be exchanged for a Supabase session via the id_token grant.
 */
@Singleton
public class UserRepository {

    private static final String TAG = "UserRepository";
    private static final String PROFILES = "profiles";

    private final UserDao userDao;
    private final SupabaseAuthService authService;
    private final UserPreferences userPreferences;
    private final UserApi userApi;
    private final SupabaseDatabaseService dbService;
    private final ExecutorService executor;
    private final Gson gson = new Gson();

    private final MutableLiveData<User> currentUser = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoggedIn = new MutableLiveData<>(false);

    @Inject
    public UserRepository(
            UserDao userDao,
            SupabaseAuthService authService,
            UserPreferences userPreferences,
            UserApi userApi,
            SupabaseDatabaseService dbService) {
        this.userDao = userDao;
        this.authService = authService;
        this.userPreferences = userPreferences;
        this.userApi = userApi;
        this.dbService = dbService;
        this.executor = Executors.newSingleThreadExecutor();

        checkExistingSession();
    }

    // ── LiveData getters ──────────────────────────────────────────────────────

    public LiveData<User> getCurrentUser() {
        return currentUser;
    }

    public LiveData<Boolean> isLoggedIn() {
        return isLoggedIn;
    }

    public boolean isGuest() {
        User u = currentUser.getValue();
        return u != null && u.isGuest();
    }

    public boolean isAuthor() {
        User u = currentUser.getValue();
        return u != null && u.isAuthor();
    }

    public boolean isAdmin() {
        User u = currentUser.getValue();
        return u != null && u.isAdmin();
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    public void signUp(String email, String password, String username, AuthCallback cb) {
        authService.signUp(
                email,
                password,
                username,
                result -> {
                    if (result.isSuccess()) {
                        saveUserSession(result);
                        cb.onSuccess(result.getUser());
                    } else if (result.isPendingVerification()) {
                        // Account created but email confirmation required — not an error.
                        cb.onPendingVerification(result.getUser());
                    } else {
                        cb.onError(result.getErrorMessage());
                    }
                });
    }

    public void signIn(String email, String password, AuthCallback cb) {
        authService.signIn(
                email,
                password,
                result -> {
                    if (result.isSuccess()) {
                        saveUserSession(result);
                        fetchAndMergeProfile(result.getUser(), result.getAccessToken(), cb);
                    } else {
                        cb.onError(result.getErrorMessage());
                    }
                });
    }

    /**
     * Sign in with a Google ID token obtained via the native Google Sign-In SDK.
     *
     * @param idToken  The ID token from GoogleSignInAccount.getIdToken().
     * @param rawNonce The raw nonce used to build the hashed nonce passed to
     *                 GoogleSignInOptions.requestNonce(). Supabase validates it
     *                 by comparing sha256(rawNonce) to the nonce in the ID token.
     */
    public void signInWithGoogle(String idToken, String rawNonce, AuthCallback cb) {
        authService.signInWithGoogleIdToken(
                idToken,
                rawNonce,
                result -> {
                    if (result.isSuccess()) {
                        saveUserSession(result);
                        fetchAndMergeProfile(result.getUser(), result.getAccessToken(), cb);
                    } else if (result.isPendingVerification()) {
                        cb.onSuccess(result.getUser());
                    } else {
                        cb.onError(result.getErrorMessage());
                    }
                });
    }

    /**
     * Exchanges a Supabase PKCE authorization code for a full session.
     * Called after the OAuth PKCE callback deep-link is received.
     *
     * @param authCode     The code from the deep-link query parameter.
     * @param codeVerifier The original code verifier generated before opening the browser.
     */
    public void exchangeOAuthCode(String authCode, String codeVerifier, AuthCallback cb) {
        authService.exchangeOAuthCode(
                authCode,
                codeVerifier,
                result -> {
                    if (result.isSuccess()) {
                        saveUserSession(result);
                        fetchAndMergeProfile(result.getUser(), result.getAccessToken(), cb);
                    } else {
                        cb.onError(result.getErrorMessage());
                    }
                });
    }

    public void continueAsGuest() {
        User guest = authService.createGuestUser();
        currentUser.postValue(guest);
        isLoggedIn.postValue(true);
        userPreferences.setGuestMode(true);
    }

    public void signOut(SimpleCallback cb) {
        String token = userPreferences.getAccessToken();
        if (token != null && !token.isEmpty()) {
            authService.signOut(
                    token,
                    (success, error) -> {
                        clearUserSession();
                        cb.onResult(success, error);
                    });
        } else {
            clearUserSession();
            cb.onResult(true, null);
        }
    }

    /** The deep-link URI Supabase will redirect to after verifying the recovery token. */
    private static final String RESET_REDIRECT_URI = "novelverse://auth/reset";

    public void resetPassword(String email, SimpleCallback cb) {
        authService.resetPassword(email, RESET_REDIRECT_URI,
                (success, error) -> cb.onResult(success, error));
    }

    /**
     * Updates the password for a user who arrived via a recovery deep link.
     * Uses the temporary access token extracted from the link's URL fragment.
     */
    public void updatePasswordWithToken(String accessToken, String newPassword, SimpleCallback cb) {
        authService.updatePasswordWithToken(accessToken, newPassword,
                (success, error) -> cb.onResult(success, error));
    }

    public void resendVerification(String email, SimpleCallback cb) {
        authService.resendVerification(email, (success, error) -> cb.onResult(success, error));
    }

    // ── Profile update — waits for Supabase confirmation ─────────────────────

    public void updateProfile(User user, SimpleCallback callback) {
        currentUser.postValue(user);
        cacheProfileFields(user);

        executor.execute(
                () -> {
                    try {
                        UserEntity entity = mapToEntity(user);
                        entity.setDirty(false);
                        userDao.update(entity);
                    } catch (Exception ignored) {
                    }
                });

        String token = userPreferences.getAccessToken();
        if (token == null || user.getId() == null) {
            callback.onResult(false, "Not authenticated");
            return;
        }

        JsonObject body = new JsonObject();
        // Note: id is not included — it's in the PATCH URL filter (?id=eq.{id}).
        // Email is intentionally excluded: changing it requires going through auth, not profiles.

        String username = user.getUsername();
        if (username != null && !username.isEmpty()) {
            body.addProperty("username", username);
        }

        if (user.getDisplayName() != null) body.addProperty("display_name", user.getDisplayName());
        if (user.getBio() != null) body.addProperty("bio", user.getBio());
        if (user.getAvatarUrl() != null) body.addProperty("avatar_url", user.getAvatarUrl());
        if (user.getRole() != null) body.addProperty("role", user.getRole());
        // Profile redesign
        if (user.getCoverUrl() != null) body.addProperty("cover_url", user.getCoverUrl());
        if (user.getUserStatus() != null) body.addProperty("user_status", user.getUserStatus());

        if (user.getFontSize() > 0) body.addProperty("font_size", user.getFontSize());
        if (user.getLineSpacing() > 0) body.addProperty("line_spacing", user.getLineSpacing());
        if (user.getThemePreference() != null)
            body.addProperty("theme_preference", user.getThemePreference());

        // PATCH instead of upsert: profile rows always exist after sign-up.
        // POST upsert caused NOT NULL failures (username) when any required column
        // was absent from the body, because the INSERT phase runs unconditionally.
        dbService.update(
                PROFILES,
                user.getId(),
                body,
                token,
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override
                    public void onSuccess(String r) {
                        android.util.Log.d(TAG, "Profile synced to Supabase ✓");
                        callback.onResult(true, null);
                    }

                    @Override
                    public void onError(String e) {
                        android.util.Log.w(TAG, "Profile sync failed: " + e);
                        executor.execute(
                                () -> {
                                    try {
                                        UserEntity entity = mapToEntity(user);
                                        entity.setDirty(true);
                                        userDao.update(entity);
                                    } catch (Exception ignored) {
                                    }
                                });
                        callback.onResult(false, "Couldn't reach server: " + e);
                    }
                });
    }

    // ── Reading preferences ───────────────────────────────────────────────────

    public void updateReadingPreferences(int fontSize, float lineSpacing, String theme) {
        executor.execute(
                () -> {
                    User user = currentUser.getValue();
                    if (user != null) {
                        user.setFontSize(fontSize);
                        user.setLineSpacing(lineSpacing);
                        user.setThemePreference(theme);
                        userDao.updateReadingPreferences(user.getId(), fontSize, lineSpacing);
                        currentUser.postValue(user);
                    }
                    userPreferences.setFontSize(fontSize);
                    userPreferences.setLineSpacing(lineSpacing);
                    userPreferences.setThemePreference(theme);
                });

        String token = userPreferences.getAccessToken();
        String userId = userPreferences.getUserId();
        if (token == null || userId == null) return;

        JsonObject body = new JsonObject();
        body.addProperty("id", userId);
        body.addProperty("font_size", fontSize);
        body.addProperty("line_spacing", lineSpacing);
        body.addProperty("theme_preference", theme != null ? theme : "light");

        dbService.upsert(
                PROFILES,
                body,
                token,
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override
                    public void onSuccess(String r) {
                        android.util.Log.d(TAG, "Reading prefs synced ✓");
                    }

                    @Override
                    public void onError(String e) {
                        android.util.Log.w(TAG, "Reading prefs sync failed: " + e);
                    }
                });
    }

    // ── Notification preferences ──────────────────────────────────────────────

    public void updateNotificationPreferences(
            boolean pushEnabled, boolean emailEnabled, boolean marketingEnabled) {
        executor.execute(
                () -> {
                    User user = currentUser.getValue();
                    if (user != null) {
                        user.setPushNotifications(pushEnabled);
                        user.setEmailNotifications(emailEnabled);
                        user.setMarketingEmails(marketingEnabled);
                        userPreferences.setPushNotifications(pushEnabled);
                        userPreferences.setEmailNotifications(emailEnabled);
                        userPreferences.setMarketingEmails(marketingEnabled);
                        currentUser.postValue(user);
                    }
                });
    }

    // ── Misc ──────────────────────────────────────────────────────────────────

    public LiveData<UserEntity> getUserById(String userId) {
        return userDao.getUserById(userId);
    }

    public void refreshCurrentUser(SimpleCallback cb) {
        String token = userPreferences.getAccessToken();
        if (token == null || token.isEmpty()) {
            cb.onResult(false, "No active session");
            return;
        }
        authService.getCurrentUser(
                token,
                (user, error) -> {
                    if (user != null) {
                        fetchAndMergeProfileThen(
                                user,
                                token,
                                mergedUser -> {
                                    currentUser.postValue(mergedUser);
                                    executor.execute(
                                            () -> {
                                                UserEntity entity = mapToEntity(mergedUser);
                                                entity.setCurrentUser(true);
                                                userDao.insert(entity);
                                            });
                                    cb.onResult(true, null);
                                });
                    } else {
                        cb.onResult(false, error);
                    }
                });
    }

    public void changePassword(String newPassword, SimpleCallback cb) {
        String token = userPreferences.getAccessToken();
        if (token == null) {
            cb.onResult(false, "Not logged in");
            return;
        }
        authService.updatePassword(
                token, newPassword, (success, error) -> cb.onResult(success, error));
    }

    public void addPoints(int amount, String reason, SimpleCallback cb) {
        String userId = userPreferences.getUserId();
        String token = userPreferences.getAccessToken();
        if (userId == null || token == null) {
            cb.onResult(false, "Not logged in");
            return;
        }

        executor.execute(
                () -> {
                    User user = currentUser.getValue();
                    if (user != null) {
                        user.setPointsBalance(user.getPointsBalance() + amount);
                        currentUser.postValue(user);
                        userDao.update(mapToEntity(user));
                    }
                });

        JsonObject params = new JsonObject();
        params.addProperty("p_user_id", userId);
        params.addProperty("p_amount", amount);
        params.addProperty("p_reason", reason != null ? reason : "");
        dbService.callRpc(
                "add_user_points",
                params,
                token,
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override
                    public void onSuccess(String r) {
                        cb.onResult(true, null);
                    }

                    @Override
                    public void onError(String e) {
                        executor.execute(
                                () -> {
                                    User user = currentUser.getValue();
                                    if (user != null) {
                                        user.setPointsBalance(
                                                Math.max(0, user.getPointsBalance() - amount));
                                        currentUser.postValue(user);
                                        userDao.update(mapToEntity(user));
                                    }
                                });
                        cb.onResult(false, e);
                    }
                });
    }

    public void uploadAvatar(
            String userId, byte[] bytes, com.novelverse.app.domain.utils.BiCallback<String> cb) {
        String path = userId;
        String token = userPreferences.getAccessToken();
        dbService.uploadFile(
                "user-avatars",
                path,
                bytes,
                "image/jpeg",
                token,
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override
                    public void onSuccess(String r) {
                        String publicUrl =
                                dbService.getPublicUrl("user-avatars", path)
                                        + "?t="
                                        + System.currentTimeMillis();
                        cb.onResult(publicUrl, null);
                    }

                    @Override
                    public void onError(String e) {
                        cb.onResult(null, e);
                    }
                });
    }

    /**
     * Generic file upload to any Supabase Storage bucket.
     * Used for profile cover photos (bucket = "cover-photos").
     */
    public void uploadFile(String bucket, String path, byte[] bytes, String mimeType,
                           com.novelverse.app.domain.utils.BiCallback<String> cb) {
        String token = userPreferences.getAccessToken();
        dbService.uploadFile(bucket, path, bytes, mimeType, token,
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override public void onSuccess(String r) {
                        String publicUrl = dbService.getPublicUrl(bucket, path)
                                + "?t=" + System.currentTimeMillis();
                        cb.onResult(publicUrl, null);
                    }
                    @Override public void onError(String e) { cb.onResult(null, e); }
                });
    }

    // ── Session management ────────────────────────────────────────────────────

    private void checkExistingSession() {
        String token = userPreferences.getAccessToken();
        String refreshToken = userPreferences.getRefreshToken();

        if (token != null && !token.isEmpty()) {
            authService.getCurrentUser(
                    token,
                    (authUser, error) -> {
                        if (authUser != null) {
                            fetchAndMergeProfileThen(
                                    authUser,
                                    token,
                                    mergedUser -> {
                                        currentUser.postValue(mergedUser);
                                        isLoggedIn.postValue(true);
                                    });
                        } else if (refreshToken != null && !refreshToken.isEmpty()) {
                            doRefreshToken(refreshToken);
                        } else {
                            isLoggedIn.postValue(false);
                        }
                    });
        } else {
            isLoggedIn.postValue(false);
        }
    }

    private void fetchAndMergeProfileThen(
            User authUser, String token, java.util.function.Consumer<User> onReady) {
        dbService.selectById(
                PROFILES,
                authUser.getId(),
                token,
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override
                    public void onSuccess(String result) {
                        try {
                            JsonObject profile = gson.fromJson(result, JsonObject.class);
                            if (profile != null) mergeProfileJson(authUser, profile);
                        } catch (Exception e) {
                            android.util.Log.w(TAG, "Profile merge failed: " + e.getMessage());
                        }
                        onReady.accept(authUser);
                    }

                    @Override
                    public void onError(String error) {
                        android.util.Log.w(
                                TAG,
                                "Profile fetch failed: " + error + " — using auth user fallback");
                        onReady.accept(authUser);
                    }
                });
    }

    private void fetchAndMergeProfile(User authUser, String token, AuthCallback cb) {
        fetchAndMergeProfileThen(
                authUser,
                token,
                mergedUser -> {
                    currentUser.postValue(mergedUser);
                    cb.onSuccess(mergedUser);
                });
    }

    private void mergeProfileJson(User user, JsonObject p) {
        if (has(p, "username")) user.setUsername(p.get("username").getAsString());
        if (has(p, "display_name")) user.setDisplayName(p.get("display_name").getAsString());
        if (has(p, "bio")) user.setBio(p.get("bio").getAsString());
        if (has(p, "avatar_url")) user.setAvatarUrl(p.get("avatar_url").getAsString());
        if (has(p, "role")) user.setRole(p.get("role").getAsString());
        if (has(p, "points_balance")) user.setPointsBalance(p.get("points_balance").getAsInt());
        if (has(p, "font_size")) user.setFontSize(p.get("font_size").getAsInt());
        if (has(p, "line_spacing")) user.setLineSpacing(p.get("line_spacing").getAsFloat());
        if (has(p, "theme_preference"))
            user.setThemePreference(p.get("theme_preference").getAsString());
        if (has(p, "subscription_tier"))
            user.setSubscriptionTier(p.get("subscription_tier").getAsString());
        if (has(p, "followers_count")) user.setFollowersCount(p.get("followers_count").getAsInt());
        if (has(p, "following_count")) user.setFollowingCount(p.get("following_count").getAsInt());
        if (has(p, "total_earnings")) user.setTotalEarnings(p.get("total_earnings").getAsDouble());
        if (has(p, "available_for_payout"))
            user.setAvailableForPayout(p.get("available_for_payout").getAsDouble());
        if (has(p, "is_verified")) user.setVerified(p.get("is_verified").getAsBoolean());
        if (has(p, "is_email_verified"))
            user.setEmailVerified(p.get("is_email_verified").getAsBoolean());
        if (has(p, "is_banned")) user.setBanned(p.get("is_banned").getAsBoolean());
        // Profile redesign
        if (has(p, "cover_url"))    user.setCoverUrl(p.get("cover_url").getAsString());
        if (has(p, "user_status"))  user.setUserStatus(p.get("user_status").getAsString());
        // Parse last_active_at — ISO-8601 string from Supabase e.g. "2026-04-27T10:00:00.000Z"
        if (has(p, "last_active_at")) {
            try {
                String iso = p.get("last_active_at").getAsString();
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US);
                sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                // Strip microseconds / trailing Z for parsing
                String clean = iso.replaceAll("\\.\\d+Z?$", "");
                user.setLastActiveAt(sdf.parse(clean));
            } catch (Exception ignored) { /* keep null — shows offline */ }
        }
        // Always stamp the current time — this user is online right now
        java.util.Date now = new java.util.Date();
        user.setLastActiveAt(now);
        // Push last_active_at to Supabase asynchronously (fire-and-forget)
        touchLastActiveAt(user.getId());
    }

    /**
     * Updates last_active_at to NOW() in Supabase so the server trigger
     * can flip user_status → 'online'. Fire-and-forget; errors are silently ignored.
     */
    private void touchLastActiveAt(String userId) {
        if (userId == null) return;
        String token = userPreferences.getAccessToken();
        if (token == null) return;
        // Use PATCH not UPSERT - UPSERT would require username (NOT NULL) which we may not have.
        com.google.gson.JsonObject body = new com.google.gson.JsonObject();
        body.addProperty("last_active_at",
                new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US) {{
                    setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                }}.format(new java.util.Date()));
        dbService.update(PROFILES, userId, body, token, new SupabaseDatabaseService.DatabaseCallback() {
            @Override public void onSuccess(String r) { /* silent */ }
            @Override public void onError(String e) {
                android.util.Log.w(TAG, "touchLastActiveAt failed: " + e);
            }
        });
    }

    private boolean has(JsonObject o, String key) {
        return o != null && o.has(key) && !o.get(key).isJsonNull();
    }

    private void cacheProfileFields(User user) {
        if (user.getRole() != null) userPreferences.setCachedRole(user.getRole());
        if (user.getUsername() != null) userPreferences.setCachedUsername(user.getUsername());
        if (user.getDisplayName() != null)
            userPreferences.setCachedDisplayName(user.getDisplayName());
        if (user.getAvatarUrl() != null) userPreferences.setCachedAvatarUrl(user.getAvatarUrl());
        if (user.getBio() != null) userPreferences.setCachedBio(user.getBio());
        if (user.getFontSize() > 0) userPreferences.setFontSize(user.getFontSize());
        if (user.getLineSpacing() > 0) userPreferences.setLineSpacing(user.getLineSpacing());
        if (user.getThemePreference() != null)
            userPreferences.setThemePreference(user.getThemePreference());
    }

    private void doRefreshToken(String refreshToken) {
        authService.refreshToken(
                refreshToken,
                result -> {
                    if (result.isSuccess()) saveUserSession(result);
                    else clearUserSession();
                });
    }

    private void saveUserSession(AuthResult result) {
        User user = result.getUser();
        userPreferences.setAccessToken(result.getAccessToken());
        userPreferences.setRefreshToken(result.getRefreshToken());
        userPreferences.setTokenExpiresAt(
                System.currentTimeMillis() + (result.getExpiresIn() * 1000));
        userPreferences.setUserId(user.getId());
        userPreferences.setGuestMode(false);

        executor.execute(
                () -> {
                    UserEntity entity = mapToEntity(user);
                    entity.setAuthToken(result.getAccessToken());
                    entity.setRefreshToken(result.getRefreshToken());
                    entity.setTokenExpiresAt(
                            new Date(System.currentTimeMillis() + (result.getExpiresIn() * 1000)));
                    entity.setCurrentUser(true);
                    userDao.insert(entity);
                });

        currentUser.postValue(user);
        isLoggedIn.postValue(true);
    }

    private void clearUserSession() {
        userPreferences.clear();
        executor.execute(() -> userDao.clearCurrentUser());
        currentUser.postValue(null);
        isLoggedIn.postValue(false);
    }

    // ── Entity mapping ────────────────────────────────────────────────────────

    private UserEntity mapToEntity(User user) {
        UserEntity e = new UserEntity();
        e.setId(user.getId());
        e.setUsername(user.getUsername());
        e.setDisplayName(user.getDisplayName());
        e.setEmail(user.getEmail());
        e.setAvatarUrl(user.getAvatarUrl());
        e.setBio(user.getBio());
        e.setRole(user.getRole());
        e.setPointsBalance(user.getPointsBalance());
        e.setTotalSpent(user.getTotalSpent());
        e.setVerified(user.isVerified());
        e.setEmailVerified(user.isEmailVerified());
        e.setPhoneNumber(user.getPhoneNumber());
        e.setLanguagePreference(user.getLanguagePreference());
        e.setThemePreference(user.getThemePreference());
        e.setFontSize(user.getFontSize());
        e.setLineSpacing(user.getLineSpacing());
        e.setAutoScrollSpeed(user.getAutoScrollSpeed());
        e.setTtsSpeed(user.getTtsSpeed());
        e.setNotificationsEnabled(user.isNotificationsEnabled());
        e.setEmailNotifications(user.isEmailNotifications());
        e.setPushNotifications(user.isPushNotifications());
        e.setMarketingEmails(user.isMarketingEmails());
        e.setPrivacySetting(user.getPrivacySetting());
        e.setLastActiveAt(user.getLastActiveAt());
        e.setCreatedAt(user.getCreatedAt());
        e.setSubscriptionTier(user.getSubscriptionTier());
        e.setSubscriptionExpiresAt(user.getSubscriptionExpiresAt());
        e.setTotalEarnings(user.getTotalEarnings());
        e.setAvailableForPayout(user.getAvailableForPayout());
        e.setBanned(user.isBanned());
        e.setFollowersCount(user.getFollowersCount());
        e.setFollowingCount(user.getFollowingCount());
        // Profile redesign
        e.setCoverUrl(user.getCoverUrl());
        e.setUserStatus(user.getUserStatus());
        return e;
    }

    // ── Callback interfaces ───────────────────────────────────────────────────

    public interface AuthCallback {
        void onSuccess(User user);

        /**
         * Called when the account was created but email confirmation is still required.
         * Default falls back to onError so existing call-sites continue to compile.
         */
        default void onPendingVerification(User user) {
            onError("Account created! Please check your email to verify your address, then sign in.");
        }

        void onError(String error);
    }

    public interface SimpleCallback {
        void onResult(boolean success, String error);
    }
}