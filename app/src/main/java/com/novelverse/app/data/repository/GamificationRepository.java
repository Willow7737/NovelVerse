package com.novelverse.app.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;

import com.novelverse.app.data.local.dao.AchievementDao;
import com.novelverse.app.data.local.dao.DailyCapDao;
import com.novelverse.app.data.local.dao.TokenTransactionDao;
import com.novelverse.app.data.local.dao.UserAchievementDao;
import com.novelverse.app.data.local.dao.UserCurrencyDao;
import com.novelverse.app.data.local.dao.UserLevelDao;
import com.novelverse.app.data.local.dao.UserStreakDao;
import com.novelverse.app.data.local.entities.AchievementEntity;
import com.novelverse.app.data.local.entities.DailyCapEntity;
import com.novelverse.app.data.local.entities.TokenTransactionEntity;
import com.novelverse.app.data.local.entities.UserAchievementEntity;
import com.novelverse.app.data.local.entities.UserCurrencyEntity;
import com.novelverse.app.data.local.entities.UserLevelEntity;
import com.novelverse.app.data.local.entities.UserStreakEntity;
import com.novelverse.app.data.remote.supabase.SupabaseDatabaseService;
import com.novelverse.app.domain.gamification.AchievementDefinitions;
import com.novelverse.app.domain.gamification.StreakEngine;
import com.novelverse.app.domain.gamification.XpLevelEngine;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.novelverse.app.domain.models.StreakHistoryItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Gamification repository — local-first, optimistic updates, server-wins conflict resolution.
 *
 * <p>Threading contract: • All methods that touch Room must run on the executor (never on the main
 * thread). • Methods returning void dispatch themselves via executor internally. • Methods that
 * must return a result use a ResultCallback so callers stay off-thread. • checkAchievementRateLimit
 * / unlockAchievement are synchronous helpers — they must only ever be called from within an
 * executor.execute() block (e.g. from AchievementEngine which is always dispatched by
 * ReaderActivity via executeAchievementCheck()).
 */
@Singleton
public class GamificationRepository {

    private static final String TAG = "GamificationRepo";
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private final AchievementDao achievementDao;
    private final UserAchievementDao userAchievementDao;
    private final UserCurrencyDao userCurrencyDao;
    private final TokenTransactionDao tokenTransactionDao;
    private final UserLevelDao userLevelDao;
    private final UserStreakDao userStreakDao;
    private final DailyCapDao dailyCapDao;
    private final SupabaseDatabaseService supabase;
    private final com.novelverse.app.data.local.preferences.UserPreferences userPreferences;
    final ExecutorService executor; // package-private so ViewModel can submit tasks

    // ── Callback interfaces ───────────────────────────────────────────────────

    /** Generic boolean result callback — delivered on the executor thread. */
    public interface ResultCallback {
        void onResult(boolean success);
    }

    /** Callback that delivers the streak history list on the OkHttp thread. */
    public interface HistoryCallback {
        void onResult(List<StreakHistoryItem> items);
    }

    /** Callback for ad-reward RPCs: success + ink awarded + failure reason string. */
    public interface AdRewardCallback {
        void onResult(boolean success, int inkAwarded, String reason);
    }

    // ── Constructor ───────────────────────────────────────────────────────────

    @Inject
    public GamificationRepository(
            AchievementDao achievementDao,
            UserAchievementDao userAchievementDao,
            UserCurrencyDao userCurrencyDao,
            TokenTransactionDao tokenTransactionDao,
            UserLevelDao userLevelDao,
            UserStreakDao userStreakDao,
            DailyCapDao dailyCapDao,
            SupabaseDatabaseService supabase,
            com.novelverse.app.data.local.preferences.UserPreferences userPreferences) {
        this.achievementDao = achievementDao;
        this.userAchievementDao = userAchievementDao;
        this.userCurrencyDao = userCurrencyDao;
        this.tokenTransactionDao = tokenTransactionDao;
        this.userLevelDao = userLevelDao;
        this.userStreakDao = userStreakDao;
        this.dailyCapDao = dailyCapDao;
        this.supabase = supabase;
        this.userPreferences = userPreferences;
        this.executor = Executors.newSingleThreadExecutor();
    }

