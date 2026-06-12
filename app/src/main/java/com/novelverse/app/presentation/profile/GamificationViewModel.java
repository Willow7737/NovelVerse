package com.novelverse.app.presentation.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.novelverse.app.data.local.entities.AchievementEntity;
import com.novelverse.app.data.local.entities.TokenTransactionEntity;
import com.novelverse.app.data.local.entities.UserAchievementEntity;
import com.novelverse.app.data.local.entities.UserCurrencyEntity;
import com.novelverse.app.data.local.entities.UserLevelEntity;
import com.novelverse.app.data.local.entities.UserStreakEntity;
import com.novelverse.app.data.repository.GamificationRepository;
import com.novelverse.app.domain.gamification.StreakEngine;
import com.novelverse.app.utils.SingleLiveEvent;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel for all gamification UI.
 * Survives configuration changes; exposes LiveData streams from Room.
 *
 * <p>levelUpEvent fires exactly once per level-up via {@link SingleLiveEvent}.
 * The payload is an int[] {newLevel, xpTotal} so callers can pass both extras
 * to {@link com.novelverse.app.presentation.gamification.LevelUpActivity}.
 */
@HiltViewModel
public class GamificationViewModel extends ViewModel {

    private final GamificationRepository repo;

    // Stable LiveData wired once per userId
    private final MediatorLiveData<UserCurrencyEntity> currency = new MediatorLiveData<>();
    private final MediatorLiveData<UserLevelEntity>    level    = new MediatorLiveData<>();
    private final MediatorLiveData<UserStreakEntity>   streak   = new MediatorLiveData<>();
    private final MediatorLiveData<List<UserAchievementEntity>> allAchievements = new MediatorLiveData<>();
    private final MediatorLiveData<List<AchievementEntity>>    catalog          = new MediatorLiveData<>();
    private final MediatorLiveData<List<TokenTransactionEntity>> transactions   = new MediatorLiveData<>();

    private final MutableLiveData<StreakEngine.StreakResult> lastStreakResult = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();

    /**
     * Fires once per level-up with int[] {newLevel, (int) xpTotal}.
     * Observed only in HomeFragment — no other fragment should re-trigger it.
     *
     * Note: xpTotal is cast to int for the array. If a user ever exceeds
     * Integer.MAX_VALUE XP (~2.1 billion) this will overflow, but that
     * is not a real concern for the foreseeable future. If you want to be
     * safe, switch to a long[] or a small wrapper object.
     */
    private final SingleLiveEvent<int[]> levelUpEvent = new SingleLiveEvent<>();

    private String currentUserId;
    private LiveData<UserCurrencyEntity> currencySource;
    private LiveData<UserLevelEntity>    levelSource;
    private LiveData<UserStreakEntity>   streakSource;
    private LiveData<List<UserAchievementEntity>> achievementsSource;
    private LiveData<List<AchievementEntity>> catalogSource;
    private LiveData<List<TokenTransactionEntity>> txSource;

    @Inject
    public GamificationViewModel(GamificationRepository repo) {
        this.repo = repo;
        // Wire catalog once (no userId dependency)
        catalogSource = repo.getCatalogLive();
        catalog.addSource(catalogSource, catalog::setValue);

        // Wire level-up callback from repo → SingleLiveEvent
        repo.setLevelUpListener((newLevel, xpTotal) ->
                levelUpEvent.postValue(new int[]{newLevel, (int) xpTotal}));
    }

    /** Call after auth — wires all LiveData for the signed-in user. */
    public void init(String userId) {
        if (userId == null || userId.equals(currentUserId)) return;
        currentUserId = userId;

        // Seed achievements and ensure user rows
        repo.seedAchievementsIfNeeded();
        repo.ensureUserRows(userId, System.currentTimeMillis());

        // Swap sources
        if (currencySource != null)     currency.removeSource(currencySource);
        if (levelSource != null)        level.removeSource(levelSource);
        if (streakSource != null)       streak.removeSource(streakSource);
        if (achievementsSource != null) allAchievements.removeSource(achievementsSource);
        if (txSource != null)           transactions.removeSource(txSource);

        currencySource     = repo.getCurrencyLive(userId);
        levelSource        = repo.getLevelLive(userId);
        streakSource       = repo.getStreakLive(userId);
        achievementsSource = repo.getAllUserAchievementsLive(userId);
        txSource           = repo.getRecentTransactionsLive(userId);

        currency.addSource(currencySource,           currency::setValue);
        level.addSource(levelSource,                 level::setValue);
        streak.addSource(streakSource,               streak::setValue);
        allAchievements.addSource(achievementsSource, allAchievements::setValue);
        transactions.addSource(txSource,             transactions::setValue);
    }

