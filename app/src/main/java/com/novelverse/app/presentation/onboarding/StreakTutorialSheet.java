package com.novelverse.app.presentation.onboarding;

import androidx.annotation.NonNull;
import com.novelverse.app.R;

/**
 * Onboarding sheet explaining the daily streak + check-in system.
 *
 * <p>Shown once per session the first time a user lands on the home screen.
 * The tutorial image ({@code img_tutorial_streak_tap}) should already be
 * in your {@code res/drawable} folder — it's the one you added.
 *
 * <h3>To show this sheet</h3>
 * <pre>{@code
 *   if (OnboardingManager.get(ctx).shouldShow(OnboardingManager.KEY_STREAK_TUTORIAL)) {
 *       new StreakTutorialSheet()
 *           .show(getSupportFragmentManager(), StreakTutorialSheet.TAG);
 *   }
 * }</pre>
 */
public class StreakTutorialSheet extends BaseOnboardingSheet {

    public static final String TAG = "StreakTutorialSheet";

    @NonNull
    @Override
    public String getSheetKey() {
        return OnboardingManager.KEY_STREAK_TUTORIAL;
    }

    @Override
    public int getImageRes() {
        return R.drawable.img_tutorial_streak_tap;
    }

    @NonNull
    @Override
    public String getTitle() {
        return "Keep your streak alive! \uD83D\uDD25";  // 🔥
    }

    @NonNull
    @Override
    public String getBody() {
        return "Check in every day to build your reading streak. "
             + "Tap the flame icon at the top of the home screen or visit your profile "
             + "to collect your daily reward. Miss a day and your streak resets — "
             + "so come back tomorrow!";
    }
}
