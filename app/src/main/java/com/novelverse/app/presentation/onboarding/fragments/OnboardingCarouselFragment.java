package com.novelverse.app.presentation.onboarding.fragments;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.novelverse.app.R;
import com.novelverse.app.presentation.onboarding.OnboardingActivity;
import com.novelverse.app.presentation.onboarding.OnboardingSlide;
import com.novelverse.app.presentation.onboarding.OnboardingSlideAdapter;
import com.novelverse.app.presentation.onboarding.OnboardingViewModel;

import java.util.Arrays;
import java.util.List;

/**
 * Onboarding carousel fragment with smooth, responsive page indicator animations.
 *
 * Hosts a ViewPager2 with slide pages, animated page indicators, and navigation buttons.
 * Skip / Get Started / Login actions are wired directly to the hosting activity.
 *
 * Features:
 * - Smooth width morphing animation (pill to dot)
 * - Color fade transitions between active and inactive states
 * - Responsive scaling for different screen sizes
 * - Natural acceleration/deceleration interpolation
 */
public class OnboardingCarouselFragment extends Fragment {

    private ViewPager2 viewPager;
    private TextView skipButton;
    private MaterialButton getStartedButton;
    private TextView loginLink;
    private LinearLayout indicatorsContainer;

    private View[] indicatorViews;
    private ValueAnimator[] activeAnimators;

    // Animation configuration
    private static final long ANIMATION_DURATION = 350L;
    private static final int INDICATOR_ACTIVE_WIDTH_DP = 24;
    private static final int INDICATOR_INACTIVE_WIDTH_DP = 6;
    private static final int INDICATOR_HEIGHT_DP = 6;
    private static final int INDICATOR_SPACING_DP = 8;

    // Define your 4 slides here — replace drawable/string resources with your actual ones
    private final List<OnboardingSlide> slides = Arrays.asList(
            new OnboardingSlide(
                    R.drawable.img_onboarding_slide_1,   // replace with your drawable
                    R.string.onboarding_title_1,         // existing string
                    R.string.onboarding_desc_1           // existing string
            ),
            new OnboardingSlide(
                    R.drawable.img_onboarding_slide_2,
                    R.string.onboarding_title_2,
                    R.string.onboarding_desc_2
            ),
            new OnboardingSlide(
                    R.drawable.img_onboarding_slide_3,
                    R.string.onboarding_title_3,
                    R.string.onboarding_desc_3
            ),
            new OnboardingSlide(
                    R.drawable.img_onboarding_slide_4,
                    R.string.onboarding_title_4,
                    R.string.onboarding_desc_4
            )
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_onboarding_carousel, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        setupViewPager();
        setupIndicators(view);
        setupClickListeners();
        updateIndicators(0);
    }

    private void bindViews(@NonNull View view) {
        viewPager = view.findViewById(R.id.view_pager);
        skipButton = view.findViewById(R.id.skip_button);
        getStartedButton = view.findViewById(R.id.get_started_button);
        loginLink = view.findViewById(R.id.login_link);
        indicatorsContainer = view.findViewById(R.id.page_indicators);
    }

    private void setupViewPager() {
        OnboardingSlideAdapter adapter = new OnboardingSlideAdapter(slides);
        viewPager.setAdapter(adapter);

        // Page change callback updates dot indicators with smooth animation
        viewPager.registerOnPageChangeCallback(pageChangeCallback);
    }

    private void setupIndicators(@NonNull View view) {
        // Map indicator IDs to views for easy updates
        indicatorViews = new View[]{
                view.findViewById(R.id.indicator_0),
                view.findViewById(R.id.indicator_1),
                view.findViewById(R.id.indicator_2),
                view.findViewById(R.id.indicator_3)
        };
        activeAnimators = new ValueAnimator[indicatorViews.length];
    }

    private void setupClickListeners() {
        // Skip button → jump directly to the Get Started step
        skipButton.setOnClickListener(v -> navigateToStep(OnboardingViewModel.STEP_GET_STARTED));

        // Get Started button → same destination as skip
        getStartedButton.setOnClickListener(v -> navigateToStep(OnboardingViewModel.STEP_GET_STARTED));

        // Login link → go to login step
        loginLink.setOnClickListener(v -> navigateToStep(OnboardingViewModel.STEP_LOGIN));
    }