    // ── Seeding ───────────────────────────────────────────────────────────────

    /** Call once at app start (idempotent — INSERT OR IGNORE). */
    public void seedAchievementsIfNeeded() {
        executor.execute(
                () -> {
                    if (achievementDao.count() == 0) {
                        achievementDao.insertAll(AchievementDefinitions.all());
                        Log.i(TAG, "Seeded " + achievementDao.count() + " achievements");
                    }
                });
    }

    /** Ensures gamification rows exist for a user (idempotent). */
    public void ensureUserRows(String userId, long nowMs) {
        executor.execute(
                () -> {
                    // Currency
                    UserCurrencyEntity currency = userCurrencyDao.get(userId);
                    if (currency == null) {
                        currency = new UserCurrencyEntity();
                        currency.setUserId(userId);
                        userCurrencyDao.insert(currency);
                    }
                    // Level
                    UserLevelEntity level = userLevelDao.get(userId);
                    if (level == null) {
                        level = new UserLevelEntity();
                        level.setUserId(userId);
                        level.setXpLevelTarget(XpLevelEngine.xpToCompleteLevel(1));
                        userLevelDao.insert(level);
                    }
                    // Streak
                    UserStreakEntity streak = userStreakDao.get(userId);
                    if (streak == null) {
                        streak = new UserStreakEntity();
                        streak.setUserId(userId);
                        streak.setGraceWindowStart(nowMs);
                        streak.setFreezeMonthResetAt(nowMs);
                        userStreakDao.insert(streak);
                    }
                    // Ensure user_achievement rows exist for all catalog entries
                    List<AchievementEntity> catalog = achievementDao.getAll();
                    for (AchievementEntity ach : catalog) {
                        if (userAchievementDao.get(userId, ach.getId()) == null) {
                            UserAchievementEntity ua = new UserAchievementEntity();
                            ua.setId(UUID.randomUUID().toString());
                            ua.setUserId(userId);
                            ua.setAchievementId(ach.getId());
                            userAchievementDao.insert(ua);
                        }
                    }
                });
    }

    // ── Live data observers ───────────────────────────────────────────────────

    public LiveData<UserCurrencyEntity> getCurrencyLive(String userId) {
        return userCurrencyDao.getLive(userId);
    }

    public LiveData<UserLevelEntity> getLevelLive(String userId) {
        return userLevelDao.getLive(userId);
    }

    public LiveData<UserStreakEntity> getStreakLive(String userId) {
        return userStreakDao.getLive(userId);
    }

    public LiveData<List<UserAchievementEntity>> getUnlockedAchievementsLive(String userId) {
        return userAchievementDao.getUnlockedLive(userId);
    }

    public LiveData<List<UserAchievementEntity>> getAllUserAchievementsLive(String userId) {
        return userAchievementDao.getAllForUserLive(userId);
    }

    public LiveData<List<AchievementEntity>> getCatalogLive() {
        return achievementDao.getAllLive();
    }

    public LiveData<List<TokenTransactionEntity>> getRecentTransactionsLive(String userId) {
        return tokenTransactionDao.getRecentLive(userId);
    }

    // ── Currency operations (optimistic) ─────────────────────────────────────

    public void addInk(String userId, int amount, String type, String reason, long nowMs) {
        executor.execute(
                () -> {
                    if (amount <= 0) return;

                    if ("EARN_READING".equals(type)) {
                        String dateKey = DATE_FMT.format(new Date(nowMs));
                        ensureDailyCapRow(userId, dateKey);
                        DailyCapEntity cap = dailyCapDao.get(userId, dateKey);
                        if (cap != null && cap.getInkFromReading() >= cap.getReadingCap()) {
                            Log.d(TAG, "Daily reading Ink cap reached for " + userId);
                            return;
                        }
                        dailyCapDao.addReadingInk(userId, dateKey, amount);
                    }

                    userCurrencyDao.addInk(userId, amount);
                    UserCurrencyEntity after = userCurrencyDao.get(userId);
                    recordTransaction(
                            userId,
                            type,
                            amount,
                            0,
                            after != null ? after.getInkBalance() : 0,
                            after != null ? after.getQuillBalance() : 0,
                            reason,
                            nowMs);
                    syncCurrencyToServer(userId);
                });
    }

