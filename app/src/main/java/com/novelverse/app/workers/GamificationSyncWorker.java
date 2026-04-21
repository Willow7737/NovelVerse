package com.novelverse.app.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.JsonObject;
import com.novelverse.app.data.local.dao.DailyCapDao;
import com.novelverse.app.data.local.dao.TokenTransactionDao;
import com.novelverse.app.data.local.dao.UserAchievementDao;
import com.novelverse.app.data.local.dao.UserCurrencyDao;
import com.novelverse.app.data.local.dao.UserLevelDao;
import com.novelverse.app.data.local.dao.UserStreakDao;
import com.novelverse.app.data.local.entities.TokenTransactionEntity;
import com.novelverse.app.data.local.entities.UserAchievementEntity;
import com.novelverse.app.data.local.entities.UserCurrencyEntity;
import com.novelverse.app.data.local.entities.UserLevelEntity;
import com.novelverse.app.data.local.entities.UserStreakEntity;
import com.novelverse.app.data.local.preferences.UserPreferences;
import com.novelverse.app.data.remote.supabase.SupabaseDatabaseService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;

/**
 * Periodically syncs all gamification data to Supabase.
 * Runs every 15 minutes when the device has network (via WorkManager).
 * Uses server-wins conflict resolution: if server version > local, overwrites local.
 *
 * Tables synced:
 *   user_achievements → upsert unlocked achievements
 *   user_currency     → upsert with version check (server wins)
 *   token_transactions → insert-only (ledger is immutable)
 *   user_levels       → upsert
 *   user_streaks      → upsert
 *   daily_caps        → pruned locally after 7 days
 */
@HiltWorker
public class GamificationSyncWorker extends Worker {

    private static final String TAG = "GamSyncWorker";
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private final UserAchievementDao  userAchievementDao;
    private final UserCurrencyDao     userCurrencyDao;
    private final TokenTransactionDao tokenTransactionDao;
    private final UserLevelDao        userLevelDao;
    private final UserStreakDao       userStreakDao;
    private final DailyCapDao         dailyCapDao;
    private final SupabaseDatabaseService supabase;
    private final UserPreferences     prefs;

    @AssistedInject
    public GamificationSyncWorker(
            @Assisted Context context,
            @Assisted WorkerParameters params,
            UserAchievementDao userAchievementDao,
            UserCurrencyDao userCurrencyDao,
            TokenTransactionDao tokenTransactionDao,
            UserLevelDao userLevelDao,
            UserStreakDao userStreakDao,
            DailyCapDao dailyCapDao,
            SupabaseDatabaseService supabase,
            UserPreferences prefs) {
        super(context, params);
        this.userAchievementDao  = userAchievementDao;
        this.userCurrencyDao     = userCurrencyDao;
        this.tokenTransactionDao = tokenTransactionDao;
        this.userLevelDao        = userLevelDao;
        this.userStreakDao        = userStreakDao;
        this.dailyCapDao         = dailyCapDao;
        this.supabase            = supabase;
        this.prefs               = prefs;
    }