    // ── Exposed LiveData ──────────────────────────────────────────────────────

    public LiveData<UserCurrencyEntity> getCurrency()       { return currency; }
    public LiveData<UserLevelEntity>    getLevel()          { return level; }
    public LiveData<UserStreakEntity>   getStreak()         { return streak; }
    public LiveData<List<UserAchievementEntity>> getAllAchievements() { return allAchievements; }
    public LiveData<List<AchievementEntity>>     getCatalog()        { return catalog; }
    public LiveData<List<TokenTransactionEntity>> getTransactions()  { return transactions; }
    public LiveData<StreakEngine.StreakResult>    getLastStreakResult() { return lastStreakResult; }
    public LiveData<String> getToastMessage()               { return toastMessage; }

    /**
     * One-shot event: fires when the user levels up.
     * Payload: int[] {newLevel, xpTotal (truncated to int)}.
     * Observe this only in HomeFragment.
     */
    public LiveData<int[]> getLevelUpEvent() { return levelUpEvent; }

    // ── Actions ───────────────────────────────────────────────────────────────

    /** Call from ReaderActivity on stop or scroll milestone. */
    public void onReadingActivity(int secondsRead, double progressPct, double prevPct,
                                  int totalNovelsRead, int totalChaptersRead) {
        if (currentUserId == null) return;
        long now = System.currentTimeMillis();
        // Streak update runs on repo's executor — result posted back via lastStreakResult
        repo.executeAchievementCheck(() -> {
            StreakEngine.StreakResult result = repo.recordReadingActivity(currentUserId, now);
            lastStreakResult.postValue(result);
        });
        // XP and Ink dispatch themselves to the executor internally
        repo.addXp(currentUserId, 10, now);
        if (secondsRead >= 30 && progressPct > prevPct) {
            int ink = Math.min(50, secondsRead / 30);
            repo.addInk(currentUserId, ink, "EARN_READING", "Reading session", now);
        }
    }

    /**
     * Applies a streak freeze off the main thread.
     * Result is posted to toastMessage LiveData — observe it in the Fragment.
     */
    public void applyFreeze(boolean useFreeFreeze) {
        if (currentUserId == null) return;
        repo.applyStreakFreeze(
                currentUserId,
                useFreeFreeze,
                System.currentTimeMillis(),
                success -> toastMessage.postValue(success ? "Streak frozen! ❄️" : "No free freezes available")
        );
    }

    /**
     * Applies a shield recovery off the main thread.
     * Result is posted to toastMessage LiveData — observe it in the Fragment.
     */
    public void applyShieldRecovery(int recoveredStreak, boolean isFree) {
        if (currentUserId == null) return;
        repo.applyShieldRecovery(
                currentUserId,
                recoveredStreak,
                isFree,
                System.currentTimeMillis(),
                success -> toastMessage.postValue(success ? "Streak restored! 🛡" : "Insufficient Quill")
        );
    }

    // ── Computed helpers ──────────────────────────────────────────────────────

    public int getUnlockedCount() {
        List<UserAchievementEntity> list = allAchievements.getValue();
        if (list == null) return 0;
        int count = 0;
        for (UserAchievementEntity ua : list) if (ua.isUnlocked()) count++;
        return count;
    }

    public float getLevelProgressFraction() {
        UserLevelEntity lv = level.getValue();
        if (lv == null || lv.getXpLevelTarget() == 0) return 0f;
        return (float) lv.getXpInLevel() / lv.getXpLevelTarget();
    }

    public boolean hasFreeFreezeAvailable() {
        UserStreakEntity s = streak.getValue();
        if (s == null) return false;
        return StreakEngine.hasFreeFreeze(s);
    }

    public boolean shieldFreeForCurrentStreak() {
        UserStreakEntity s = streak.getValue();
        if (s == null) return false;
        return StreakEngine.shieldFree(s.getCurrentStreak());
    }
}