    public void spendInk(String userId, int amount, String type, String reason, long nowMs) {
        executor.execute(() -> spendInkInternal(userId, amount, type, reason, nowMs));
    }

    /** Synchronous variant — only call from within an executor.execute() block. */
    private void spendInkInternal(
            String userId, int amount, String type, String reason, long nowMs) {
        UserCurrencyEntity cur = userCurrencyDao.get(userId);
        if (cur == null || cur.getInkBalance() < amount) {
            Log.w(TAG, "Insufficient Ink for " + userId);
            return;
        }
        userCurrencyDao.addInk(userId, -amount);
        UserCurrencyEntity after = userCurrencyDao.get(userId);
        recordTransaction(
                userId,
                type,
                -amount,
                0,
                after != null ? after.getInkBalance() : 0,
                after != null ? after.getQuillBalance() : 0,
                reason,
                nowMs);
        syncCurrencyToServer(userId);
    }

    public void spendQuill(String userId, int amount, String type, String reason, long nowMs) {
        executor.execute(() -> spendQuillInternal(userId, amount, type, reason, nowMs));
    }

    /** Synchronous variant — only call from within an executor.execute() block. */
    private void spendQuillInternal(
            String userId, int amount, String type, String reason, long nowMs) {
        UserCurrencyEntity cur = userCurrencyDao.get(userId);
        if (cur == null || cur.getQuillBalance() < amount) {
            Log.w(TAG, "Insufficient Quill for " + userId);
            return;
        }
        userCurrencyDao.addQuill(userId, -amount);
        UserCurrencyEntity after = userCurrencyDao.get(userId);
        recordTransaction(
                userId,
                type,
                0,
                -amount,
                after != null ? after.getInkBalance() : 0,
                after != null ? after.getQuillBalance() : 0,
                reason,
                nowMs);
        syncCurrencyToServer(userId);
    }

    // ── Achievement unlock ────────────────────────────────────────────────────

    /**
     * Mark an achievement as unlocked and credit its rewards. Idempotent — safe to call multiple
     * times, second call is a no-op. Returns the unlocked AchievementEntity or null if already
     * unlocked / not found.
     *
     * <p>⚠️ Synchronous — must only be called from within an executor.execute() block.
     */
    public AchievementEntity unlockAchievement(String userId, String achievementId, long nowMs) {
        UserAchievementEntity ua = userAchievementDao.get(userId, achievementId);
        if (ua == null || ua.isUnlocked()) return null;

        AchievementEntity ach = achievementDao.getById(achievementId);
        if (ach == null) return null;

        userAchievementDao.markUnlocked(userId, achievementId, nowMs);
        addXpInternal(userId, ach.getXpReward(), nowMs);

        if (ach.getInkReward() > 0) {
            userCurrencyDao.addInk(userId, ach.getInkReward());
            UserCurrencyEntity after = userCurrencyDao.get(userId);
            recordTransaction(
                    userId,
                    "EARN_ACHIEVEMENT",
                    ach.getInkReward(),
                    0,
                    after != null ? after.getInkBalance() : 0,
                    after != null ? after.getQuillBalance() : 0,
                    "Unlocked " + ach.getTitle(),
                    nowMs);
        }
        if (ach.getQuillReward() > 0) {
            userCurrencyDao.addQuill(userId, ach.getQuillReward());
        }

        userAchievementDao.markRewardClaimed(userId, achievementId);

        UserAchievementEntity updated = userAchievementDao.get(userId, achievementId);
        if (updated != null) {
            updated.setNeedsSync(true);
            userAchievementDao.update(updated);
        }

        syncAchievementsToServer(userId);
        return ach;
    }

    /**
     * Dispatches an achievement check to the executor so callers (e.g. ReaderActivity scroll
     * listeners) never touch Room on the main thread.
     *
     * <p>Use this instead of calling AchievementEngine directly from UI callbacks.
     */
    public void executeAchievementCheck(Runnable engineCall) {
        executor.execute(engineCall);
    }

