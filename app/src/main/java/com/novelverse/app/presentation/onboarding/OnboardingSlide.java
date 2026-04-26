package com.novelverse.app.presentation.onboarding;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

/**
 * Immutable data class for a single onboarding slide.
 */
public class OnboardingSlide {

    @DrawableRes
    private final int imageResId;

    @StringRes
    private final int headlineResId;

    @StringRes
    private final int subtitleResId;

    public OnboardingSlide(@DrawableRes int imageResId,
                           @StringRes int headlineResId,
                           @StringRes int subtitleResId) {
        this.imageResId = imageResId;
        this.headlineResId = headlineResId;
        this.subtitleResId = subtitleResId;
    }

    @DrawableRes
    public int getImageResId() {
        return imageResId;
    }

    @StringRes
    public int getHeadlineResId() {
        return headlineResId;
    }

    @StringRes
    public int getSubtitleResId() {
        return subtitleResId;
    }
}
