package com.novelverse.app.domain.gamification;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.novelverse.app.data.local.entities.AchievementEntity;
import com.novelverse.app.data.repository.GamificationRepository;
import com.novelverse.app.presentation.common.views.AchievementToastView;

import java.util.Calendar;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Single achievement evaluator — client-side, shows toasts immediately.
 *
 * Evaluation triggers:
 *   - READING_PAUSE: fired when reader pauses/stops, carries seconds read & progress pct
 *   - CHAPTER_COMPLETE: fired when a chapter is finished
 *   - NOVEL_COMPLETE: fired when all chapters of a novel are done
 *   - CHAPTER_PUBLISHED: fired after a chapter is published (min 1000 words + 5 reads)
 *   - REVIEW_WRITTEN: fired after a valid review (50+ chars, spam-check passed)
 *   - PROFILE_VIEWED: fired when the user's profile is viewed
 *   - BOOKMARK_ADDED: fired when a bookmark is added
 *   - FOLLOW_ADDED: fired when user follows an author
 *   - FOLLOWER_GAINED: fired when user gains a follower
 *   - SUPPORTER_STARTED: fired on subscription start
 *   - STREAK_UPDATED: carries new streak count
 *
 * Anti-exploit:
 *   - Reading events require ≥30s minimum + actual scroll progress advance
 *   - Reviews require 50+ char minimum (enforced before calling this)
 *   - Rate limit: 10 achievements/minute (enforced via DailyCapDao)
 */
@Singleton
public class AchievementEngine {

    private static final String TAG = "AchievementEngine";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final GamificationRepository repo;

    @Inject
    public AchievementEngine(GamificationRepository repo) {
        this.repo = repo;
    }

    // ── Public trigger methods ────────────────────────────────────────────────

    /**
     * Call when user pauses/leaves reader.
     * @param secondsRead  how long they actually read (anti-exploit: must be ≥30)
     * @param progressPct  current progress percentage (must have advanced)
     * @param prevProgressPct  progress at last save (ensures real advancement)
     */
    public void onReadingPause(Activity activity, String userId,
                               int secondsRead, double progressPct, double prevProgressPct,
                               int totalNovelsRead, int totalChaptersRead, long nowMs) {
        if (secondsRead < 30) return; // anti-exploit: minimum 30s
        if (progressPct <= prevProgressPct) return; // anti-exploit: must advance

        addXpAsync(userId, XpLevelEngine.XP_CHAPTER_READ, nowMs);
        addInkAsync(userId, inkForSeconds(secondsRead), "EARN_READING",
            "Reading session", nowMs);

        // Night reader — between 00:00 and 04:00
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(nowMs);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        if (hour >= 0 && hour < 4) {
            tryUnlock(activity, userId, "night_reader", nowMs);
        }

        // Reader milestones by novel count
        checkReaderMilestones(activity, userId, totalNovelsRead, nowMs);
    }

    public void onChapterComplete(Activity activity, String userId,
                                  int totalChaptersRead, long nowMs) {
        if (totalChaptersRead == 1) tryUnlock(activity, userId, "first_page", nowMs);
        addXpAsync(userId, XpLevelEngine.XP_CHAPTER_READ, nowMs);
    }

    public void onNovelComplete(Activity activity, String userId,
                                int totalNovelsCompleted, long nowMs) {
        addXpAsync(userId, XpLevelEngine.XP_NOVEL_COMPLETED, nowMs);
        if (totalNovelsCompleted == 1) tryUnlock(activity, userId, "completionist", nowMs);
        if (totalNovelsCompleted >= 5)  tryUnlock(activity, userId, "finished_strong", nowMs);
    }

    /**
     * @param wordCount    chapter word count (must be ≥1000)
     * @param readCount    number of reads (must be ≥5)
     * @param totalPublished total chapters published by this author
     */
    public void onChapterPublished(Activity activity, String userId,
                                   int wordCount, int readCount, int totalPublished, long nowMs) {
        if (wordCount < 1000 || readCount < 5) {
            // Trigger first_words on publish regardless of threshold
            if (totalPublished == 1) tryUnlock(activity, userId, "first_words", nowMs);
            return;
        }
        addXpAsync(userId, XpLevelEngine.XP_CHAPTER_PUBLISHED, nowMs);
        tryUnlock(activity, userId, "published", nowMs);
        if (totalPublished >= 10) tryUnlock(activity, userId, "prolific", nowMs);
    }