    @NonNull
    @Override
    public Result doWork() {
        String userId = prefs.getUserId();
        String token  = prefs.getAccessToken();
        if (userId == null || token == null) {
            Log.d(TAG, "No authenticated user — skipping gamification sync");
            return Result.success();
        }

        Log.d(TAG, "Starting gamification sync for " + userId);
        boolean anyFailure = false;

        anyFailure |= !syncAchievements(userId, token);
        anyFailure |= !syncCurrency(userId, token);
        anyFailure |= !syncTransactions(userId, token);
        anyFailure |= !syncLevel(userId, token);
        anyFailure |= !syncStreak(userId, token);

        // Prune old daily_caps (keep last 7 days)
        String sevenDaysAgo = DATE_FMT.format(new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)));
        dailyCapDao.pruneOlderThan(sevenDaysAgo);

        if (anyFailure) {
            Log.w(TAG, "Some gamification sync tasks failed — will retry");
            return Result.retry();
        }
        Log.d(TAG, "Gamification sync complete");
        return Result.success();
    }

    // ── Sync methods ──────────────────────────────────────────────────────────

    private boolean syncAchievements(String userId, String token) {
        List<UserAchievementEntity> pending = userAchievementDao.getPendingSync();
        if (pending.isEmpty()) return true;
        boolean ok = true;
        for (UserAchievementEntity ua : pending) {
            JsonObject body = new JsonObject();
            body.addProperty("user_id",          ua.getUserId());
            body.addProperty("achievement_id",   ua.getAchievementId());
            body.addProperty("is_unlocked",      ua.isUnlocked());
            body.addProperty("current_progress", ua.getCurrentProgress());
            body.addProperty("reward_claimed",   ua.isRewardClaimed());
            if (ua.getUnlockedAt() > 0) {
                body.addProperty("unlocked_at",  new java.util.Date(ua.getUnlockedAt()).toString());
            }

            AtomicBoolean success = new AtomicBoolean(false);
            CountDownLatch latch = new CountDownLatch(1);
            supabase.upsert("user_achievements", body, "user_id,achievement_id", token,
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override public void onSuccess(String r) { success.set(true); latch.countDown(); }
                    @Override public void onError(String e)   { Log.w(TAG, "achievement sync: " + e); latch.countDown(); }
                });
            awaitLatch(latch);
            if (success.get()) {
                userAchievementDao.markSynced(ua.getId());
            } else {
                ok = false;
            }
        }
        return ok;
    }

    private boolean syncCurrency(String userId, String token) {
        UserCurrencyEntity cur = userCurrencyDao.get(userId);
        if (cur == null || !cur.isNeedsSync()) return true;

        JsonObject body = new JsonObject();
        body.addProperty("user_id",       userId);
        body.addProperty("ink_balance",   cur.getInkBalance());
        body.addProperty("quill_balance", cur.getQuillBalance());
        body.addProperty("version",       cur.getVersion());

        AtomicBoolean success = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);
        supabase.upsert("user_currency", body, "user_id", token,
            new SupabaseDatabaseService.DatabaseCallback() {
                @Override public void onSuccess(String r) {
                    // Server-wins: parse returned balances and overwrite local if server version higher
                    try {
                        JsonObject resp = new com.google.gson.Gson().fromJson(r, JsonObject.class);
                        if (resp != null && resp.has("version")) {
                            long serverVersion = resp.get("version").getAsLong();
                            if (serverVersion >= cur.getVersion()) {
                                int serverInk   = resp.has("ink_balance")   ? resp.get("ink_balance").getAsInt()   : cur.getInkBalance();
                                int serverQuill = resp.has("quill_balance") ? resp.get("quill_balance").getAsInt() : cur.getQuillBalance();
                                userCurrencyDao.applyServerState(userId, serverInk, serverQuill, serverVersion, System.currentTimeMillis());
                            }
                        }
                    } catch (Exception ignored) {}
                    success.set(true);
                    latch.countDown();
                }
                @Override public void onError(String e) { Log.w(TAG, "currency sync: " + e); latch.countDown(); }
            });
        awaitLatch(latch);
        return success.get();
    }

    private boolean syncTransactions(String userId, String token) {
        List<TokenTransactionEntity> pending = tokenTransactionDao.getPendingSync();
        if (pending.isEmpty()) return true;
        boolean ok = true;
        for (TokenTransactionEntity tx : pending) {
            JsonObject body = new JsonObject();
            body.addProperty("id",          tx.getId());
            body.addProperty("user_id",     tx.getUserId());
            body.addProperty("type",        tx.getType());
            body.addProperty("ink_delta",   tx.getInkDelta());
            body.addProperty("quill_delta", tx.getQuillDelta());
            body.addProperty("ink_after",   tx.getInkAfter());
            body.addProperty("quill_after", tx.getQuillAfter());
            if (tx.getReason() != null) body.addProperty("reason", tx.getReason());

            AtomicBoolean success = new AtomicBoolean(false);
            CountDownLatch latch = new CountDownLatch(1);
            supabase.upsert("token_transactions", body, "id", token,
                new SupabaseDatabaseService.DatabaseCallback() {
                    @Override public void onSuccess(String r) { success.set(true); latch.countDown(); }
                    @Override public void onError(String e)   { Log.w(TAG, "tx sync: " + e); latch.countDown(); }
                });
            awaitLatch(latch);
            if (success.get()) tokenTransactionDao.markSynced(tx.getId());
            else ok = false;
        }
        return ok;
    }

    private boolean syncLevel(String userId, String token) {
        UserLevelEntity lv = userLevelDao.get(userId);
        if (lv == null || !lv.isNeedsSync()) return true;

        JsonObject body = new JsonObject();
        body.addProperty("user_id",          userId);
        body.addProperty("xp_total",         lv.getXpTotal());
        body.addProperty("current_level",    lv.getCurrentLevel());
        body.addProperty("xp_in_level",      lv.getXpInLevel());
        body.addProperty("xp_level_target",  lv.getXpLevelTarget());
        body.addProperty("badge_slots",      lv.getBadgeSlots());
        if (lv.getEquippedBadgeIds() != null) body.addProperty("equipped_badge_ids", lv.getEquippedBadgeIds());

        AtomicBoolean success = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);
        supabase.upsert("user_levels", body, "user_id", token,
            new SupabaseDatabaseService.DatabaseCallback() {
                @Override public void onSuccess(String r) { success.set(true); latch.countDown(); }
                @Override public void onError(String e)   { Log.w(TAG, "level sync: " + e); latch.countDown(); }
            });
        awaitLatch(latch);
        if (success.get()) userLevelDao.markSynced(userId);
        return success.get();
    }

    private boolean syncStreak(String userId, String token) {
        UserStreakEntity streak = userStreakDao.get(userId);
        if (streak == null || !streak.isNeedsSync()) return true;

        JsonObject body = new JsonObject();
        body.addProperty("user_id",        userId);
        body.addProperty("current_streak", streak.getCurrentStreak());
        body.addProperty("longest_streak", streak.getLongestStreak());
        body.addProperty("freeze_expires_at",              streak.getFreezeExpiresAt());
        body.addProperty("free_freezes_used_this_month",   streak.getFreeFreezesUsedThisMonth());
        body.addProperty("grace_used_in_window",           streak.isGraceUsedInWindow());

        AtomicBoolean success = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);
        supabase.upsert("user_streaks", body, "user_id", token,
            new SupabaseDatabaseService.DatabaseCallback() {
                @Override public void onSuccess(String r) { success.set(true); latch.countDown(); }
                @Override public void onError(String e)   { Log.w(TAG, "streak sync: " + e); latch.countDown(); }
            });
        awaitLatch(latch);
        if (success.get()) userStreakDao.markSynced(userId);
        return success.get();
    }

    private void awaitLatch(CountDownLatch latch) {
        try { latch.await(15, TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
