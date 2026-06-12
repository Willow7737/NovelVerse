package com.novelverse.app.presentation.streak;

import android.app.Activity;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.novelverse.app.ads.AdManager;
import com.novelverse.app.data.local.entities.UserStreakEntity;
import com.novelverse.app.data.local.preferences.UserPreferences;
import com.novelverse.app.data.repository.GamificationRepository;
import com.novelverse.app.domain.models.StreakHistoryItem;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel for {@link StreakActivity}.
 *
 * Responsibilities:
 *   • Expose live streak state from Room
 *   • Load past streak history from Supabase
 *   • Orchestrate rewarded-ad flows (ink, freeze, recovery)
 *   • Post {@link AdResult} events back to the UI
 */
@HiltViewModel
public class StreakViewModel extends ViewModel {

    private final GamificationRepository repo;
    private final UserPreferences         prefs;
    private final AdManager               adManager;

    // ── Observable state ─────────────────────────────────────────────────────

    /** Live streak from Room — updates immediately on local writes. */
    public final LiveData<UserStreakEntity> streakLive;

    private final MutableLiveData<List<StreakHistoryItem>> historyData =
            new MutableLiveData<>(Collections.emptyList());
    public final LiveData<List<StreakHistoryItem>> historyLive = historyData;

    /** One-shot event emitted after an ad reward is processed (success or fail). */
    private final MutableLiveData<AdResult> adResultData = new MutableLiveData<>();
    public final LiveData<AdResult> adResultLive = adResultData;

    // ── Constructor ───────────────────────────────────────────────────────────

    @Inject
    public StreakViewModel(
            GamificationRepository repo,
            UserPreferences prefs,
            AdManager adManager) {
        this.repo       = repo;
        this.prefs      = prefs;
        this.adManager  = adManager;

        String userId = prefs.getUserId();
        streakLive = (userId != null)
                ? repo.getStreakLive(userId)
                : new MutableLiveData<>(null);

        if (userId != null) {
            loadHistory();
        }
    }

    // ── History ───────────────────────────────────────────────────────────────

    /** Fetch past streak history from Supabase and post to historyLive. */
    public void loadHistory() {
        String userId = prefs.getUserId();
        if (userId == null) return;

        repo.fetchStreakHistory(userId, 20, items ->
                historyData.postValue(items != null ? items : Collections.emptyList()));
    }

    // ── Ad flows ──────────────────────────────────────────────────────────────

    /**
     * Show a rewarded ad; on reward earned, call {@code ad_earn_ink} RPC.
     * Posts an {@link AdResult} to {@link #adResultLive}.
     *
     * @param activity current foreground Activity (not retained)
     */
    public void watchAdForInk(Activity activity) {
        String userId = prefs.getUserId();
        if (userId == null) return;

        adManager.showRewardedAd(activity, new AdManager.RewardedAdListener() {
            @Override
            public void onUserEarnedReward(int amount, String type) {
                repo.adEarnInk(userId, (success, inkAwarded, reason) ->
                        adResultData.postValue(
                                success
                                ? AdResult.inkSuccess(inkAwarded)
                                : AdResult.failure(AdResult.Type.INK, reason)));
            }

            @Override
            public void onAdNotReady() {
                adResultData.postValue(AdResult.adNotReady(AdResult.Type.INK));
            }
        });
    }

    /**
     * Show a rewarded ad; on reward, grant a 48-hour streak freeze via
     * {@code ad_restore_freeze} RPC and update local Room.
     */
    public void watchAdForFreeze(Activity activity) {
        String userId = prefs.getUserId();
        if (userId == null) return;

        adManager.showRewardedAd(activity, new AdManager.RewardedAdListener() {
            @Override
            public void onUserEarnedReward(int amount, String type) {
                repo.adRestoreFreeze(userId, success ->
                        adResultData.postValue(
                                success
                                ? AdResult.freezeSuccess()
                                : AdResult.failure(AdResult.Type.FREEZE, "daily_limit")));
            }

            @Override
            public void onAdNotReady() {
                adResultData.postValue(AdResult.adNotReady(AdResult.Type.FREEZE));
            }
        });
    }

    /**
     * Show a rewarded ad; on reward, recover the most recently broken streak
     * (up to {@code recoverTo} days) via {@code ad_recover_streak} RPC.
     */
    public void watchAdToRecover(Activity activity, int recoverTo) {
        String userId = prefs.getUserId();
        if (userId == null) return;

        adManager.showRewardedAd(activity, new AdManager.RewardedAdListener() {
            @Override
            public void onUserEarnedReward(int amount, String type) {
                repo.adRecoverStreak(userId, recoverTo, success -> {
                    adResultData.postValue(
                            success
                            ? AdResult.recoverySuccess(recoverTo)
                            : AdResult.failure(AdResult.Type.RECOVERY, "server_error"));
                    if (success) loadHistory(); // refresh past streaks
                });
            }

            @Override
            public void onAdNotReady() {
                adResultData.postValue(AdResult.adNotReady(AdResult.Type.RECOVERY));
            }
        });
    }

    // ── AdResult event ────────────────────────────────────────────────────────

    /** One-shot event model emitted after each rewarded-ad attempt. */
    public static final class AdResult {

        public enum Type { INK, FREEZE, RECOVERY }

        public final Type    type;
        public final boolean success;
        public final int     inkAwarded;   // > 0 only for INK type
        public final int     recoveredTo;  // > 0 only for RECOVERY type
        public final String  reason;       // failure reason or null

        private AdResult(Type t, boolean s, int ink, int recovered, String reason) {
            this.type        = t;
            this.success     = s;
            this.inkAwarded  = ink;
            this.recoveredTo = recovered;
            this.reason      = reason;
        }

        public static AdResult inkSuccess(int ink) {
            return new AdResult(Type.INK, true, ink, 0, null);
        }

        public static AdResult freezeSuccess() {
            return new AdResult(Type.FREEZE, true, 0, 0, null);
        }

        public static AdResult recoverySuccess(int to) {
            return new AdResult(Type.RECOVERY, true, 0, to, null);
        }

        public static AdResult failure(Type type, String reason) {
            return new AdResult(type, false, 0, 0, reason);
        }

        public static AdResult adNotReady(Type type) {
            return new AdResult(type, false, 0, 0, "ad_not_ready");
        }
    }
}