    public void onTotalReadsUpdated(Activity activity, String userId,
                                    int totalReads, long nowMs) {
        if (totalReads >= 100)   tryUnlock(activity, userId, "rising_star", nowMs);
        if (totalReads >= 1000)  tryUnlock(activity, userId, "going_viral", nowMs);
        if (totalReads >= 10000) tryUnlock(activity, userId, "bestseller", nowMs);
    }

    /**
     * @param reviewCharCount  must be ≥50 (caller validates spam)
     * @param totalReviews     total valid reviews written
     */
    public void onReviewWritten(Activity activity, String userId,
                                int reviewCharCount, int totalReviews, long nowMs) {
        if (reviewCharCount < 50) return;
        addXpAsync(userId, XpLevelEngine.XP_REVIEW_WRITTEN, nowMs);
        if (totalReviews >= 10) tryUnlock(activity, userId, "critic", nowMs);
    }

    public void onReviewsLiked(Activity activity, String userId, int totalLiked, long nowMs) {
        if (totalLiked >= 25) tryUnlock(activity, userId, "trusted_voice", nowMs);
    }

    public void onBookmarkAdded(Activity activity, String userId,
                                int totalBookmarks, long nowMs) {
        if (totalBookmarks == 1) tryUnlock(activity, userId, "bookmarked", nowMs);
    }

    public void onFollowAdded(Activity activity, String userId,
                              int totalFollowing, long nowMs) {
        if (totalFollowing >= 10) tryUnlock(activity, userId, "connected", nowMs);
    }

    public void onFollowerGained(Activity activity, String userId,
                                 int totalFollowers, long nowMs) {
        if (totalFollowers >= 50) tryUnlock(activity, userId, "trendsetter", nowMs);
    }

    public void onStreakUpdated(Activity activity, String userId,
                                int newStreak, long nowMs) {
        addXpAsync(userId, XpLevelEngine.XP_DAILY_STREAK, nowMs);
        String achId = streakAchievementId(newStreak);
        if (achId != null) tryUnlock(activity, userId, achId, nowMs);
    }

    public void onSubscriptionStarted(Activity activity, String userId,
                                      int monthsSubscribed, long nowMs) {
        if (monthsSubscribed >= 1)  tryUnlock(activity, userId, "supporter_bronze", nowMs);
        if (monthsSubscribed >= 3)  tryUnlock(activity, userId, "supporter_silver", nowMs);
        if (monthsSubscribed >= 12) tryUnlock(activity, userId, "supporter_gold", nowMs);
    }

    // ── Core unlock path ──────────────────────────────────────────────────────

    private void tryUnlock(Activity activity, String userId, String achievementId, long nowMs) {
        // Rate limit check (synchronous on calling thread — cheap Room read)
        if (!repo.checkAchievementRateLimit(userId, nowMs)) {
            Log.w(TAG, "Rate limit hit — skipping " + achievementId);
            return;
        }

        AchievementEntity ach = repo.unlockAchievement(userId, achievementId, nowMs);
        if (ach != null) {
            // Show toast on main thread immediately
            mainHandler.post(() -> AchievementToastView.show(activity, ach));
            Log.i(TAG, "Unlocked: " + ach.getTitle() + " for " + userId);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void checkReaderMilestones(Activity activity, String userId, int totalNovels, long nowMs) {
        if (totalNovels >= 10)  tryUnlock(activity, userId, "bookworm",   nowMs);
        if (totalNovels >= 25)  tryUnlock(activity, userId, "scholar",    nowMs);
        if (totalNovels >= 50)  tryUnlock(activity, userId, "sage",       nowMs);
        if (totalNovels >= 100) tryUnlock(activity, userId, "lorekeeper", nowMs);
    }

    private String streakAchievementId(int streak) {
        if (streak == 3)   return "streak_3";
        if (streak == 7)   return "streak_7";
        if (streak == 30)  return "streak_30";
        if (streak == 100) return "streak_100";
        return null;
    }

    /** Ink earned per reading session: 1 Ink per 30s, capped at 50 per session */
    private int inkForSeconds(int seconds) {
        return Math.min(50, seconds / 30);
    }

    private void addXpAsync(String userId, int xp, long nowMs) {
        new Thread(() -> repo.addXp(userId, xp, nowMs)).start();
    }

    private void addInkAsync(String userId, int amount, String type, String reason, long nowMs) {
        new Thread(() -> repo.addInk(userId, amount, type, reason, nowMs)).start();
    }
}
