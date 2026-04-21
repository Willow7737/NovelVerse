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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Gamification repository — local-first, optimistic updates, server-wins conflict resolution.
 *
 * Threading contract:
 *   • All methods that touch Room must run on the executor (never on the main thread).
 *   • Methods returning void dispatch themselves via executor internally.
 *   • Methods that must return a result use a ResultCallback so callers stay off-thread.
 *   • checkAchievementRateLimit / unlockAchievement are synchronous helpers — they must
 *     only ever be called from within an executor.execute() block (e.g. from AchievementEngine
 *     which is always dispatched by ReaderActivity via executeAchievementCheck()).
 */
@Singleton
public class GamificationRepository {

    private static final String TAG = "GamificationRepo";
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private final AchievementDao      achievementDao;
    private final UserAchievementDao  userAchievementDao;
    private final UserCurrencyDao     userCurrencyDao;
    private final TokenTransactionDao tokenTransactionDao;
    private final UserLevelDao        userLevelDao;
    private final UserStreakDao       userStreakDao;
    private final DailyCapDao         dailyCapDao;
    private final SupabaseDatabaseService supabase;
    final ExecutorService             executor; // package-private so ViewModel can submit tasks

    // ── Callback interfaces ───────────────────────────────────────────────────

    /** Generic boolean result callback — delivered on the executor thread. */
    public interface ResultCallback {
        /** @param success the boolean result of the operation */
        void onResult(boolean success);
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
            SupabaseDatabaseService supabase) {
        this.achievementDao      = achievementDao;
        this.userAchievementDao  = userAchievementDao;
        this.userCurrencyDao     = userCurrencyDao;
        this.tokenTransactionDao = tokenTransactionDao;
        this.userLevelDao        = userLevelDao;
        this.userStreakDao        = userStreakDao;
        this.dailyCapDao         = dailyCapDao;
        this.supabase            = supabase;
        this.executor            = Executors.newSingleThreadExecutor();
    }

    // ── Seeding ───────────────────────────────────────────────────────────────

    /** Call once at app start (idempotent — INSERT OR IGNORE). */
    public void seedAchievementsIfNeeded() {
        executor.execute(() -> {
            if (achievementDao.count() == 0) {
                achievementDao.insertAll(AchievementDefinitions.all());
                Log.i(TAG, "Seeded " + achievementDao.count() + " achievements");
            }
        });
    }