    public void updateAchievementProgress(String userId, String achievementId, int newProgress) {
        executor.execute(
                () -> userAchievementDao.updateProgress(userId, achievementId, newProgress));
    }

    // ── XP ────────────────────────────────────────────────────────────────────

    public void addXp(String userId, int xp, long nowMs) {
        executor.execute(() -> addXpInternal(userId, xp, nowMs));
    }

    private void addXpInternal(String userId, int xp, long nowMs) {
        UserLevelEntity current = userLevelDao.get(userId);
        if (current == null) return;
        XpLevelEngine.LevelUpResult result = XpLevelEngine.addXp(current, xp, nowMs);
        if (result != null) {
            userLevelDao.updateProgress(
                    userId,
                    result.updated.getXpTotal(),
                    result.updated.getCurrentLevel(),
                    result.updated.getXpInLevel(),
                    result.updated.getXpLevelTarget(),
                    result.updated.getBadgeSlots());
            if (result.didLevelUp && levelUpListener != null) {
                levelUpListener.onLevelUp(result.newLevel, result.updated.getXpTotal());
            }
        }
    }

    // ── Level-up callback ─────────────────────────────────────────────────────

    public interface LevelUpListener {
        void onLevelUp(int newLevel, long xpTotal);
    }

    private LevelUpListener levelUpListener;

    public void setLevelUpListener(LevelUpListener listener) {
        this.levelUpListener = listener;
    }

    // ── Streak ────────────────────────────────────────────────────────────────

    public StreakEngine.StreakResult recordReadingActivity(String userId, long nowMs) {
        // Called from executor context (via executeAchievementCheck) — safe
        UserStreakEntity current = userStreakDao.get(userId);
        StreakEngine.StreakResult result = StreakEngine.onActivity(current, nowMs);
        UserStreakEntity updated = result.updated;
        if (current == null) {
            updated.setUserId(userId);
            userStreakDao.insert(updated);
        } else {
            userStreakDao.updateStreak(
                    userId,
                    updated.getCurrentStreak(),
                    updated.getLongestStreak(),
                    updated.getLastActivityDate());
        }
        if (result.event != StreakEngine.StreakEvent.ALREADY_TODAY) {
            // addInk dispatches to executor itself, but we're already on it —
            // call the internal variant directly to avoid re-queuing.
            int bonus = 10 + Math.min(updated.getCurrentStreak(), 50);
            userCurrencyDao.addInk(userId, bonus);
            UserCurrencyEntity after = userCurrencyDao.get(userId);
            recordTransaction(
                    userId,
                    "EARN_STREAK",
                    bonus,
                    0,
                    after != null ? after.getInkBalance() : 0,
                    after != null ? after.getQuillBalance() : 0,
                    "Daily streak bonus",
                    nowMs);
        }
        // ── Record a broken-streak run to the server history table ────────────
        if (result.event == StreakEngine.StreakEvent.BROKEN && result.oldStreak > 1) {
            // Approximate start: (oldStreak - 1) days before last activity date
            long approxStartMs = current.getLastActivityDate()
                    - (long)(result.oldStreak - 1) * TimeUnit.DAYS.toMillis(1);
            recordStreakBreak(userId, result.oldStreak, approxStartMs, nowMs);
        }
        syncStreakToServer(userId);
        return result;
    }

    /**
     * Applies a streak freeze asynchronously. The callback is invoked on the executor thread — post
     * to main thread in the ViewModel if you need to update UI (e.g. via Handler.post or
     * LiveData.postValue).
     */
    public void applyStreakFreeze(
            String userId, boolean useFreeFreeze, long nowMs, ResultCallback callback) {
        executor.execute(
                () -> {
                    UserStreakEntity current = userStreakDao.get(userId);
                    if (current == null) {
                        if (callback != null) callback.onResult(false);
                        return;
                    }
                    UserStreakEntity updated =
                            StreakEngine.applyFreeze(current, useFreeFreeze, nowMs);
                    if (updated == null) {
                        if (callback != null) callback.onResult(false);
                        return;
                    }
                    userStreakDao.applyFreeze(
                            userId,
                            updated.getFreezeExpiresAt(),
                            updated.getFreeFreezesUsedThisMonth());
                    if (!useFreeFreeze) {
                        spendInkInternal(
                                userId,
                                StreakEngine.FREEZE_COST_INK,
                                "SPEND_FREEZE",
                                "Streak freeze",
                                nowMs);
                    }
                    syncStreakToServer(userId);
                    if (callback != null) callback.onResult(true);
                });
    }

