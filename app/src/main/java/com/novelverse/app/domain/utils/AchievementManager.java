package com.novelverse.app.domain.utils;

import android.app.Activity;

import com.novelverse.app.R;
import com.novelverse.app.domain.models.Achievement;
import com.novelverse.app.presentation.common.views.AchievementToastView;

/** Task 34: Checks achievement conditions and triggers the toast banner */
public class AchievementManager {

    public enum Event {
        CHAPTER_READ, BOOKMARK_ADDED, STREAK_UPDATED, NOVEL_COMPLETED
    }

    public static void check(Activity activity, Event event, int contextValue) {
        Achievement achieved = null;
        switch (event) {
            case CHAPTER_READ:
                if (contextValue == 1) {
                    achieved = new Achievement("first_chapter", "First Chapter Read",
                        "You read your first chapter!", R.drawable.ic_badge_first_chapter, 10);
                }
                break;
            case BOOKMARK_ADDED:
                if (contextValue == 1) {
                    achieved = new Achievement("first_bookmark", "First Bookmark",
                        "You added your first bookmark!", R.drawable.ic_badge_first_bookmark, 5);
                }
                break;
            case STREAK_UPDATED:
                if (contextValue == 7) {
                    achieved = new Achievement("streak_7", "7-Day Streak",
                        "7 days in a row!", R.drawable.ic_badge_streak_7, 25);
                } else if (contextValue == 30) {
                    achieved = new Achievement("streak_30", "30-Day Streak",
                        "30 days in a row!", R.drawable.ic_badge_streak_30, 100);
                }
                break;
            case NOVEL_COMPLETED:
                achieved = new Achievement("completionist", "Completionist",
                    "Finished a novel!", R.drawable.ic_badge_completionist, 50);
                break;
        }
        if (achieved != null) AchievementToastView.show(activity, achieved);
    }
}