    /** Ensures gamification rows exist for a user (idempotent). */
    public void ensureUserRows(String userId, long nowMs) {
        executor.execute(() -> {
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
        executor.execute(() -> {
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
            recordTransaction(userId, type, amount, 0,
                after != null ? after.getInkBalance() : 0,
                after != null ? after.getQuillBalance() : 0,
                reason, nowMs);
            syncCurrencyToServer(userId);
        });
    }

    public void spendInk(String userId, int amount, String type, String reason, long nowMs) {
        executor.execute(() -> spendInkInternal(userId, amount, type, reason, nowMs));
    }

    /** Synchronous variant — only call from within an executor.execute() block. */
    private void spendInkInternal(String userId, int amount, String type, String reason, long nowMs) {
        UserCurrencyEntity cur = userCurrencyDao.get(userId);
        if (cur == null || cur.getInkBalance() < amount) {
            Log.w(TAG, "Insufficient Ink for " + userId);
            return;
        }
        userCurrencyDao.addInk(userId, -amount);
        UserCurrencyEntity after = userCurrencyDao.get(userId);
        recordTransaction(userId, type, -amount, 0,
            after != null ? after.getInkBalance() : 0,
            after != null ? after.getQuillBalance() : 0,
            reason, nowMs);
        syncCurrencyToServer(userId);
    }

    public void spendQuill(String userId, int amount, String type, String reason, long nowMs) {
        executor.execute(() -> spendQuillInternal(userId, amount, type, reason, nowMs));
    }

    /** Synchronous variant — only call from within an executor.execute() block. */
    private void spendQuillInternal(String userId, int amount, String type, String reason, long nowMs) {
        UserCurrencyEntity cur = userCurrencyDao.get(userId);
        if (cur == null || cur.getQuillBalance() < amount) {
            Log.w(TAG, "Insufficient Quill for " + userId);
            return;
        }
        userCurrencyDao.addQuill(userId, -amount);
        UserCurrencyEntity after = userCurrencyDao.get(userId);
        recordTransaction(userId, type, 0, -amount,
            after != null ? after.getInkBalance() : 0,
            after != null ? after.getQuillBalance() : 0,
            reason, nowMs);
        syncCurrencyToServer(userId);
    }

    // ── Achievement unlock ────────────────────────────────────────────────────

    /**
     * Mark an achievement as unlocked and credit its rewards.
     * Idempotent — safe to call multiple times, second call is a no-op.
     * Returns the unlocked AchievementEntity or null if already unlocked / not found.
     *
     * ⚠️ Synchronous — must only be called from within an executor.execute() block.
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
            recordTransaction(userId, "EARN_ACHIEVEMENT", ach.getInkReward(), 0,
                after != null ? after.getInkBalance() : 0,
                after != null ? after.getQuillBalance() : 0,
                "Unlocked " + ach.getTitle(), nowMs);
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
     * Dispatches an achievement check to the executor so callers (e.g. ReaderActivity
     * scroll listeners) never touch Room on the main thread.
     *
     * Use this instead of calling AchievementEngine directly from UI callbacks.
     */
    public void executeAchievementCheck(Runnable engineCall) {
        executor.execute(engineCall);
    }

    public void updateAchievementProgress(String userId, String achievementId, int newProgress) {
        executor.execute(() ->
            userAchievementDao.updateProgress(userId, achievementId, newProgress));
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
            userLevelDao.updateProgress(userId,
                result.updated.getXpTotal(),
                result.updated.getCurrentLevel(),
                result.updated.getXpInLevel(),
                result.updated.getXpLevelTarget(),
                result.updated.getBadgeSlots());
            if (result.didLevelUp && levelUpListener != null) {
                levelUpListener.onLevelUp(result.newLevel);
            }
        }
    }

    // ── Level-up callback ─────────────────────────────────────────────────────

    public interface LevelUpListener {
        void onLevelUp(int newLevel);
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
            userStreakDao.updateStreak(userId,
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
            recordTransaction(userId, "EARN_STREAK", bonus, 0,
                after != null ? after.getInkBalance() : 0,
                after != null ? after.getQuillBalance() : 0,
                "Daily streak bonus", nowMs);
        }
        syncStreakToServer(userId);
        return result;
    }

    /**
     * Applies a streak freeze asynchronously.
     * The callback is invoked on the executor thread — post to main thread in the
     * ViewModel if you need to update UI (e.g. via Handler.post or LiveData.postValue).
     */
    public void applyStreakFreeze(String userId, boolean useFreeFreeze, long nowMs,
                                   ResultCallback callback) {
        executor.execute(() -> {
            UserStreakEntity current = userStreakDao.get(userId);
            if (current == null) {
                if (callback != null) callback.onResult(false);
                return;
            }
            UserStreakEntity updated = StreakEngine.applyFreeze(current, useFreeFreeze, nowMs);
            if (updated == null) {
                if (callback != null) callback.onResult(false);
                return;
            }
            userStreakDao.applyFreeze(userId,
                updated.getFreezeExpiresAt(),
                updated.getFreeFreezesUsedThisMonth());
            if (!useFreeFreeze) {
                spendInkInternal(userId, StreakEngine.FREEZE_COST_INK,
                    "SPEND_FREEZE", "Streak freeze", nowMs);
            }
            syncStreakToServer(userId);
            if (callback != null) callback.onResult(true);
        });
    }

    /**
     * Applies a shield recovery asynchronously.
     * The callback is invoked on the executor thread — post to main thread in the
     * ViewModel if you need to update UI.
     */
    public void applyShieldRecovery(String userId, int recoveredStreak,
                                     boolean isFreeRecovery, long nowMs,
                                     ResultCallback callback) {
        executor.execute(() -> {
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
                spendQuillInternal(userId, StreakEngine.SHIELD_COST_QUILL,
                    "SPEND_SHIELD", "Streak shield recovery", nowMs);
            }
            UserStreakEntity updated =
                StreakEngine.applyShieldRecovery(current, recoveredStreak, nowMs);
            userStreakDao.updateStreak(userId,
                updated.getCurrentStreak(),
                updated.getLongestStreak(),
                updated.getLastActivityDate());
            syncStreakToServer(userId);
            if (callback != null) callback.onResult(true);
        });
    }

    // ── Anti-exploit: rate limit ──────────────────────────────────────────────

    /**
     * Returns true if an achievement can be unlocked right now (≤10/min limit).
     *
     * ⚠️ Synchronous — must only be called from within an executor.execute() block.
     * Use executeAchievementCheck() to dispatch from UI threads.
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

    private void syncCurrencyToServer(String userId) {
        UserCurrencyEntity e = userCurrencyDao.get(userId);
        if (e == null || !e.isNeedsSync()) return;
        Log.d(TAG, "Sync currency for " + userId + " (stub — wire token)");
    }

    private void syncAchievementsToServer(String userId) {
        List<UserAchievementEntity> pending = userAchievementDao.getPendingSync();
        for (UserAchievementEntity ua : pending) {
            Log.d(TAG, "Sync achievement " + ua.getAchievementId() + " for " + ua.getUserId());
            userAchievementDao.markSynced(ua.getId());
        }
    }

    private void syncStreakToServer(String userId) {
        List<UserStreakEntity> pending = userStreakDao.getPendingSync();
        for (UserStreakEntity e : pending) {
            Log.d(TAG, "Sync streak for " + e.getUserId());
            userStreakDao.markSynced(e.getUserId());
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void recordTransaction(String userId, String type, int inkDelta, int quillDelta,
                                    int inkAfter, int quillAfter, String reason, long nowMs) {
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