    /**
     * Applies a shield recovery asynchronously. The callback is invoked on the executor thread —
     * post to main thread in the ViewModel if you need to update UI.
     */
    public void applyShieldRecovery(
            String userId,
            int recoveredStreak,
            boolean isFreeRecovery,
            long nowMs,
            ResultCallback callback) {
        executor.execute(
                () -> {
                    UserStreakEntity current = userStreakDao.get(userId);
                    if (current == null) {
                        if (callback != null) callback.onResult(false);
                        return;
                    }
                    if (!isFreeRecovery) {
                        UserCurrencyEntity cur = userCurrencyDao.get(userId);
                        if (cur == null || cur.getQuillBalance() < StreakEngine.SHIELD_COST_QUILL) {
                            if (callback != null) callback.onResult(false);
                            return;
                        }
                        spendQuillInternal(
                                userId,
                                StreakEngine.SHIELD_COST_QUILL,
                                "SPEND_SHIELD",
                                "Streak shield recovery",
                                nowMs);
                    }
                    UserStreakEntity updated =
                            StreakEngine.applyShieldRecovery(current, recoveredStreak, nowMs);
                    userStreakDao.updateStreak(
                            userId,
                            updated.getCurrentStreak(),
                            updated.getLongestStreak(),
                            updated.getLastActivityDate());
                    syncStreakToServer(userId);
                    if (callback != null) callback.onResult(true);
                });
    }

    // ── Streak history & ad rewards ───────────────────────────────────────────