    private void navigateToStep(int step) {
        OnboardingActivity activity = (OnboardingActivity) requireActivity();
        activity.goToStep(step);
    }

    /**
     * Updates page indicators with smooth morphing animation.
     * Active: Animates to 24dp wide pill with primary color
     * Inactive: Animates to 6dp circle with secondary color
     */
    private void updateIndicators(int activePosition) {
        if (indicatorViews == null) return;

        for (int i = 0; i < indicatorViews.length; i++) {
            View indicator = indicatorViews[i];
            if (indicator == null) continue;

            boolean isActive = (i == activePosition);

            // Cancel any ongoing animation for this indicator
            if (activeAnimators[i] != null) {
                activeAnimators[i].cancel();
            }

            // Animate width change for smooth morphing
            int targetWidth = dpToPx(isActive ? INDICATOR_ACTIVE_WIDTH_DP : INDICATOR_INACTIVE_WIDTH_DP);
            int currentWidth = indicator.getWidth();

            // If this is the first call or width is not yet measured, set directly
            if (currentWidth == 0) {
                setIndicatorWidth(indicator, targetWidth);
                setIndicatorBackground(indicator, isActive);
            } else {
                // Animate the width change
                animateIndicatorWidth(indicator, currentWidth, targetWidth, isActive);
            }
        }
    }

    /**
     * Animates the width of an indicator from current to target width.
     * Also animates the background color transition.
     */
    private void animateIndicatorWidth(View indicator, int fromWidth, int toWidth, boolean toActive) {
        ValueAnimator widthAnimator = ValueAnimator.ofInt(fromWidth, toWidth);
        widthAnimator.setDuration(ANIMATION_DURATION);
        widthAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        int indicatorIndex = getIndicatorIndex(indicator);
        if (indicatorIndex >= 0) {
            activeAnimators[indicatorIndex] = widthAnimator;
        }

        widthAnimator.addUpdateListener(animation -> {
            int animatedWidth = (int) animation.getAnimatedValue();
            setIndicatorWidth(indicator, animatedWidth);
        });

        widthAnimator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                // Ensure final state is correct
                setIndicatorBackground(indicator, toActive);
            }
        });

        // Start color animation immediately
        animateIndicatorColor(indicator, toActive);

        widthAnimator.start();
    }

    /**
     * Animates the background color of an indicator.
     */
    private void animateIndicatorColor(View indicator, boolean toActive) {
        // Use ObjectAnimator to smoothly transition alpha/color
        // We'll update the background drawable at the midpoint
        ValueAnimator colorAnimator = ValueAnimator.ofFloat(0f, 1f);
        colorAnimator.setDuration(ANIMATION_DURATION);
        colorAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        colorAnimator.addUpdateListener(animation -> {
            float progress = (float) animation.getAnimatedValue();
            // At midpoint (0.5), switch the drawable for smooth visual transition
            if (progress >= 0.5f) {
                setIndicatorBackground(indicator, toActive);
            }
        });

        colorAnimator.start();
    }

    /**
     * Sets the width of an indicator view using LayoutParams.
     */
    private void setIndicatorWidth(View indicator, int widthPx) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) indicator.getLayoutParams();
        if (params != null) {
            params.width = widthPx;
            indicator.setLayoutParams(params);
        }
    }

    /**
     * Sets the background drawable for an indicator based on active state.
     */
    private void setIndicatorBackground(View indicator, boolean isActive) {
        indicator.setBackgroundResource(isActive
                ? R.drawable.bg_page_indicator_active
                : R.drawable.bg_page_indicator_inactive);
    }

    /**
     * Finds the index of an indicator view in the array.
     */
    private int getIndicatorIndex(View indicator) {
        for (int i = 0; i < indicatorViews.length; i++) {
            if (indicatorViews[i] == indicator) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Converts dp to pixels based on device density.
     */
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewPager.unregisterOnPageChangeCallback(pageChangeCallback);
        
        // Cancel all active animators
        if (activeAnimators != null) {
            for (ValueAnimator animator : activeAnimators) {
                if (animator != null && animator.isRunning()) {
                    animator.cancel();
                }
            }
        }
    }

    // Keep a reference so we can unregister it in onDestroyView
    private final ViewPager2.OnPageChangeCallback pageChangeCallback =
            new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    updateIndicators(position);
                }
            };
}