    /**
     * Calls the {@code get_streak_history} RPC and delivers parsed results to {@code callback}.
     * Delivered on the OkHttp callback thread — post to main if updating UI.
     */
    public void fetchStreakHistory(String userId, int limit, HistoryCallback callback) {
        String token = userPreferences.getAccessToken();
        if (token == null) {
            if (callback != null) callback.onResult(new ArrayList<>());
            return;
        }
        com.google.gson.JsonObject params = new com.google.gson.JsonObject();
        params.addProperty("p_user_id", userId);
        params.addProperty("p_limit", limit);
        supabase.callRpc("get_streak_history", params, token,
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override public void onSuccess(String result) {
                        List<StreakHistoryItem> items = parseStreakHistory(result);
                        if (callback != null) callback.onResult(items);
                    }
                    @Override public void onError(String error) {
                        Log.e(TAG, "fetchStreakHistory error: " + error);
                        if (callback != null) callback.onResult(new ArrayList<>());
                    }
                });
    }

    /**
     * Fires the {@code record_streak_break} RPC asynchronously.
     * Called inside {@link #recordReadingActivity} when StreakEvent.BROKEN is emitted, so the
     * server keeps a permanent history of every broken run.
     */
    public void recordStreakBreak(String userId, int streakLength,
                                  long startedAtMs, long endedAtMs) {
        String token = userPreferences.getAccessToken();
        if (token == null || streakLength <= 0) return;
        com.google.gson.JsonObject params = new com.google.gson.JsonObject();
        params.addProperty("p_user_id",      userId);
        params.addProperty("p_streak_length", streakLength);
        params.addProperty("p_started_ms",   startedAtMs);
        params.addProperty("p_ended_ms",     endedAtMs);
        supabase.callRpc("record_streak_break", params, token,
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override public void onSuccess(String r) {
                        Log.d(TAG, "Streak break recorded — length=" + streakLength);
                    }
                    @Override public void onError(String err) {
                        Log.e(TAG, "record_streak_break error: " + err);
                    }
                });
    }

    /**
     * Calls the {@code ad_earn_ink} RPC. On success the server credits +25 Ink and logs the ad
     * watch. Delivers result on the OkHttp thread — ViewModel posts to LiveData.
     */
    public void adEarnInk(String userId, AdRewardCallback callback) {
        String token = userPreferences.getAccessToken();
        if (token == null) {
            if (callback != null) callback.onResult(false, 0, "no_token");
            return;
        }
        com.google.gson.JsonObject params = new com.google.gson.JsonObject();
        params.addProperty("p_user_id", userId);
        supabase.callRpc("ad_earn_ink", params, token,
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override public void onSuccess(String result) {
                        try {
                            com.google.gson.JsonObject obj =
                                    JsonParser.parseString(result).getAsJsonObject();
                            boolean success = obj.get("success").getAsBoolean();
                            int ink = success ? obj.get("ink_awarded").getAsInt() : 0;
                            String reason = (!success && obj.has("reason"))
                                    ? obj.get("reason").getAsString() : null;
                            if (callback != null) callback.onResult(success, ink, reason);
                        } catch (Exception e) {
                            Log.e(TAG, "adEarnInk parse error", e);
                            if (callback != null) callback.onResult(false, 0, "parse_error");
                        }
                    }
                    @Override public void onError(String error) {
                        Log.e(TAG, "adEarnInk error: " + error);
                        if (callback != null) callback.onResult(false, 0, "network_error");
                    }
                });
    }

    /**
     * Calls the {@code ad_restore_freeze} RPC and updates local Room with the new
     * freeze_expires_at timestamp so the streak LiveData refreshes immediately.
     */
    public void adRestoreFreeze(String userId, ResultCallback callback) {
        String token = userPreferences.getAccessToken();
        if (token == null) {
            if (callback != null) callback.onResult(false);
            return;
        }
        com.google.gson.JsonObject params = new com.google.gson.JsonObject();
        params.addProperty("p_user_id", userId);
        supabase.callRpc("ad_restore_freeze", params, token,
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override public void onSuccess(String result) {
                        try {
                            com.google.gson.JsonObject obj =
                                    JsonParser.parseString(result).getAsJsonObject();
                            boolean success = obj.get("success").getAsBoolean();
                            if (success && obj.has("freeze_expires_ms")) {
                                long expiresMs = obj.get("freeze_expires_ms").getAsLong();
                                // Update Room so the LiveData refreshes without waiting for sync
                                executor.execute(() ->
                                        userStreakDao.applyFreeze(userId, expiresMs, 0));
                            }
                            if (callback != null) callback.onResult(success);
                        } catch (Exception e) {
                            Log.e(TAG, "adRestoreFreeze parse error", e);
                            if (callback != null) callback.onResult(false);
                        }
                    }
                    @Override public void onError(String error) {
                        Log.e(TAG, "adRestoreFreeze error: " + error);
                        if (callback != null) callback.onResult(false);
                    }
                });
    }

    /**
     * Calls the {@code ad_recover_streak} RPC and — on success — patches local Room
     * so the hero card updates immediately without waiting for the next sync cycle.
     */
    public void adRecoverStreak(String userId, int recoverTo, ResultCallback callback) {
        String token = userPreferences.getAccessToken();
        if (token == null) {
            if (callback != null) callback.onResult(false);
            return;
        }
        com.google.gson.JsonObject params = new com.google.gson.JsonObject();
        params.addProperty("p_user_id",    userId);
        params.addProperty("p_recover_to", recoverTo);
        supabase.callRpc("ad_recover_streak", params, token,
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override public void onSuccess(String result) {
                        try {
                            com.google.gson.JsonObject obj =
                                    JsonParser.parseString(result).getAsJsonObject();
                            boolean success = obj.get("success").getAsBoolean();
                            if (success) {
                                long nowMs = System.currentTimeMillis();
                                executor.execute(() -> {
                                    UserStreakEntity cur = userStreakDao.get(userId);
                                    if (cur != null) {
                                        userStreakDao.updateStreak(
                                                userId,
                                                recoverTo,
                                                Math.max(cur.getLongestStreak(), recoverTo),
                                                nowMs);
                                    }
                                });
                            }
                            if (callback != null) callback.onResult(success);
                        } catch (Exception e) {
                            Log.e(TAG, "adRecoverStreak parse error", e);
                            if (callback != null) callback.onResult(false);
                        }
                    }
                    @Override public void onError(String error) {
                        Log.e(TAG, "adRecoverStreak error: " + error);
                        if (callback != null) callback.onResult(false);
                    }
                });
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<StreakHistoryItem> parseStreakHistory(String json) {
        List<StreakHistoryItem> list = new ArrayList<>();
        try {
            JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
            for (JsonElement el : arr) {
                com.google.gson.JsonObject o = el.getAsJsonObject();
                String id     = o.has("id")     ? o.get("id").getAsString()     : UUID.randomUUID().toString();
                int    length = o.has("streak_length") ? o.get("streak_length").getAsInt() : 1;
                long   start  = parseIsoToMs(o.has("started_at") && !o.get("started_at").isJsonNull()
                        ? o.get("started_at").getAsString() : null);
                long   end    = parseIsoToMs(o.has("ended_at") && !o.get("ended_at").isJsonNull()
                        ? o.get("ended_at").getAsString() : null);
                list.add(new StreakHistoryItem(id, length, start, end));
            }
        } catch (Exception e) {
            Log.e(TAG, "parseStreakHistory error", e);
        }
        return list;
    }

    private static long parseIsoToMs(String iso) {
        if (iso == null || iso.isEmpty()) return 0L;
        try {
            // Supabase returns ISO-8601: "2026-04-10T14:23:00+00:00"
            java.text.SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US);
            return sdf.parse(iso).getTime();
        } catch (Exception e) {
            try {
                // Fallback without offset
                java.text.SimpleDateFormat sdf2 =
                        new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                return sdf2.parse(iso).getTime();
            } catch (Exception ignored) {}
        }
        return 0L;
    }



    /**
     * Returns true if an achievement can be unlocked right now (≤10/min limit).
     *
     * <p>⚠️ Synchronous — must only be called from within an executor.execute() block. Use
     * executeAchievementCheck() to dispatch from UI threads.
     */
    public boolean checkAchievementRateLimit(String userId, long nowMs) {
        String dateKey = DATE_FMT.format(new Date(nowMs));
        ensureDailyCapRow(userId, dateKey);
        DailyCapEntity cap = dailyCapDao.get(userId, dateKey);
        if (cap == null) return true;

        long minuteWindowStart = cap.getLastAchievementMinuteTs();
        int countInWindow = cap.getAchievementsInMinute();

        if (nowMs - minuteWindowStart > 60_000L) {
            dailyCapDao.updateMinuteWindow(userId, dateKey, nowMs, 1);
            return true;
        }
        if (countInWindow >= 10) return false;
        dailyCapDao.updateMinuteWindow(userId, dateKey, minuteWindowStart, countInWindow + 1);
        return true;
    }

    // ── Supabase background sync (fire-and-forget) ────────────────────────────

    /**
     * Sync currency balances to Supabase user_currency table. Uses UPSERT on user_id conflict so
     * it's safe to call repeatedly.
     */
    private void syncCurrencyToServer(String userId) {
        UserCurrencyEntity e = userCurrencyDao.get(userId);
        if (e == null || !e.isNeedsSync()) return;
        String token = userPreferences.getAccessToken();
        if (token == null) return;

        com.google.gson.JsonObject body = new com.google.gson.JsonObject();
        body.addProperty("user_id", userId);
        body.addProperty("ink_balance", e.getInkBalance());
        body.addProperty("quill_balance", e.getQuillBalance());
        body.addProperty("lifetime_ink_earned", e.getLifetimeInkEarned());
        body.addProperty("lifetime_quill_spent", e.getLifetimeQuillSpent());

        supabase.upsert(
                "user_currency",
                body,
                "user_id",
                token,
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override
                    public void onSuccess(String r) {
                        executor.execute(() -> userCurrencyDao.markSynced(userId));
                        Log.d(TAG, "Currency synced for " + userId);
                    }

                    @Override
                    public void onError(String err) {
                        Log.e(TAG, "Currency sync failed: " + err);
                    }
                });
    }

    /** Sync unlocked achievements to Supabase user_achievements table. */
    private void syncAchievementsToServer(String userId) {
        List<UserAchievementEntity> pending = userAchievementDao.getPendingSync();
        if (pending == null || pending.isEmpty()) return;
        String token = userPreferences.getAccessToken();
        if (token == null) return;

        for (UserAchievementEntity ua : pending) {
            com.google.gson.JsonObject body = new com.google.gson.JsonObject();
            body.addProperty("user_id", ua.getUserId());
            body.addProperty("achievement_id", ua.getAchievementId());
            body.addProperty("is_unlocked", ua.isUnlocked());
            body.addProperty("unlocked_at", ua.getUnlockedAt());
            body.addProperty("progress", ua.getProgress());

            final String localId = ua.getId();
            supabase.upsert(
                    "user_achievements",
                    body,
                    "user_id,achievement_id",
                    token,
                    new SupabaseDatabaseService.DatabaseCallback() {
                        @Override
                        public void onSuccess(String r) {
                            executor.execute(() -> userAchievementDao.markSynced(localId));
                        }

                        @Override
                        public void onError(String err) {
                            Log.e(TAG, "Achievement sync failed: " + err);
                        }
                    });
        }
    }

    /**
     * Sync streak state to Supabase via the sync_user_streak RPC. The RPC accepts epoch-millisecond
     * longs and converts to timestamptz server-side.
     */
    private void syncStreakToServer(String userId) {
        List<UserStreakEntity> pending = userStreakDao.getPendingSync();
        if (pending == null || pending.isEmpty()) return;
        String token = userPreferences.getAccessToken();
        if (token == null) return;

        for (UserStreakEntity e : pending) {
            com.google.gson.JsonObject params = new com.google.gson.JsonObject();
            params.addProperty("p_user_id", e.getUserId());
            params.addProperty("p_current_streak", e.getCurrentStreak());
            params.addProperty("p_longest_streak", e.getLongestStreak());
            params.addProperty("p_last_activity_date", e.getLastActivityDate());
            params.addProperty("p_grace_window_start", e.getGraceWindowStart());
            params.addProperty("p_freeze_expires_at", e.getFreezeExpiresAt());

            supabase.callRpc(
                    "sync_user_streak",
                    params,
                    token,
                    new SupabaseDatabaseService.DatabaseCallback() {
                        @Override
                        public void onSuccess(String r) {
                            executor.execute(() -> userStreakDao.markSynced(e.getUserId()));
                            Log.d(TAG, "Streak synced for " + e.getUserId());
                        }

                        @Override
                        public void onError(String err) {
                            Log.e(TAG, "Streak sync failed: " + err);
                        }
                    });
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void recordTransaction(
            String userId,
            String type,
            int inkDelta,
            int quillDelta,
            int inkAfter,
            int quillAfter,
            String reason,
            long nowMs) {
        TokenTransactionEntity tx = new TokenTransactionEntity();
        tx.setId(UUID.randomUUID().toString());
        tx.setUserId(userId);
        tx.setType(type);
        tx.setInkDelta(inkDelta);
        tx.setQuillDelta(quillDelta);
        tx.setInkAfter(inkAfter);
        tx.setQuillAfter(quillAfter);
        tx.setReason(reason);
        tx.setCreatedAt(nowMs);
        tx.setNeedsSync(true);
        tokenTransactionDao.insert(tx);
    }

    /** Must only be called from within an executor.execute() block. */
    private void ensureDailyCapRow(String userId, String dateKey) {
        DailyCapEntity existing = dailyCapDao.get(userId, dateKey);
        if (existing == null) {
            DailyCapEntity cap = new DailyCapEntity();
            cap.setId(userId + "_" + dateKey);
            cap.setUserId(userId);
            cap.setDateKey(dateKey);
            dailyCapDao.insert(cap);
        }
    }
}
